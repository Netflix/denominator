package denominator.cli;
import static com.google.common.io.Closeables.closeQuietly;
import static denominator.Credentials.ListCredentials.from;
import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static denominator.Denominator.listProviders;
import static java.lang.String.format;
import io.airlift.command.Cli;
import io.airlift.command.Cli.CliBuilder;
import io.airlift.command.Command;
import io.airlift.command.Help;
import io.airlift.command.Option;
import io.airlift.command.OptionType;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Joiner;

import denominator.DNSApiManager;
import denominator.Denominator.Version;
import denominator.Provider;
import denominator.cli.GeoResourceRecordSetCommands.GeoRegionList;
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
               .withCommand(GeoResourceRecordSetGet.class);

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
        public void run() {
            System.out.println(providerAndCredentialsTable());
        }

        final static String table = "%-20s %-16s %s%n";

        public static String providerAndCredentialsTable() {
            return providerAndCredentialsTable(listProviders());
        }

        public static String providerAndCredentialsTable(Iterable<Provider> providers) {
            StringBuilder builder = new StringBuilder();
            
            builder.append(format(table, "provider", "credential type", "credential arguments"));
            for (Provider provider : providers) {
                if (provider.getCredentialTypeToParameterNames().isEmpty())
                    builder.append(format("%-20s%n", provider.getName()));
                for (Entry<String, Collection<String>> entry : provider.getCredentialTypeToParameterNames().asMap()
                        .entrySet()) {
                    builder.append(format(table, provider.getName(), entry.getKey(), Joiner.on(' ').join(entry.getValue())));
                }
            }
            return builder.toString();
        }
    }

    public static abstract class DenominatorCommand implements Runnable {
        @Option(type = OptionType.GLOBAL, required = true, name = { "-p", "--provider" }, description = "provider to affect")
        public String providerName;

        @Option(type = OptionType.GLOBAL, name = { "-c", "--credential" }, description = "adds a credential argument (execute denominator providers for what these are)")
        public List<String> credentialArgs;

        public void run() {
            DNSApiManager mgr = null;
            try {
                if (credentialArgs == null)
                    mgr = create(providerName);
                else
                    mgr = create(providerName, credentials(from(credentialArgs)));
                for (Iterator<String> i = doRun(mgr); i.hasNext();)
                    System.out.println(i.next());
            } finally {
                closeQuietly(mgr);
            }
        }

        /**
         * return a lazy iterator where possible to improve the perceived responsiveness of the cli
         */
        protected abstract Iterator<String> doRun(DNSApiManager mgr);
    }

    @Command(name = "list", description = "Lists the zone names present in this provider")
    public static class ZoneList extends DenominatorCommand {
        public Iterator<String> doRun(DNSApiManager mgr) {
            return mgr.getApi().getZoneApi().list();
        }
    }
}