package denominator.cli;
import static com.google.common.base.Preconditions.checkArgument;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.String.format;
import io.airlift.command.Cli;
import io.airlift.command.Cli.CliBuilder;
import io.airlift.command.Command;
import io.airlift.command.Help;
import io.airlift.command.Option;
import io.airlift.command.OptionType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Singleton;

import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterators;
import com.google.common.io.Files;
import com.google.common.net.InternetDomainName;

import dagger.ObjectGraph;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.MapCredentials;
import denominator.DNSApiManager;
import denominator.Denominator.Version;
import denominator.Provider;
import denominator.Providers;
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
import denominator.model.Zone;
import feign.Logger;
import feign.Logger.Level;

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

    @Command(name = "providers", description = "List the providers and their metadata ")
    public static class ListProviders implements Runnable {
        final static String table = "%-10s %-51s %-14s %-14s %s%n";

        public void run() {
            System.out.println(providerAndCredentialsTable());
        }

        public static String providerAndCredentialsTable() {
            StringBuilder builder = new StringBuilder();
            
            builder.append(format(table, "provider", "url", "duplicateZones", "credentialType", "credentialArgs"));
            for (Provider provider : Providers.list()) {
                if (provider.credentialTypeToParameterNames().isEmpty())
                    builder.append(format("%-10s %-51s %-14s %n", provider.name(), provider.url(),
                            provider.supportsDuplicateZoneNames()));
                for (Entry<String, Collection<String>> entry : provider.credentialTypeToParameterNames().entrySet()) {
                    String parameters = Joiner.on(' ').join(entry.getValue());
                    builder.append(format(table, provider.name(), provider.url(),
                            provider.supportsDuplicateZoneNames(), entry.getKey(), parameters));
                }
            }
            return builder.toString();
        }
    }

    public static abstract class DenominatorCommand implements Runnable {
        @Option(type = OptionType.GLOBAL, name = { "-q", "--quiet" }, description = "do not emit informational messages about http commands invoked")
        public boolean quiet;

        @Option(type = OptionType.GLOBAL, name = { "-v", "--verbose" }, description = "emit details such as http requests sent and responses received")
        public boolean verbose;

        @Option(type = OptionType.GLOBAL, name = { "-p", "--provider" }, description = "provider to affect")
        public String providerName;

        @Option(type = OptionType.GLOBAL, name = { "-u", "--url" }, description = "alternative api url to connect to")
        public String url;

        @Option(type = OptionType.GLOBAL, name = { "-c", "--credential" }, description = "adds a credential argument (execute denominator providers for what these are)")
        public List<String> credentialArgs;

        @Option(type = OptionType.GLOBAL, name = { "-C", "--config" }, description = "path to configuration file (used to store credentials). default: ~/.denominatorconfig")
        public String configPath = "~/.denominatorconfig";

        @Option(type = OptionType.GLOBAL, name = { "-n", "--name" }, description = "unique name of provider configuration")
        public String name;

        protected Credentials credentials;

        @SuppressWarnings("unchecked")
        public void run() {
            if (providerName != null && credentialArgs != null) {
                credentials = Credentials.ListCredentials.from(credentialArgs);
            } else if (name != null) {
                Map<?, ?> configFromFile = getConfigFromFile();
                if (configFromFile != null) {
                    credentials = MapCredentials.from(Map.class.cast(configFromFile.get("credentials")));
                    providerName = configFromFile.get("provider").toString();
                    if (configFromFile.containsKey("url"))
                        url = configFromFile.get("url").toString();
                }
            }
            Provider provider = Providers.getByName(providerName);
            if (url != null) {
                provider = Providers.withUrl(provider, url);
            }
            Builder<Object> modulesForGraph = ImmutableList.builder() //
                    .add(Providers.provide(provider)) //
                    .add(Providers.instantiateModule(provider));

            Object logModule = logModule(quiet, verbose);
            if (logModule != null)
                modulesForGraph.add(logModule);

            if (credentials != null)
                modulesForGraph.add(credentials(credentials));
            DNSApiManager mgr = null;
            try {
                mgr = ObjectGraph.create(modulesForGraph.build().toArray()).get(DNSApiManager.class);
                for (Iterator<String> i = doRun(mgr); i.hasNext();)
                    System.out.println(i.next());
            } finally {
                if (mgr != null) {
                    try {
                        mgr.close();
                    } catch (IOException ignored) {

                    }
                }
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
    }

    @Command(name = "list", description = "Lists the zones present in this provider.  If the second column is present, it is the zone id.")
    public static class ZoneList extends DenominatorCommand {
        public Iterator<String> doRun(DNSApiManager mgr) {
            return Iterators.transform(mgr.api().zones().iterator(), new Function<Zone, String>() {

                @Override
                public String apply(Zone input) {
                    if (input.id() != null)
                        return input.name() + " " + input.id();
                    return input.name();
                }

            });
        }
    }

    /**
     * Returns a log configuration module or null if none is needed.
     */
    static Object logModule(boolean quiet, boolean verbose) {
        checkArgument(!(quiet && verbose), "quiet and verbose flags cannot be used at the same time!");
        Logger.Level logLevel;
        if (quiet) {
            return null;
        } else if (verbose) {
            logLevel = Logger.Level.FULL;                
        } else {
            logLevel = Logger.Level.BASIC;
        }
        return new LogModule(logLevel);
    }

    @dagger.Module(overrides = true, library = true)
    static class LogModule {
        final Logger.Level logLevel;

        LogModule(Level logLevel) {
            this.logLevel = logLevel;
        }

        @Provides
        @Singleton
        Logger logger() {
            return new Logger.ErrorLogger();
        }

        @Provides
        @Singleton
        Logger.Level level() {
            return logLevel;
        }
    }

    static String idOrName(DNSApiManager mgr, String zoneIdOrName) {
        if (!InternetDomainName.isValid(zoneIdOrName) || !InternetDomainName.from(zoneIdOrName).hasParent()) {
            return zoneIdOrName;
        } else if (InternetDomainName.isValid(zoneIdOrName) && mgr.provider().supportsDuplicateZoneNames()) {
            List<Zone> currentZones = new ArrayList<Zone>();
            for (Zone zone : mgr.api().zones()) {
                if (zoneIdOrName.equals(zone.name()))
                    return zone.id();
                currentZones.add(zone);
            }
            checkArgument(false, "zone %s not found in %s", zoneIdOrName, currentZones);
        }
        return zoneIdOrName;
    }
}