package denominator.cli.groups;

import static dagger.Provides.Type.SET;

import java.io.IOException;
import java.io.Writer;

import javax.inject.Inject;

import org.yaml.snakeyaml.Yaml;

import dagger.Provides;
import denominator.DNSApiManager;
import denominator.cli.Action;
import denominator.cli.Command;
import denominator.cli.Group;
import denominator.cli.codec.YamlCodec;

public class ZoneGroup implements Group {

    @Override
    public String name() {
        return "zone";
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
        Command listZones(ListZones in) {
            return in;
        }
    }

    static class ListZones implements Command {
        @Override
        public String description() {
            return "Lists the zones present in this provider";
        }

        @Override
        public Action action() {
            return Action.GET;
        }

        @Inject
        DNSApiManager mgr;

        @Inject
        Yaml yaml;

        @Override
        public void execute(Writer writer) throws IOException {
            yaml.dumpAll(mgr.api().zones().iterator(), writer);
        }
    }
}
