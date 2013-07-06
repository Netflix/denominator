package denominator.cli.groups;

import static dagger.Provides.Type.SET;
import static denominator.cli.Denominator.idOrName;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.yaml.snakeyaml.Yaml;

import dagger.Provides;
import denominator.DNSApiManager;
import denominator.cli.Action;
import denominator.cli.Command;
import denominator.cli.Group;
import denominator.cli.codec.YamlCodec;
import denominator.model.ResourceRecordSet;

public class ResourceRecordSetGroup implements Group {

    @Override
    public String name() {
        return "rrset";
    }

    @Override
    public boolean needsDNSApi() {
        return true;
    }

    @Override
    public Object module() {
        return new Module();
    }

    @dagger.Module(library = true, includes = YamlCodec.class, complete = false)
    static class Module {
        @Provides(type = SET)
        Command listResourceRecordSets(ListResourceRecordSets in) {
            return in;
        }

    }

    static class ListResourceRecordSets implements Command {
        @Override
        public String description() {
            return "Lists the record record sets present in this zone";
        }

        @Override
        public Action action() {
            return Action.GET;
        }

        @Option(required = true, name = "-z", aliases = "--zone", usage = "zone name (or id if ambiguous) to affect. ex. denominator.io. or EXFHEDD")
        private String zoneIdOrName;

        @Option(name = "-n", aliases = "--name", usage = "name of the record set. ex. www.denominator.io.")
        public String name;

        @Option(name = "-t", aliases = "--type", usage = "type of the record set. ex. CNAME")
        public String type;

        @Option(name = "-q", aliases = "--qualifier", usage = "qualifier of the record set (if applicable). ex. US")
        public String qualifier;

        @Inject
        DNSApiManager mgr;

        @Inject
        Yaml yaml;

        @Override
        public void execute(Writer writer) throws IOException {
            if (name != null && type != null && qualifier != null) {
                yaml.dump(
                        mgr.api().recordSetsInZone(idOrName(mgr, zoneIdOrName))
                                .getByNameTypeAndQualifier(name, type, qualifier), writer);
                return;
            }
            Iterator<ResourceRecordSet<?>> iterator;
            if (name != null && type != null)
                iterator = mgr.api().recordSetsInZone(idOrName(mgr, zoneIdOrName)).iterateByNameAndType(name, type);
            else if (name != null)
                iterator = mgr.api().recordSetsInZone(idOrName(mgr, zoneIdOrName)).iterateByName(name);
            else
                iterator = mgr.api().recordSetsInZone(zoneIdOrName).iterator();
            yaml.dumpAll(iterator, writer);
        }
    }
}
