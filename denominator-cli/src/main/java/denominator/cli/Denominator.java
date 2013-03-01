package denominator.cli;
import static com.google.common.collect.Iterators.transform;
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

import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import denominator.DNSApiManager;
import denominator.Provider;
import denominator.model.ResourceRecordSet;

public class Denominator {
    public static void main(String[] args) {
        CliBuilder<Runnable> builder = Cli.<Runnable> builder("denominator")
                                          .withDescription("dns manager")
                                          .withDefaultCommand(Help.class)
                                          .withCommand(Help.class)
                                          .withCommand(ListProviders.class);

        builder.withGroup("zone")
               .withDescription("manage zones")
               .withDefaultCommand(ZoneList.class)
               .withCommand(ZoneList.class);

        builder.withGroup("record")
               .withDescription("manage resource record sets in a zone")
               .withDefaultCommand(ResourceRecordSetList.class)
               .withCommand(ResourceRecordSetList.class);

        Cli<Runnable> denominatorParser = builder.build();

        denominatorParser.parse(args).run();
    }

    @Command(name = "providers", description = "List the providers and their expected credentials")
    public static class ListProviders implements Runnable {
        public void run() {
            System.out.println(providerAndCredentialsTable());
        }

        final static String table = "%-20s %-16s %s%n";

        public static String providerAndCredentialsTable() {
            StringBuilder builder = new StringBuilder();
            
            builder.append(format(table, "provider", "credential type", "credential arguments"));
            for (Provider provider : listProviders()) {
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

    public static abstract class ResourceRecordSetCommand extends DenominatorCommand {
        @Option(type = OptionType.GLOBAL, required = true, name = { "-z", "--zone" }, description = "zone name to affect")
        public String zoneName;
    }

    @Command(name = "list", description = "Lists the normal record record sets present in this zone")
    public static class ResourceRecordSetList extends ResourceRecordSetCommand {
        public Iterator<String> doRun(DNSApiManager mgr) {
            Iterator<ResourceRecordSet<?>> list = mgr.getApi().getResourceRecordSetApiForZone(zoneName).list();
            return transform(list, ResourceRecordSetToString.INSTANCE);
        }
    }

    private static enum ResourceRecordSetToString implements Function<ResourceRecordSet<?>, String> {
        INSTANCE;

        @Override
        public String apply(ResourceRecordSet<?> input) {
            StringBuilder lines = new StringBuilder();
            for (Map<String, Object> rdata : input) {
                lines.append(format("%-50s %-5s %-6s %s%n", input.getName(), input.getType(), input.getTTL().orNull(),
                        flatten(rdata)));
            }
            return lines.toString();
        }
    }

    private static String flatten(Map<String, Object> input) {
        ImmutableList<Object> orderedRdataValues = ImmutableList.copyOf(input.values());
        if (orderedRdataValues.size() == 1) {
            Object rdata = orderedRdataValues.get(0);
            return rdata instanceof InetAddress ? InetAddress.class.cast(rdata).getHostAddress() :rdata.toString();
        }
        return Joiner.on(' ').join(input.values());
    }
}