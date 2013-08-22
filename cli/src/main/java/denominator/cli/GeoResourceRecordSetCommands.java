package denominator.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.forArray;
import static com.google.common.collect.Iterators.singletonIterator;
import static com.google.common.collect.Iterators.transform;
import static denominator.cli.Denominator.idOrName;
import static denominator.cli.Denominator.json;
import static denominator.model.profile.Geos.withAdditionalRegions;
import static java.lang.String.format;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.airlift.command.OptionType;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.gson.reflect.TypeToken;

import denominator.DNSApiManager;
import denominator.cli.Denominator.DenominatorCommand;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetToString;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;

class GeoResourceRecordSetCommands {

    private static abstract class GeoResourceRecordSetCommand extends DenominatorCommand {
        @Option(type = OptionType.GROUP, required = true, name = { "-z", "--zone" }, description = "zone name (or id if ambiguous) to affect. ex. denominator.io. or EXFHEDD")
        public String zoneIdOrName;
    }

    @Command(name = "types", description = "Lists the record types that support geo profile in this zone")
    public static class GeoTypeList extends GeoResourceRecordSetCommand {
        @Override
        protected Iterator<String> doRun(DNSApiManager mgr) {
            return mgr.provider().profileToRecordTypes().get("geo").iterator();
        }
    }

    @Command(name = "regions", description = "Lists the geo regions supported in this zone")
    public static class GeoRegionList extends GeoResourceRecordSetCommand {
        @Override
        protected Iterator<String> doRun(DNSApiManager mgr) {
            return FluentIterable
                    .from(mgr.api().geoRecordSetsInZone(idOrName(mgr, zoneIdOrName)).supportedRegions()
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
            Iterator<ResourceRecordSet<?>> iterator;
            if (name != null && type != null)
                iterator = mgr.api().geoRecordSetsInZone(idOrName(mgr, zoneIdOrName)).iterateByNameAndType(name, type);
            if (name != null)
                iterator = mgr.api().geoRecordSetsInZone(idOrName(mgr, zoneIdOrName)).iterateByName(name);
            else
                iterator = mgr.api().geoRecordSetsInZone(idOrName(mgr, zoneIdOrName)).iterator();
            return transform(iterator, GeoResourceRecordSetToString.INSTANCE);
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
            GeoResourceRecordSetApi api = mgr.api().geoRecordSetsInZone(idOrName(mgr, zoneIdOrName));
            ResourceRecordSet<?> rrs = api.getByNameTypeAndQualifier(name, type, group);
            return rrs != null ? singletonIterator(GeoResourceRecordSetToString.INSTANCE.apply(rrs))
                    : singletonIterator("");
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
            String cmd = format(";; in zone %s applying ttl %d to rrset %s %s %s", zoneIdOrName, ttl, name, type, group);
            return concat(forArray(cmd), new Iterator<String>() {
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    GeoResourceRecordSetApi api = mgr.api().geoRecordSetsInZone(idOrName(mgr, zoneIdOrName));
                    ResourceRecordSet<?> rrs = api.getByNameTypeAndQualifier(name, type, group);
                    if (rrs != null && rrs.ttl() != null && rrs.ttl().intValue() != ttl) {
                        api.put(ResourceRecordSet.<Map<String, Object>> builder()
                                                 .name(name)
                                                 .type(type)
                                                 .qualifier(group)
                                                 .ttl(ttl)
                                                 .weighted(rrs.weighted())
                                                 .geo(rrs.geo())
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

    @Command(name = "add", description = "adds the regions or territories specified to the geo record set")
    public static class GeoResourceRecordAddRegions extends GeoResourceRecordSetCommand {

        @Option(type = OptionType.COMMAND, required = true, name = { "-n", "--name" }, description = "name of the record set. ex. www.denominator.io.")
        public String name;

        @Option(type = OptionType.COMMAND, required = true, name = { "-t", "--type" }, description = "type of the record set. ex. CNAME")
        public String type;

        @Option(type = OptionType.COMMAND, required = true, name = { "-g", "--group" }, description = "geo group of the record set. ex. US-WEST-2")
        public String group;

        @Option(type = OptionType.COMMAND, required = true, name = { "-r", "--regions" }, description = "regions to add in json. ex. {\"Mexico\":[\"Mexico\"],\"South America\":[\"Ecuador\"]}")
        public String regions;

        @Option(type = OptionType.COMMAND, required = false, name = { "--dry-run" }, description = "when true, don't actually perform the update")
        public Boolean dryRun;

        @Option(type = OptionType.COMMAND, required = false, name = { "--validate-regions" }, description = "when true, check to ensure that the regions specified are indeed supported")
        public Boolean validateRegions;

        public Iterator<String> doRun(final DNSApiManager mgr) {
            checkArgument(!regions.isEmpty(), "specify regions to apply");
            // resolve into a concrete zone id or name.
            zoneIdOrName = idOrName(mgr, zoneIdOrName);
            final GeoResourceRecordSetApi api = mgr.api().geoRecordSetsInZone(zoneIdOrName);
            checkArgument(api != null, "geo api not available on provider %s, zone %s", mgr.provider(), zoneIdOrName);
            String cmd = format(";; in zone %s adding regions %s to rrset %s %s %s", zoneIdOrName, regions, name, type,
                    group);
            final Map<String, Collection<String>> regionsToAdd  = parseRegions();

            return concat(forArray(cmd), new Iterator<String>() {
                boolean validatedRegions;
                ResourceRecordSet<?> existing = null;
                ResourceRecordSet<?> update = null;
                boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public String next() {
                    if (Boolean.TRUE.equals(validateRegions) && !validatedRegions) {
                        validateRegions(regionsToAdd, api.supportedRegions());
                        validatedRegions = true;
                        return ";; validated regions: " + json.toJson(regionsToAdd);
                    } else if (existing == null) {
                        existing = api.getByNameTypeAndQualifier(name, type, group);
                        return ";; current rrset: " + json.toJson(existing);
                    } else if (update == null) {
                        update = withAdditionalRegions(existing, regionsToAdd);
                        if (update == existing) {
                            done = true;
                            return ";; ok";
                        }
                        return ";; revised rrset: " + json.toJson(update);
                    } else {
                        if (!Boolean.TRUE.equals(dryRun)) {
                            api.put(update);
                        }
                        done = true;
                        return ";; ok";
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            });
        }

        private Map<String, Collection<String>> parseRegions() {
            try {
                return json.fromJson(regions, new TypeToken<Map<String, Collection<String>>>() {
                }.getType());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "parse failure on regions! check json syntax. ex. {\"United States (US)\":[\"Arizona\"]}", e);
            }
        }
    }

    static void validateRegions(Map<String, Collection<String>> regionsToAdd,
            Map<String, Collection<String>> supportedRegions) {
        MapDifference<String, Collection<String>> comparison = Maps.difference(regionsToAdd, supportedRegions);
        checkArgument(comparison.entriesOnlyOnLeft().isEmpty(), "unsupported regions: %s", comparison
                .entriesOnlyOnLeft().keySet());
        for (Entry<String, Collection<String>> entry : regionsToAdd.entrySet()) {
            ImmutableSet<String> toAdd = ImmutableSet.copyOf(entry.getValue());
            SetView<String> intersection = Sets.intersection(toAdd,
                    ImmutableSet.copyOf(supportedRegions.get(entry.getKey())));
            SetView<String> unsupported = Sets.difference(toAdd, intersection);
            checkArgument(unsupported.isEmpty(), "unsupported territories in %s:", entry.getKey(), unsupported);
        }
    }

    static enum GeoResourceRecordSetToString implements Function<ResourceRecordSet<?>, String> {
        INSTANCE;

        @Override
        public String apply(ResourceRecordSet<?> rrset) {
            ImmutableList.Builder<String> lines = ImmutableList.<String> builder();
            for (String line : Splitter.on('\n').split(ResourceRecordSetToString.INSTANCE.apply(rrset))) {
                if (rrset.geo() != null) {
                    lines.add(new StringBuilder().append(line).append(' ').append(json.toJson(rrset.geo().regions()))
                            .toString());
                } else {
                    lines.add(line);
                }
            }
            return Joiner.on('\n').join(lines.build());
        }
    }
}
