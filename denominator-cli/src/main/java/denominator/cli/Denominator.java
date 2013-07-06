package denominator.cli;

import static denominator.CredentialsConfiguration.credentials;
import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.yaml.snakeyaml.Yaml;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.MapCredentials;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.cli.codec.YamlCodec;
import denominator.model.Zone;

// library is true as DNSApiManager is exposed in case certain commands need it.
@dagger.Module(library = true, includes = { YamlCodec.class, SupportedProviders.class })
public class Denominator {

    @Option(name = "-p", aliases = "--provider", usage = "provider to affect")
    private String providerName;

    @Option(name = "-u", aliases = "--url", usage = "alternative api url to connect to")
    private String url;

    @Option(name = "-c", aliases = "--credential", usage = "adds a credential argument (execute denominator providers for what these are)")
    private List<String> credentialArgs;

    @Option(name = "-C", aliases = "--config", usage = "path to configuration file (used to store credentials)")
    private String configPath;

    @Option(name = "-n", aliases = "--name", usage = "unique name of provider configuration")
    private String name;

    @Argument(handler = GroupHandler.class)
    Group group;

    public static void main(String[] args) throws IOException, CmdLineException {
        try {
            execute(new PrintWriter(System.out), args);
        } catch (RuntimeException e) {
            System.err.println(";; error: " + e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }

    public static void execute(Writer writer, String... args) throws IOException, CmdLineException {
        execute(writer, args, (Object[]) null);
    }

    public static void execute(Writer writer, String[] args, Object... modules) throws IOException, CmdLineException {
        Denominator denominator = new Denominator();
        new CmdLineParser(denominator).parseArgument(args);
        int groupIndex = -1;
        Action action = Action.GET;
        List<String> scopedArgs = new ArrayList<String>();
        OUTER: for (int i = 0; i < args.length; i++) {
            if (args[i].equals(denominator.group.name())) {
                groupIndex = i;
                continue OUTER;
            } else {
                for (Action a : Action.values()) {
                    if (args[i].equalsIgnoreCase(a.name())) {
                        action = a;
                        continue OUTER;
                    }
                }
                if (args[i].equalsIgnoreCase("list")) {
                    // support old name
                    continue OUTER;
                }
                if (groupIndex != -1)
                    scopedArgs.add(args[i]);
            }
        }

        @Module(injects = Commands.class, complete = false)
        class Commands {
            @Inject
            Set<Command> get;
        }
        Commands commands = new Commands();
        List<Object> modulesForGraph = new ArrayList<Object>(3);
        modulesForGraph.add(denominator.group.module());
        modulesForGraph.add(commands);
        if (denominator.group.needsDNSApi()) {
            modulesForGraph.add(denominator);
        }
        if (modules != null) {
            for (Object module : modules) {
                modulesForGraph.add(module);
            }
        }
        ObjectGraph.create(modulesForGraph.toArray()).inject(commands);

        List<Action> actionsOnGroup = new ArrayList<Action>();
        for (Command command : commands.get) {
            if (action.equals(command.action())) {
                new CmdLineParser(command).parseArgument(scopedArgs.toArray(new String[] {}));
                command.execute(writer);
                return;
            } else {
                actionsOnGroup.add(command.action());
            }
        }
        throw new IllegalArgumentException(format("action %s not available on %s; valid choices: %s", action,
                denominator.group.name(), actionsOnGroup));
    }

    @Provides
    @Named("url")
    String url(@SuppressWarnings("rawtypes") Map configFromFile) {
        return configFromFile.containsKey("url") ? url = configFromFile.get("url").toString() : url;
    }

    // visible for testing
    static interface ReaderForPath {
        Reader apply(String path);
    }

    static class FileReaderForPath implements ReaderForPath {
        @Override
        public Reader apply(String path) {
            try {
                return new FileReader(new File(path));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("config file not found: " + path);
            }
        }
    }

    @Provides
    ReaderForPath readerForPath() {
        return new FileReaderForPath();
    }

    @SuppressWarnings("rawtypes")
    @Provides
    @Singleton
    Map configFromFile(ReaderForPath readerForPath, Yaml yaml) {
        return configPath != null ? yamlConfig(readerForPath, yaml) : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    @Provides
    @Singleton
    DNSApiManager dnsApiManager(Set<Provider> providers, @SuppressWarnings("rawtypes") Map configFromFile) {
        Credentials credentials = null;
        if (providerName != null && credentialArgs != null) {
            credentials = Credentials.ListCredentials.from(credentialArgs);
        } else if (!configFromFile.isEmpty()) {
            credentials = MapCredentials.from(Map.class.cast(configFromFile.get("credentials")));
            providerName = configFromFile.get("provider").toString();
        }
        List<String> providerNames = new ArrayList<String>(providers.size());
        for (Provider provider : providers) {
            if (provider.name().equals(providerName)) {
                if (credentials != null) {
                    return denominator.Denominator.create(provider, credentials(credentials));
                }
                return denominator.Denominator.create(provider);
            } else {
                providerNames.add(provider.name());
            }
        }
        throw new IllegalArgumentException(format("invalid provider name %s! valid choices: %s", providerName,
                providerNames));
    }

    /**
     * Load configuration for given name from a YAML configuration file.
     */
    Map<?, ?> yamlConfig(ReaderForPath readerForPath, Yaml yaml) {
        if (configPath == null)
            return null;
        if (configPath.startsWith("~"))
            configPath = System.getProperty("user.home") + configPath.substring(1);
        Reader reader = readerForPath.apply(configPath);
        List<Object> names = new ArrayList<Object>();
        for (Object configObject : yaml.loadAll(reader)) {
            Map<?, ?> config = Map.class.cast(configObject);
            Object configName = config.get("name");
            if (configName.equals(name)) {
                return config;
            }
            names.add(configName);
        }
        throw new IllegalArgumentException(format("invalid name %s! valid choices: %s", name, names));
    }

    public static String idOrName(DNSApiManager mgr, String zoneIdOrName) {
        if (zoneIdOrName.indexOf('.') != -1) {
            return zoneIdOrName;
        }
        List<Zone> currentZones = new ArrayList<Zone>();
        for (Zone zone : mgr.api().zones()) {
            if (zoneIdOrName.equals(zone.name()))
                return zone.id();
            currentZones.add(zone);
        }
        throw new IllegalArgumentException(format("zone %s not found in %s", zoneIdOrName, currentZones));
    }
}
