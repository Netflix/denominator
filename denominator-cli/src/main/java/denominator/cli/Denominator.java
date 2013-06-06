package denominator.cli;
import static com.google.common.io.Closeables.closeQuietly;
import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.provider;
import static java.lang.String.format;
import io.airlift.command.Cli;
import io.airlift.command.Cli.CliBuilder;
import io.airlift.command.Command;
import io.airlift.command.Help;
import io.airlift.command.Option;
import io.airlift.command.OptionType;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.io.Files;

import dagger.ObjectGraph;
import denominator.Credentials;
import denominator.Credentials.MapCredentials;
import denominator.DNSApiManager;
import denominator.Denominator.Version;
import denominator.Provider;
import denominator.cli.GeoResourceRecordSetCommands.GeoRegionList;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetApplyTTL;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetGet;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetList;
import denominator.cli.GeoResourceRecordSetCommands.GeoTypeList;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetAdd;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetApplyTTL;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetDelete;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetGet;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetList;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetRemove;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetReplace;
import denominator.clouddns.CloudDNSProvider;
import denominator.dynect.DynECTProvider;
import denominator.mock.MockProvider;
import denominator.route53.Route53Provider;
import denominator.ultradns.UltraDNSProvider;

public class Denominator {
    public static void main(String[] args) {
        CliBuilder<Runnable> builder = Cli.<Runnable> builder("denominator")
                                          .withDescription("Denominator: Portable control of DNS clouds")
                                          .withDefaultCommand(Help.class)
                                          .withCommand(Help.class)
                                          .withCommand(PrintVersion.class)
                                          .withCommand(ListProviders.class);

        builder.withGroup("zone")
               .withDescription("manage zones")
               .withDefaultCommand(ZoneList.class)
               .withCommand(ZoneList.class);

        builder.withGroup("record")
               .withDescription("manage resource record sets in a zone")
               .withDefaultCommand(ResourceRecordSetList.class)
               .withCommand(ResourceRecordSetList.class)
               .withCommand(ResourceRecordSetGet.class)
               .withCommand(ResourceRecordSetAdd.class)
               .withCommand(ResourceRecordSetApplyTTL.class)
               .withCommand(ResourceRecordSetReplace.class)
               .withCommand(ResourceRecordSetRemove.class)
               .withCommand(ResourceRecordSetDelete.class);

        builder.withGroup("geo")
               .withDescription("manage geo resource record sets in a zone")
               .withDefaultCommand(GeoResourceRecordSetList.class)
               .withCommand(GeoTypeList.class)
               .withCommand(GeoRegionList.class)
               .withCommand(GeoResourceRecordSetList.class)
               .withCommand(GeoResourceRecordSetGet.class)
               .withCommand(GeoResourceRecordSetApplyTTL.class);

        Cli<Runnable> denominatorParser = builder.build();
        try {
            denominatorParser.parse(args).run();
        } catch (RuntimeException e) {
            System.err.println(";; error: "+ e.getMessage());
            System.exit(1);
        } 
        System.exit(0);
    }

    @Command(name = "version", description = "output the version of denominator and java runtime in use")
    public static class PrintVersion implements Runnable {
        public void run() {
            System.out.println("Denominator " + Version.INSTANCE);
            System.out.println("Java version: " + System.getProperty("java.version"));
        }
    }

    @Command(name = "providers", description = "List the providers and their expected credentials")
    public static class ListProviders implements Runnable {
        final static String table = "%-10s %-52s %-14s %s%n";

        public void run() {
            System.out.println(providerAndCredentialsTable());
        }

        public static String providerAndCredentialsTable() {
            StringBuilder builder = new StringBuilder();
            
            builder.append(format(table, "provider", "url", "credentialType", "credentialArgs"));
            for (Provider provider : listProviders()) {
                if (provider.getCredentialTypeToParameterNames().isEmpty())
                    builder.append(format("%-10s %-52s%n", provider.getName(), provider.getUrl()));
                for (Entry<String, Collection<String>> entry : provider.getCredentialTypeToParameterNames().asMap()
                        .entrySet()) {
                    String parameters = Joiner.on(' ').join(entry.getValue());
                    builder.append(format(table, provider.getName(), provider.getUrl(), entry.getKey(), parameters));
                }
            }
            return builder.toString();
        }
    }

    public static abstract class DenominatorCommand implements Runnable {
        @Option(type = OptionType.GLOBAL, name = { "-p", "--provider" }, description = "provider to affect")
        public String providerName;

        @Option(type = OptionType.GLOBAL, name = { "-u", "--url" }, description = "alternative api url to connect to")
        public String url;

        @Option(type = OptionType.GLOBAL, name = { "-c", "--credential" }, description = "adds a credential argument (execute denominator providers for what these are)")
        public List<String> credentialArgs;

        @Option(type = OptionType.GLOBAL, name = { "-C", "--config" }, description = "path to configuration file (used to store credentials)")
        public String configPath;

        @Option(type = OptionType.GLOBAL, name = { "-n", "--name" }, description = "unique name of provider configuration")
        public String name;

        protected Credentials credentials;

        @SuppressWarnings("unchecked")
        public void run() {
            if (providerName != null && credentialArgs != null) {
                credentials = Credentials.ListCredentials.from(credentialArgs);
            } else if (configPath != null) {
                Map<?, ?> configFromFile = getConfigFromFile();
                if (configFromFile != null) {
                    credentials = MapCredentials.from(Map.class.cast(configFromFile.get("credentials")));
                    providerName = configFromFile.get("provider").toString();
                }
            }
            Builder<Object> modulesForGraph = ImmutableList.builder().add(provider(newProvider())).add(newModule());
            if (credentials != null)
                modulesForGraph.add(credentials(credentials));
            DNSApiManager mgr = null;
            try {
                mgr = ObjectGraph.create(modulesForGraph.build().toArray()).get(DNSApiManager.class);
                for (Iterator<String> i = doRun(mgr); i.hasNext();)
                    System.out.println(i.next());
            } finally {
                closeQuietly(mgr);
            }
        }

        /**
         * Load configuration for given name from a YAML configuration file.
         */
        Map<?, ?> getConfigFromFile() {
            if (configPath == null)
                return null;
            String configFileContent = null;
            try {
                configFileContent = getFileContentsFromPath(configPath);
            } catch (IOException e) {
                System.err.println("configuration file not found: " + e.getMessage());
                System.exit(1);
            }
            return getConfigFromYaml(configFileContent);
        }

        Map<?, ?> getConfigFromYaml(String yamlAsString) {
            Yaml yaml = new Yaml();
            Iterable<Object> configs = yaml.loadAll(yamlAsString);
            Object providerConf = FluentIterable.from(configs).firstMatch(new Predicate<Object>() {
                @Override
                public boolean apply(Object input) {
                    return name.equals(Map.class.cast(input).get("name"));
                }
            }).get();
            return Map.class.cast(providerConf);
        }

        String getFileContentsFromPath(String path) throws IOException {
            if (path.startsWith("~"))
                path = System.getProperty("user.home") + path.substring(1);
            return Files.toString(new File(path), Charsets.UTF_8);
        }

        /**
         * return a lazy iterator where possible to improve the perceived responsiveness of the cli
         */
        protected abstract Iterator<String> doRun(DNSApiManager mgr);

        /**
         * This avoids service loader lookup which adds runtime and build
         * complexity.
         * 
         * <h3>Note</h3>
         * 
         * update this code block when adding new providers to the CLI.
         */
        // TODO: consider generating this method at compile time
        protected Provider newProvider() {
            if ("mock".equals(providerName)) {
                return new MockProvider(url);
            } else if ("clouddns".equals(providerName)) {
                return new CloudDNSProvider(url);
            } else if ("dynect".equals(providerName)) {
                return new DynECTProvider(url);
            } else if ("route53".equals(providerName)) {
                return new Route53Provider(url);
            } else if ("ultradns".equals(providerName)) {
                return new UltraDNSProvider(url);
            }
            throw new IllegalArgumentException("provider " + providerName
                    + " unsupported.  Please execute \"denominator providers\" to list configured providers.");
        }

        protected Object newModule() {
            if ("mock".equals(providerName)) {
                return new MockProvider.Module();
            } else if ("clouddns".equals(providerName)) {
                return new CloudDNSProvider.Module();
            } else if ("dynect".equals(providerName)) {
                return new DynECTProvider.Module();
            } else if ("route53".equals(providerName)) {
                return new Route53Provider.Module();
            } else if ("ultradns".equals(providerName)) {
                return new UltraDNSProvider.Module();
            }
            throw new IllegalArgumentException("provider " + providerName
                    + " unsupported.  Please execute \"denominator providers\" to list configured providers.");
        }
    }

    /**
     * Lazy to avoid loading the classes unless they are requested.
     * 
     * <h3>Note</h3>
     * 
     * update this code block when adding new providers to the CLI.
     */
    // TODO: consider generating this method at compile time
    private static Iterable<Provider> listProviders() {
        return ImmutableList.<Provider> builder()
                            .add(new MockProvider())
                            .add(new CloudDNSProvider())
                            .add(new DynECTProvider())
                            .add(new Route53Provider())
                            .add(new UltraDNSProvider()).build();
    }

    @Command(name = "list", description = "Lists the zone names present in this provider")
    public static class ZoneList extends DenominatorCommand {
        public Iterator<String> doRun(DNSApiManager mgr) {
            return mgr.getApi().getZoneApi().list();
        }
    }

}