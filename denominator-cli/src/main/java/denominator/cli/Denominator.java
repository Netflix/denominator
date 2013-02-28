package denominator.cli;

import static com.google.common.io.Closeables.closeQuietly;
import static denominator.Credentials.ListCredentials.from;
import static denominator.CredentialsConfiguration.credentials;
import static denominator.Denominator.create;
import static denominator.Denominator.listProviders;
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
import denominator.Provider;

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

        Cli<Runnable> denominatorParser = builder.build();

        denominatorParser.parse(args).run();
    }

    @Command(name = "providers", description = "List the providers and their expected credentials")
    public static class ListProviders implements Runnable {
        public void run() {
            System.out.println(providerAndCredentialsTable());
        }

        public static String providerAndCredentialsTable() {
            StringBuilder builder = new StringBuilder();
            builder.append("provider\tcredential type\tcredential parameters\n");
            for (Provider provider : listProviders()) {
                if (provider.getCredentialTypeToParameterNames().isEmpty())
                    builder.append(provider.getName()).append('\t').append('\n');
                for (Entry<String, Collection<String>> entry : provider.getCredentialTypeToParameterNames().asMap()
                        .entrySet()) {
                    builder.append(provider.getName()).append('\t').append(entry.getKey()).append('\t')
                            .append(Joiner.on(' ').join(entry.getValue())).append('\n');
                }
            }
            return builder.toString();
        }
    }

    public static abstract class DenominatorCommand implements Runnable {
        @Option(type = OptionType.GLOBAL, required = true, name = { "-p", "--provider" }, description = "provider to affect")
        public String providerName;

        @Option(type = OptionType.GLOBAL, name = { "-c", "--credential" }, description = "adds a credential argument")
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