package denominator.cli;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterators.transform;
import static java.lang.String.format;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.airlift.command.OptionType;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.primitives.UnsignedInteger;

import denominator.DNSApiManager;
import denominator.cli.Denominator.DenominatorCommand;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SOAData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.TXTData;

class ResourceRecordSetCommands {

    private static abstract class ResourceRecordSetCommand extends DenominatorCommand {
        @Option(type = OptionType.GROUP, required = true, name = { "-z", "--zone" }, description = "zone name to affect. ex. denominator.io.")
        public String zoneName;
    }

    @Command(name = "list", description = "Lists the normal record record sets present in this zone")
    public static class ResourceRecordSetList extends ResourceRecordSetCommand {

        public Iterator<String> doRun(DNSApiManager mgr) {
            Iterator<ResourceRecordSet<?>> list = mgr.getApi().getResourceRecordSetApiForZone(zoneName).list();
            return transform(list, ResourceRecordSetToString.INSTANCE);
        }
    }

    @Command(name = "get", description = "gets a normal record record set by name and type, if present in this zone")
    public static class ResourceRecordSetGet extends ResourceRecordSetCommand {
        @Option(type = OptionType.COMMAND, required = true, name = { "-n", "--name" }, description = "name of the record set. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, required = true, name = { "-t", "--type" }, description = "type of the record set. ex. CNAME")
        public String type;

        public Iterator<String> doRun(DNSApiManager mgr) {
            return Iterators.forArray(mgr.getApi().getResourceRecordSetApiForZone(zoneName)
                    .getByNameAndType(name, type).transform(ResourceRecordSetToString.INSTANCE).or(""));
        }
    }

    private static abstract class ModifyRecordSetCommand extends ResourceRecordSetCommand {
        @Option(type = OptionType.COMMAND, required = true, name = { "-n", "--name" }, description = "name of the record set. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, required = true, name = { "-t", "--type" }, description = "type of the record set. ex. CNAME")
        public String type;

        @Option(type = OptionType.COMMAND, required = true, name = { "-d", "--data" }, description = "repeat for each record value (rdata) to add. ex. 1.2.3.4")
        public List<String> values;

        protected Builder<Map<String, Object>> rrsetBuilder() {
            checkArgument(values != null, "you must pass data to add");
            Builder<Map<String, Object>> builder = ResourceRecordSet.builder().name(name).type(type);
            for (String value : values){
                builder.add(toMap(type, value));
            }
            return builder;
        }
    }

    @Command(name = "add", description = "creates or adds data to a record set corresponding to name and type.  sets ttl, if present")
    public static class ResourceRecordSetAdd extends ModifyRecordSetCommand {

        @Option(type = OptionType.COMMAND, name = "--ttl", description = "time to live of the record set. ex. 300")
        public int ttl = -1;

        public Iterator<String> doRun(final DNSApiManager mgr) {
            Builder<Map<String, Object>> builder = rrsetBuilder();
            if (ttl != -1)
                builder.ttl(ttl);
            final ResourceRecordSet<Map<String, Object>> toAdd = builder.build();
            String cmd = format(";; in zone %s adding to rrset %s %s values: [%s]", zoneName, name, type, Joiner.on(',')
                    .join(toAdd));
            if (ttl != -1)
                cmd = format("%s applying ttl %d", cmd, ttl);
            return Iterators.concat(Iterators.forArray(cmd), new Iterator<String>() {
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    mgr.getApi().getResourceRecordSetApiForZone(zoneName).add(toAdd);
                    done = true;
                    return ";; ok";
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            });
        }
    }

    @Command(name = "replace", description = "creates or replaces data in a record set corresponding to name and type.  sets ttl, if present")
    public static class ResourceRecordSetReplace extends ModifyRecordSetCommand {

        @Option(type = OptionType.COMMAND, name = "--ttl", description = "time to live of the record set. ex. 300")
        public int ttl = -1;

        public Iterator<String> doRun(final DNSApiManager mgr) {
            Builder<Map<String, Object>> builder = rrsetBuilder();
            if (ttl != -1)
                builder.ttl(ttl);
            final ResourceRecordSet<Map<String, Object>> toAdd = builder.build();
            String cmd = format(";; in zone %s replacing rrset %s %s with values: [%s]", zoneName, name, type, Joiner.on(',')
                    .join(toAdd));
            if (ttl != -1)
                cmd = format("%s and ttl %d", cmd, ttl);
            return Iterators.concat(Iterators.forArray(cmd), new Iterator<String>() {
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    mgr.getApi().getResourceRecordSetApiForZone(zoneName).replace(toAdd);
                    done = true;
                    return ";; ok";
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            });
        }
    }

    @Command(name = "remove", description = "removes data from a record set corresponding to name and type.")
    public static class ResourceRecordSetRemove extends ModifyRecordSetCommand {

        public Iterator<String> doRun(final DNSApiManager mgr) {
            final ResourceRecordSet<Map<String, Object>> toRemove = rrsetBuilder().build();
            String cmd = format(";; in zone %s removing from rrset %s %s values: [%s]", zoneName, name, type, Joiner.on(',')
                    .join(toRemove));
            return Iterators.concat(Iterators.forArray(cmd), new Iterator<String>() {
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    mgr.getApi().getResourceRecordSetApiForZone(zoneName).remove(toRemove);
                    done = true;
                    return ";; ok";
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            });
        }
    }

    static enum ResourceRecordSetToString implements Function<ResourceRecordSet<?>, String> {
        INSTANCE;

        @Override
        public String apply(ResourceRecordSet<?> input) {
            ImmutableList.Builder<String> lines = ImmutableList.<String> builder();
            for (Map<String, Object> rdata : input) {
                lines.add(format("%-50s%-7s%-6s%s", input.getName(), input.getType(), input.getTTL().orNull(),
                        flatten(rdata)));
            }
            return Joiner.on('\n').join(lines.build());
        }
    }

    static String flatten(Map<String, Object> input) {
        ImmutableList<Object> orderedRdataValues = ImmutableList.copyOf(input.values());
        if (orderedRdataValues.size() == 1) {
            Object rdata = orderedRdataValues.get(0);
            return rdata instanceof InetAddress ? InetAddress.class.cast(rdata).getHostAddress() :rdata.toString();
        }
        return Joiner.on(' ').join(input.values());
    }

    static Map<String, Object> toMap(String type, String rdata) {
        if ("A".equals(type)) {
            return AData.create(rdata);
        } else if ("AAAA".equals(type)) {
            return AAAAData.create(rdata);
        } else if ("CNAME".equals(type)) {
            return CNAMEData.create(rdata);
        } else if ("MX".equals(type)) {
            ImmutableList<String> parts = ImmutableList.copyOf(Splitter.on(' ').split(rdata));
            return MXData.create(UnsignedInteger.valueOf(parts.get(0)), parts.get(1));
        } else if ("NS".equals(type)) {
            return NSData.create(rdata);
        } else if ("PTR".equals(type)) {
            return PTRData.create(rdata);
        } else if ("SOA".equals(type)) {
            ImmutableList<String> parts = ImmutableList.copyOf(Splitter.on(' ').split(rdata));
            return SOAData.builder()
                          .mname(parts.get(0))
                          .rname(parts.get(1))
                          .serial(UnsignedInteger.valueOf(parts.get(2)))
                          .refresh(UnsignedInteger.valueOf(parts.get(3)))
                          .retry(UnsignedInteger.valueOf(parts.get(4)))
                          .expire(UnsignedInteger.valueOf(parts.get(5)))
                          .minimum(UnsignedInteger.valueOf(parts.get(6))).build();
        } else if ("SPF".equals(type)) {
            return SPFData.create(rdata);
        } else if ("SRV".equals(type)) {
            ImmutableList<String> parts = ImmutableList.copyOf(Splitter.on(' ').split(rdata));
            return SRVData.builder()
                          .priority(UnsignedInteger.valueOf(parts.get(0)))
                          .weight(UnsignedInteger.valueOf(parts.get(1)))
                          .port(UnsignedInteger.valueOf(parts.get(2)))
                          .target(parts.get(3)).build();
        } else if ("TXT".equals(type)) {
            return TXTData.create(rdata);
        } else {
            throw new IllegalArgumentException("unsupported type: " + type);
        }
    }
}