package denominator.cli;

import static com.google.common.collect.Iterators.forArray;
import static com.google.common.collect.Iterators.transform;
import static denominator.model.ResourceRecordSets.toProfile;
import static java.lang.String.format;
import io.airlift.command.Command;
import io.airlift.command.Option;
import io.airlift.command.OptionType;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

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

    static enum GeoResourceRecordSetToString implements Function<ResourceRecordSet<?>, String> {
        INSTANCE;

        @Override
        public String apply(ResourceRecordSet<?> geoRRS) {
            Geo geo = toProfile(Geo.class).apply(geoRRS);
            StringBuilder suffix = new StringBuilder().append(geo.getName()).append(' ')
                    .append(geo.getRegions());
            ImmutableList.Builder<String> lines = ImmutableList.<String> builder();
            for (String line : Splitter.on('\n').split(ResourceRecordSetToString.INSTANCE.apply(geoRRS))) {
                lines.add(new StringBuilder().append(line).append(' ').append(suffix).toString());
            }
            return Joiner.on('\n').join(lines.build());
        }
    }

}
