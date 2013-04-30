package denominator.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.forArray;
import static com.google.common.collect.Iterators.transform;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.union;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.ResourceRecordSets.toProfile;
import static java.lang.String.format;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.airlift.command.OptionType;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import denominator.DNSApiManager;
import denominator.cli.Denominator.DenominatorCommand;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetToString;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.profile.GeoResourceRecordSetApi;

class GeoResourceRecordSetCommands {

    private static abstract class GeoResourceRecordSetCommand extends DenominatorCommand {
        @Option(type = OptionType.GROUP, required = true, name = { "-z", "--zone" }, description = "zone name to affect. ex. denominator.io.")
        public String zoneName;
    }

    @Command(name = "types", description = "Lists the record types that support geo profile in this zone")
    public static class GeoTypeList extends GeoResourceRecordSetCommand {
        @Override
        protected Iterator<String> doRun(DNSApiManager mgr) {
            return mgr.getApi().getGeoResourceRecordSetApiForZone(zoneName).get().getSupportedTypes().iterator();
        }
    }

    @Command(name = "regions", description = "Lists the geo regions supported in this zone")
    public static class GeoRegionList extends GeoResourceRecordSetCommand {
        @Override
        protected Iterator<String> doRun(DNSApiManager mgr) {
            return FluentIterable
                    .from(mgr.getApi().getGeoResourceRecordSetApiForZone(zoneName).get().getSupportedRegions().asMap()
                            .entrySet()).transform(new Function<Map.Entry<String, Collection<String>>, String>() {
                        @Override
                        public String apply(Entry<String, Collection<String>> input) {
                            return format("%-28s: %s", input.getKey(), Joiner.on(';').join(input.getValue()));
                        }
                    }).iterator();
        }
    }

    @Command(name = "list", description = "Lists the geo record record sets present in this zone")
    public static class GeoResourceRecordSetList extends GeoResourceRecordSetCommand {
        @Option(type = OptionType.COMMAND, name = { "-n", "--name" }, description = "name of the record sets. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, name = { "-t", "--type" }, description = "type of the record set. (must be present with name) ex. CNAME")
        public String type;

        public Iterator<String> doRun(DNSApiManager mgr) {
            Iterator<ResourceRecordSet<?>> list;
            if (name != null && type != null)
                list = mgr.getApi().getGeoResourceRecordSetApiForZone(zoneName).get().listByNameAndType(name, type);
            if (name != null)
                list = mgr.getApi().getGeoResourceRecordSetApiForZone(zoneName).get().listByName(name);
            else
                list = mgr.getApi().getGeoResourceRecordSetApiForZone(zoneName).get().list();
            return transform(list, GeoResourceRecordSetToString.INSTANCE);
        }
    }

    @Command(name = "get", description = "Lists the geo record record set by name, type, and group, if present in this zone")
    public static class GeoResourceRecordSetGet extends GeoResourceRecordSetCommand {
        @Option(type = OptionType.COMMAND, required = true, name = { "-n", "--name" }, description = "name of the record set. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, required = true, name = { "-t", "--type" }, description = "type of the record set. ex. CNAME")
        public String type;

        @Option(type = OptionType.COMMAND, required = true, name = { "-g", "--group" }, description = "geo group of the record set. ex. US")
        public String group;

        public Iterator<String> doRun(DNSApiManager mgr) {
            GeoResourceRecordSetApi api = mgr.getApi().getGeoResourceRecordSetApiForZone(zoneName).get();
            Optional<ResourceRecordSet<?>> result = api.getByNameTypeAndGroup(name, type, group);
            return forArray(result.transform(GeoResourceRecordSetToString.INSTANCE).or(""));
        }
    }

    public static abstract class ChangeRegionInRRSetGroup extends GeoResourceRecordSetCommand {
        @Option(type = OptionType.COMMAND, required = true, name = { "-n", "--name" }, description = "name of the record set. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, required = true, name = { "-t", "--type" }, description = "type of the record set. ex. CNAME")
        public String type;

        @Option(type = OptionType.COMMAND, required = true, name = { "-g", "--group" }, description = "geo group of the record set. ex. Failover")
        public String group;

        @Option(type = OptionType.COMMAND, required = true, name = { "-r", "--region" }, description = "region in the group to update. ex. Africa")
        public String region;

        @Arguments(required = true, description = "semicolon delimited list of territories to remove.  Ex. Bouvet Island;French Southern Territories")
        public String territories;

        protected String command;

        public Iterator<String> doRun(final DNSApiManager mgr) {
            final Set<String> change = ImmutableSet.copyOf(Splitter.on(';').split(territories));
            String cmd = format(";; in zone %s %s %s from region %s in rrset %s %s %s",
                    zoneName, command, change, region, name, type, group);
            return concat(forArray(cmd), new Iterator<String>() {
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    done =  updateIfNecessary(mgr, change);
                    return ";; ok";
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            });
        }

        abstract boolean updateIfNecessary(final DNSApiManager mgr, final Set<String> change);

        Geo getGeo(DNSApiManager mgr) {
            Optional<ResourceRecordSet<?>> rrset = mgr.getApi().getGeoResourceRecordSetApiForZone(zoneName).get()
                    .getByNameTypeAndGroup(name, type, group);
            checkArgument(rrset.isPresent(), "rrset %s %s %s not found!", name, type, group);
            checkArgument(profileContainsType(Geo.class).apply(rrset.get()), "no geo profile found: %s", rrset);

            return toProfile(Geo.class).apply(rrset.get());
        }

        void update(DNSApiManager mgr, Multimap<String, String> existingRegions, Set<String> difference) {
            GeoResourceRecordSetApi api = mgr.getApi().getGeoResourceRecordSetApiForZone(zoneName).get();
            Multimap<String, String> update = LinkedHashMultimap.create(existingRegions);
            if (difference.size() == 0)
                update.removeAll(region);
            else
                update.replaceValues(region, difference);

            api.applyRegionsToNameTypeAndGroup(update, name, type, group);
        }
    }

    @Command(name = "add-to-region", description = "adds territories to a region in a geo group for a rrset, if present in this zone")
    public static class GeoResourceRecordSetAddToRegion extends ChangeRegionInRRSetGroup {
        public GeoResourceRecordSetAddToRegion() {
            this.command = "adding";
        }

        @Override
        boolean updateIfNecessary(DNSApiManager mgr, Set<String> change) {
            Multimap<String, String> existingRegions = getGeo(mgr).getRegions();
            
            if (!existingRegions.containsKey(region)) {
                update(mgr, existingRegions, change);
                return true;
            }

            ImmutableSet<String> existing = ImmutableSet.copyOf(existingRegions.get(region));
            ImmutableSet<String> union = union(existing, change).immutableCopy();
            
            if (existing.equals(union))
                return false;

            update(mgr, existingRegions, union);
            return true;
        }
    }

    @Command(name = "remove-from-region", description = "removes territories from a region in a geo group for a rrset, if present in this zone")
    public static class GeoResourceRecordSetRemoveFromRegion extends ChangeRegionInRRSetGroup {
        public GeoResourceRecordSetRemoveFromRegion() {
            this.command = "removing";
        }

        @Override
        boolean updateIfNecessary(DNSApiManager mgr, Set<String> change) {
            Multimap<String, String> existingRegions = getGeo(mgr).getRegions();
            
            if (!existingRegions.containsKey(region))
                return false;

            ImmutableSet<String> existing = ImmutableSet.copyOf(existingRegions.get(region));
            ImmutableSet<String> difference = difference(existing, change).immutableCopy();
            
            if (existing.equals(difference))
                return false;

            update(mgr, existingRegions, difference);
            return true;
        }
    }

    @Command(name = "applyttl", description = "applies the ttl to the record record set by name, type and group, if present in this zone")
    public static class GeoResourceRecordSetApplyTTL extends GeoResourceRecordSetCommand {
        @Option(type = OptionType.COMMAND, required = true, name = { "-n", "--name" }, description = "name of the record set. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, required = true, name = { "-t", "--type" }, description = "type of the record set. ex. CNAME")
        public String type;

        @Option(type = OptionType.COMMAND, required = true, name = { "-g", "--group" }, description = "geo group of the record set. ex. US")
        public String group;

        @Arguments(required = true, description = "time to live of the record set. ex. 300")
        public int ttl;

        public Iterator<String> doRun(final DNSApiManager mgr) {
            String cmd = format(";; in zone %s applying ttl %d to rrset %s %s %s", zoneName, ttl, name, type, group);
            return concat(forArray(cmd), new Iterator<String>() {
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    GeoResourceRecordSetApi api = mgr.getApi().getGeoResourceRecordSetApiForZone(zoneName).get();
                    api.applyTTLToNameTypeAndGroup(ttl, name, type, group);
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

    static enum GeoResourceRecordSetToString implements Function<ResourceRecordSet<?>, String> {
        INSTANCE;

        @Override
        public String apply(ResourceRecordSet<?> geoRRS) {
            Geo geo = toProfile(Geo.class).apply(geoRRS);
            StringBuilder suffix = new StringBuilder().append(geo.getGroup()).append(' ')
                    .append(geo.getRegions());
            ImmutableList.Builder<String> lines = ImmutableList.<String> builder();
            for (String line : Splitter.on('\n').split(ResourceRecordSetToString.INSTANCE.apply(geoRRS))) {
                lines.add(new StringBuilder().append(line).append(' ').append(suffix).toString());
            }
            return Joiner.on('\n').join(lines.build());
        }
    }

}
