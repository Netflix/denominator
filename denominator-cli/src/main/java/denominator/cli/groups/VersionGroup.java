package denominator.cli.groups;

import static dagger.Provides.Type.SET;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import javax.inject.Named;

import dagger.Provides;
import denominator.Denominator.Version;
import denominator.cli.Action;
import denominator.cli.Command;
import denominator.cli.Group;

public class VersionGroup implements Group {

    @Override
    public String name() {
        return "version";
    }

    @Override
    public boolean needsDNSApi() {
        return false;
    }

    @Override
    public Object module() {
        return new Module();
    }

    @dagger.Module(library = true)
    static class Module {
        @Provides
        @Named("url")
        String url() {
            return null;
        }

        @Provides(type = SET)
        Command get() {
            return new PrintVersion();
        }
    }

    static class PrintVersion implements Command {

        @Override
        public String description() {
            return "output the version of denominator and java runtime in use";
        }

        @Override
        public Action action() {
            return Action.GET;
        }

        @Override
        public void execute(Writer writer) throws IOException {
            @SuppressWarnings("resource")
            PrintWriter out = writer instanceof PrintWriter ? PrintWriter.class.cast(writer) : new PrintWriter(writer);
            out.println("Denominator " + Version.INSTANCE);
            out.println("Java version: " + System.getProperty("java.version"));
        }
    }
}
