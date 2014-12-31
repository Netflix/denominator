package denominator.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.forArray;
import static com.google.common.collect.Iterators.singletonIterator;
import static com.google.common.collect.Iterators.transform;
import static denominator.cli.Denominator.idOrName;
import static java.lang.String.format;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.airlift.command.OptionType;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.cli.Denominator.DenominatorCommand;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetToString;
import denominator.common.Util;
import denominator.hook.InstanceMetadataHook;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.route53.AliasTarget;

class ResourceRecordSetCommands {

    private static abstract class ResourceRecordSetCommand extends DenominatorCommand {
        @Option(type = OptionType.GROUP, required = true, name = { "-z", "--zone" }, description = "zone name (or id if ambiguous) to affect. ex. denominator.io. or EXFHEDD")
        public String zoneIdOrName;
    }

    @Command(name = "list", description = "Lists the record record sets present in this zone")
    public static class ResourceRecordSetList extends ResourceRecordSetCommand {
        @Option(type = OptionType.COMMAND, name = { "-n", "--name" }, description = "name of the record sets. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, name = { "-t", "--type" }, description = "type of the record set. ex. CNAME")
        public String type;

        public Iterator<String> doRun(DNSApiManager mgr) {
            Iterator<ResourceRecordSet<?>> iterator;
            if (name != null && type != null)
                iterator = mgr.api().recordSetsInZone(idOrName(mgr, zoneIdOrName)).iterateByNameAndType(name, type);
            else if (name != null)
                iterator = mgr.api().basicRecordSetsInZone(idOrName(mgr, zoneIdOrName)).iterateByName(name);
            else
                iterator = mgr.api().recordSetsInZone(idOrName(mgr, zoneIdOrName)).iterator();
            return transform(iterator, ResourceRecordSetToString.INSTANCE);
        }
    }

    @Command(name = "get", description = "gets a record record set by name and type (optionally by qualifier), if present in this zone")
    public static class ResourceRecordSetGet extends ResourceRecordSetCommand {
        @Option(type = OptionType.COMMAND, required = true, name = { "-n", "--name" }, description = "name of the record set. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, required = true, name = { "-t", "--type" }, description = "type of the record set. ex. CNAME")
        public String type;

        @Option(type = OptionType.COMMAND, name = { "-q", "--qualifier" }, description = "qualifier of the record set (if applicable). ex. US")
        public String qualifier;

        public Iterator<String> doRun(DNSApiManager mgr) {
            ResourceRecordSet<?> rrs;
            if (qualifier != null) {
                rrs = mgr.api().recordSetsInZone(idOrName(mgr, zoneIdOrName))
                        .getByNameTypeAndQualifier(name, type, qualifier);
            } else {
                rrs = mgr.api().basicRecordSetsInZone(idOrName(mgr, zoneIdOrName)).getByNameAndType(name, type);
            }
            return rrs != null ? singletonIterator(GeoResourceRecordSetToString.INSTANCE.apply(rrs))
                    : singletonIterator("");
        }
    }

    @Command(name = "applyttl", description = "applies the ttl to the record record set by name and type, if present in this zone")
    public static class ResourceRecordSetApplyTTL extends ResourceRecordSetCommand {
        @Option(type = OptionType.COMMAND, required = true, name = { "-n", "--name" }, description = "name of the record set. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, required = true, name = { "-t", "--type" }, description = "type of the record set. ex. CNAME")
        public String type;

        @Arguments(required = true, description = "time to live of the record set. ex. 300")
        public int ttl;

        public Iterator<String> doRun(final DNSApiManager mgr) {
            String cmd = format(";; in zone %s applying ttl %d to rrset %s %s", zoneIdOrName, ttl, name, type);
            return concat(forArray(cmd), new Iterator<String>() {
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    ResourceRecordSetApi api = mgr.api().basicRecordSetsInZone(idOrName(mgr, zoneIdOrName));
                    ResourceRecordSet<?> rrs = api.getByNameAndType(name, type);
                    if (rrs != null && rrs.ttl() != null && rrs.ttl().intValue() != ttl) {
                        api.put(ResourceRecordSet.builder()
                                                 .name(rrs.name())
                                                 .type(rrs.type())
                                                 .ttl(ttl)
                                                 .addAll(rrs.records()).build());
                    }
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

    private static abstract class ModifyRecordSetCommand extends ResourceRecordSetCommand {
        static final Pattern ELB_REGION = Pattern.compile("[^.]+\\.([^.]+)\\.elb\\.amazonaws\\.com\\.?");

        static final Map<String, String> REGION_TO_HOSTEDZONE = ImmutableMap.<String, String> builder()//
                .put("us-east-1", "Z3DZXE0Q79N41H")//
                .put("us-west-2", "Z33MTJ483KN6FU")//
                .put("eu-west-1", "Z3NF1Z3NOM5OY2")//
                .put("ap-northeast-1", "Z2YN17T5R711GT")//
                .put("ap-southeast-1", "Z1WI8VXHPB1R38")//
                .put("sa-east-1", "Z2ES78Y61JGQKS").build();

        @Option(type = OptionType.COMMAND, required = true, name = { "-n", "--name" }, description = "name of the record set. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, required = true, name = { "-t", "--type" }, description = "type of the record set. ex. CNAME")
        public String type;

        @Option(type = OptionType.COMMAND, required = false, name = { "-d", "--data" }, description = "repeat for each record value (rdata) to add. ex. 192.0.2.1")
        public List<String> values;

        @Option(type = OptionType.COMMAND, required = false, name = "--ec2-public-ipv4", description = "take data from EC2 Instance Metadata public-ipv4")
        public boolean ec2PublicIpv4;

        @Option(type = OptionType.COMMAND, required = false, name = "--ec2-public-hostname", description = "take data from EC2 Instance Metadata public-hostname")
        public boolean ec2PublicHostname;

        @Option(type = OptionType.COMMAND, required = false, name = "--ec2-local-ipv4", description = "take data from EC2 Instance Metadata local-ipv4")
        public boolean ec2LocalIpv4;

        @Option(type = OptionType.COMMAND, required = false, name = "--ec2-local-hostname", description = "take data from EC2 Instance Metadata local-hostname")
        public boolean ec2LocalHostname;

        @Option(type = OptionType.COMMAND, required = false, name = "--alias-hosted-zone-id", description = "hosted zone id of the AWS resource to alias. ex. Z3DZXE0Q79N41H")
        public String aliasHostedZoneId;

        @Option(type = OptionType.COMMAND, required = false, name = "--alias-dnsname", description = "dnsname of the AWS resource to alias. ex. nccp-cbp-frontend-12345678.us-west-2.elb.amazonaws.com.")
        public String aliasDNSName;

        @Option(type = OptionType.COMMAND, required = false, name = "--elb-dnsname", description = "dnsname of the ELB to alias. ex. nccp-cbp-frontend-12345678.us-west-2.elb.amazonaws.com.")
        public String elbDNSName;

        public URI metadataService = InstanceMetadataHook.DEFAULT_URI;

        /**
         * @throws IllegalArgumentException
         *             if an ec2 instance metadata hook was requested, but the
         *             service could not be contacted.
         */
        protected Builder<Map<String, Object>> rrsetBuilder() throws IllegalArgumentException {
            ImmutableList.Builder<String> valuesBuilder = ImmutableList.<String> builder();
            if (values != null)
                valuesBuilder.addAll(values);
            if (ec2PublicIpv4) {
                addIfPresentInMetadataService(valuesBuilder, "public-ipv4", metadataService);
            } else if (ec2PublicHostname) {
                addIfPresentInMetadataService(valuesBuilder, "public-hostname", metadataService);
            } else if (ec2LocalIpv4) {
                addIfPresentInMetadataService(valuesBuilder, "local-ipv4", metadataService);
            } else if (ec2LocalHostname) {
                addIfPresentInMetadataService(valuesBuilder, "local-hostname", metadataService);
            }
            values = valuesBuilder.build();
            checkArgument(aliasDNSName != null || elbDNSName != null || values.size() > 0, "you must pass data to add");
            Builder<Map<String, Object>> builder = ResourceRecordSet.builder().name(name).type(type);
            if (aliasDNSName != null) {
                checkArgument(aliasHostedZoneId != null, "--alias-hosted-zone-id must be present");
                checkArgument(aliasHostedZoneId.indexOf('.') == -1,
                        "--alias-hosted-zone-id must be a hosted zone id, not a zone name");
                builder.add(AliasTarget.create(aliasHostedZoneId, aliasDNSName));
            } else if (elbDNSName != null) {
                Matcher getRegion = ELB_REGION.matcher(elbDNSName);
                checkArgument(getRegion.matches(), "expected elb %s to match %s", ELB_REGION, elbDNSName);
                String hostedZoneId = REGION_TO_HOSTEDZONE.get(getRegion.group(1));
                checkArgument(hostedZoneId != null, "region %s not in configured regions: %s", getRegion.group(1),
                        REGION_TO_HOSTEDZONE.keySet());
                builder.add(AliasTarget.create(hostedZoneId, elbDNSName));
            } else {
                for (String value : values) {
                    builder.add(Util.toMap(type, value));
                }
            }
            return builder;
        }

        /**
         * @throws IllegalArgumentException
         *             if an ec2 instance metadata hook was requested, but the
         *             service could not be contacted.
         */
        private void addIfPresentInMetadataService(ImmutableList.Builder<String> valuesBuilder, String key,
                URI metadataService) throws IllegalArgumentException {
            String value = InstanceMetadataHook.get(metadataService, key);
            checkArgument(value != null, "could not retrieve %s from %s", key, metadataService);
            valuesBuilder.add(value);
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
            String cmd = format(";; in zone %s adding to rrset %s %s values: [%s]", zoneIdOrName, name, type, Joiner
                    .on(',').join(toAdd.records()));
            if (ttl != -1)
                cmd = format("%s applying ttl %d", cmd, ttl);
            return concat(forArray(cmd), new Iterator<String>() {
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    ResourceRecordSetApi api = mgr.api().basicRecordSetsInZone(idOrName(mgr, zoneIdOrName));
                    ResourceRecordSet<?> rrset = api.getByNameAndType(name, type);
                    if (rrset != null) {
                        ImmutableList<Map<String, Object>> oldRDataAsList =
                                ImmutableList.copyOf(rrset.records());
                        ImmutableList<Map<String, Object>> newRData =
                                ImmutableList.copyOf(filter(rrset.records(), not(in(toAdd.records()))));
                        if (!newRData.isEmpty()) {
                            api.put(ResourceRecordSet.builder()
                                                     .name(rrset.name())
                                                     .type(rrset.type())
                                                     .ttl(rrset.ttl())
                                                     .addAll(oldRDataAsList)
                                                     .addAll(newRData).build());
                        }
                    } else {
                        api.put(toAdd);
                    }
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
            String cmd = format(";; in zone %s replacing rrset %s %s with values: [%s]", zoneIdOrName, name, type, Joiner
                    .on(',').join(toAdd.records()));
            if (ttl != -1)
                cmd = format("%s and ttl %d", cmd, ttl);
            return concat(forArray(cmd), new Iterator<String>() {
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    mgr.api().basicRecordSetsInZone(idOrName(mgr, zoneIdOrName)).put(toAdd);
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
            String cmd = format(";; in zone %s removing from rrset %s %s values: [%s]", zoneIdOrName, name, type, Joiner
                    .on(',').join(toRemove.records()));
            return concat(forArray(cmd), new Iterator<String>() {
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    ResourceRecordSetApi api = mgr.api().basicRecordSetsInZone(idOrName(mgr, zoneIdOrName));
                    ResourceRecordSet<?> rrset = api.getByNameAndType(name, type);
                    if (rrset != null) {
                        ImmutableList<Map<String, Object>> oldRDataAsList =
                                ImmutableList.copyOf(rrset.records());
                        ImmutableList<Map<String, Object>> retainedRData =
                                ImmutableList.copyOf(filter(rrset.records(), not(in(toRemove.records()))));
                        if (retainedRData.isEmpty()) {
                            api.deleteByNameAndType(name, type);
                        } else if (!oldRDataAsList.equals(retainedRData)) {
                            api.put(ResourceRecordSet.builder()
                                                     .name(rrset.name())
                                                     .type(rrset.type())
                                                     .ttl(rrset.ttl())
                                                     .addAll(retainedRData).build());
                        }
                    }
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

    @Command(name = "delete", description = "deletes a record record set by name and type, if present in this zone")
    public static class ResourceRecordSetDelete extends ResourceRecordSetCommand {
        @Option(type = OptionType.COMMAND, required = true, name = { "-n", "--name" }, description = "name of the record set. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, required = true, name = { "-t", "--type" }, description = "type of the record set. ex. CNAME")
        public String type;

        public Iterator<String> doRun(final DNSApiManager mgr) {
            String cmd = format(";; in zone %s deleting rrset %s %s", zoneIdOrName, name, type);
            return concat(forArray(cmd), new Iterator<String>() {
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    mgr.api().basicRecordSetsInZone(idOrName(mgr, zoneIdOrName)).deleteByNameAndType(name, type);
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
            for (Map<String, Object> rdata : input.records()) {
                lines.add(format("%-50s%-7s%-20s%-6s%s", input.name(), input.type(),
                        input.qualifier() != null ? input.qualifier() : "", input.ttl(), Util.flatten(rdata)));
            }
            return Joiner.on('\n').join(lines.build());
        }
    }
}
