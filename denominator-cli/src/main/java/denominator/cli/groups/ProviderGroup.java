package denominator.cli.groups;

import static dagger.Provides.Type.SET;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.yaml.snakeyaml.Yaml;

import dagger.Provides;
import denominator.Provider;
import denominator.cli.Action;
import denominator.cli.Command;
import denominator.cli.Group;
import denominator.cli.SupportedProviders;
import denominator.cli.codec.YamlCodec;

public class ProviderGroup implements Group {

    @Override
    public String name() {
        return "provider";
    }

    @Override
    public boolean needsDNSApi() {
        return false;
    }

    @Override
    public Object module() {
        return new Module();
    }

    @dagger.Module(library = true, includes = { YamlCodec.class, SupportedProviders.class })
    static class Module {
        @Provides
        @Named("url")
        String url() {
            return null;
        }

        @Provides(type = SET)
        Command get(ListProviders listProviders) {
            return listProviders;
        }
    }

    static class ListProviders implements Command {

        @Override
        public String description() {
            return "List the providers and their metadata ";
        }

        @Override
        public Action action() {
            return Action.GET;
        }

        @Inject
        Set<Provider> providers;

        @Inject
        Yaml yaml;

        @Override
        public void execute(Writer writer) throws IOException {
            yaml.dumpAll(providers.iterator(), writer);
        }
    }
}
