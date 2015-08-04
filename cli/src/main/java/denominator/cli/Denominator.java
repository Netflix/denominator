package denominator.cli;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.bind.MapTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import dagger.ObjectGraph;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.AnonymousCredentials;
import denominator.Credentials.ListCredentials;
import denominator.Credentials.MapCredentials;
import denominator.DNSApiManager;
import denominator.Denominator.Version;
import denominator.Provider;
import denominator.Providers;
import denominator.cli.GeoResourceRecordSetCommands.GeoRegionList;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordAddRegions;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetApplyTTL;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetGet;
import denominator.cli.GeoResourceRecordSetCommands.GeoResourceRecordSetList;
import denominator.cli.GeoResourceRecordSetCommands.GeoTypeList;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetAdd;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetApplyTTL;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetDelete;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetGet;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetList;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetRemove;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetReplace;
import denominator.cli.ZoneCommands.ZoneAdd;
import denominator.cli.ZoneCommands.ZoneDelete;
import denominator.cli.ZoneCommands.ZoneList;
import denominator.cli.ZoneCommands.ZoneUpdate;
import denominator.dynect.DynECTProvider;
import denominator.model.Zone;
import denominator.ultradns.UltraDNSProvider;
import feign.Logger;
import feign.Logger.Level;
import io.airlift.airline.Cli;
import io.airlift.airline.Cli.CliBuilder;
import io.airlift.airline.Command;
import io.airlift.airline.Help;
import io.airlift.airline.Option;
import io.airlift.airline.OptionType;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.X509CertParser;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkArgument;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.String.format;

public class Denominator {

  static final TypeToken<Map<String, Object>> token = new TypeToken<Map<String, Object>>() {
  };
  static final TypeAdapter<Map<String, Object>>
      doubleToInt =
      new TypeAdapter<Map<String, Object>>() {
        TypeAdapter<Map<String, Object>>
            delegate =
            new MapTypeAdapterFactory(new ConstructorConstructor(
                Collections.<Type, InstanceCreator<?>>emptyMap()), false).create(new Gson(), token);

        @Override
        public void write(JsonWriter out, Map<String, Object> value) throws IOException {
          delegate.write(out, value);
        }

        @Override
        public Map<String, Object> read(JsonReader in) throws IOException {
          Map<String, Object> map = delegate.read(in);
          for (Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Double) {
              entry.setValue(Double.class.cast(entry.getValue()).intValue());
            }
          }
          return map;
        }
      }.nullSafe();
  // deals with scenario where gson Object type treats all numbers as doubles.
  static final Gson
      json =
      new GsonBuilder().registerTypeAdapter(token.getType(), doubleToInt).create();

  public static void main(String[] args) {
    CliBuilder<Runnable> builder = Cli.<Runnable>builder("denominator")
        .withDescription("Denominator: Portable control of DNS clouds")
        .withDefaultCommand(Help.class)
        .withCommand(Help.class)
        .withCommand(PrintVersion.class)
        .withCommand(ListProviders.class);

    builder.withGroup("zone")
        .withDescription("manage zones")
        .withDefaultCommand(ZoneList.class)
        .withCommand(ZoneList.class)
        .withCommand(ZoneAdd.class)
        .withCommand(ZoneUpdate.class)
        .withCommand(ZoneDelete.class);

    builder.withGroup("record")
        .withDescription("manage resource record sets in a zone")
        .withDefaultCommand(ResourceRecordSetList.class)
        .withCommand(ResourceRecordSetList.class)
        .withCommand(ResourceRecordSetGet.class)
        .withCommand(ResourceRecordSetAdd.class)
        .withCommand(ResourceRecordSetApplyTTL.class)
        .withCommand(ResourceRecordSetReplace.class)
        .withCommand(ResourceRecordSetRemove.class)
        .withCommand(ResourceRecordSetDelete.class);

    builder.withGroup("geo")
        .withDescription("manage geo resource record sets in a zone")
        .withDefaultCommand(GeoResourceRecordSetList.class)
        .withCommand(GeoTypeList.class)
        .withCommand(GeoRegionList.class)
        .withCommand(GeoResourceRecordSetList.class)
        .withCommand(GeoResourceRecordSetGet.class)
        .withCommand(GeoResourceRecordSetApplyTTL.class)
        .withCommand(GeoResourceRecordAddRegions.class);

    Cli<Runnable> denominatorParser = builder.build();
    try {
      denominatorParser.parse(args).run();
    } catch (RuntimeException e) {
      if (e instanceof NullPointerException) {
        e.printStackTrace();
      }
      System.err.println(";; error: " + e.getMessage());
      System.exit(1);
    }
    System.exit(0);
  }

  /**
   * Returns a log configuration module or null if none is needed.
   */
  static Object logModule(boolean quiet, boolean verbose) {
    checkArgument(!(quiet && verbose), "quiet and verbose flags cannot be used at the same time!");
    Logger.Level logLevel;
    if (quiet) {
      return null;
    } else if (verbose) {
      logLevel = Logger.Level.FULL;
    } else {
      logLevel = Logger.Level.BASIC;
    }
    return new LogModule(logLevel);
  }

  static String id(DNSApiManager mgr, String zoneIdOrName) {
    if (zoneIdOrName.indexOf('.') == -1) { // Assume that ids don't have dots in them!
      return zoneIdOrName;
    }
    if (zoneNameIsId(mgr.provider())) {
      return zoneIdOrName;
    }
    Iterator<Zone> result = mgr.api().zones().iterateByName(zoneIdOrName);
    checkArgument(result.hasNext(), "zone %s not found", zoneIdOrName);
    return result.next().id();
  }

  // Special-case providers known to use zone names as ids, as this usually saves 1-200ms of
  // lookups. We can later introduce a flag or other means to help third-party providers.
  static boolean zoneNameIsId(Provider provider) {
    return provider instanceof UltraDNSProvider || provider instanceof DynECTProvider;
  }

  @Command(name = "version", description = "output the version of denominator and java runtime in use")
  public static class PrintVersion implements Runnable {

    public void run() {
      System.out.println("Denominator " + Version.INSTANCE);
      System.out.println("Java version: " + System.getProperty("java.version"));
    }
  }

  @Command(name = "providers", description = "List the providers and their metadata ")
  public static class ListProviders implements Runnable {

    final static String table = "%-10s %-51s %-14s %-14s %s%n";

    public static String providerAndCredentialsTable() {
      StringBuilder builder = new StringBuilder();

      builder.append(format(
          table, "provider", "url", "duplicateZones", "credentialType", "credentialArgs"));
      for (Provider p : ImmutableSortedSet.copyOf(Ordering.usingToString(), Providers.list())) {
        if (p.credentialTypeToParameterNames().isEmpty()) {
          builder.append(
              format("%-10s %-51s %-14s %n", p.name(), p.url(), p.supportsDuplicateZoneNames()));
        }
        for (Entry<String, Collection<String>> e : p.credentialTypeToParameterNames().entrySet()) {
          String params = Joiner.on(' ').join(e.getValue());
          builder.append(format(
              table, p.name(), p.url(), p.supportsDuplicateZoneNames(), e.getKey(), params));
        }
      }
      return builder.toString();
    }

    public void run() {
      System.out.println(providerAndCredentialsTable());
    }
  }

  public static abstract class DenominatorCommand implements Runnable {

    private static final String ENV_PREFIX = "DENOMINATOR_";
    @Option(type = OptionType.GLOBAL, name = {"-q",
                                              "--quiet"}, description = "do not emit informational messages about http commands invoked")
    public boolean quiet;
    @Option(type = OptionType.GLOBAL, name = {"-v",
                                              "--verbose"}, description = "emit details such as http requests sent and responses received")
    public boolean verbose;
    @Option(type = OptionType.GLOBAL, name = {"-p",
                                              "--provider"}, description = "provider to affect")
    public String providerName;
    @Option(type = OptionType.GLOBAL, name = {"-u",
                                              "--url"}, description = "alternative api url to connect to")
    public String url;
    @Option(type = OptionType.GLOBAL, name = {"-c",
                                              "--credential"}, description = "adds a credential argument (execute denominator providers for what these are)")
    public List<String> credentialArgs;
    @Option(type = OptionType.GLOBAL, name = {"-C",
                                              "--config"}, description = "path to configuration file (used to store credentials). default: ~/.denominatorconfig")
    public String configPath = "~/.denominatorconfig";
    @Option(type = OptionType.GLOBAL, name = {"-n",
                                              "--configuration-name"}, description = "unique name of provider configuration")
    public String providerConfigurationName;
    protected Credentials credentials = AnonymousCredentials.INSTANCE;

    @SuppressWarnings("unchecked")
    public void run() {
      setProxyFromEnv();

      if (providerName != null && credentialArgs != null) {
        credentials = ListCredentials.from(Lists.transform(credentialArgs, decodeAnyPems));
      } else if (providerConfigurationName != null) {
        Map<?, ?> configFromFile = getConfigFromFile();
        if (configFromFile != null) {
          credentials = MapCredentials.from(
              Maps.transformValues(Map.class.cast(configFromFile.get("credentials")), decodeAnyPems));
          providerName = configFromFile.get("provider").toString();
          if (configFromFile.containsKey("url")) {
            url = configFromFile.get("url").toString();
          }
        }
      } else {
        overrideFromEnv(System.getenv());
      }
      Provider provider = Providers.getByName(providerName);
      if (url != null) {
        provider = Providers.withUrl(provider, url);
      }

      Builder<Object> modulesForGraph = ImmutableList.builder() //
          .add(Providers.provide(provider)) //
          .add(Providers.instantiateModule(provider));

      Object logModule = logModule(quiet, verbose);
      if (logModule != null) {
        modulesForGraph.add(logModule);
      }

      if (credentials != AnonymousCredentials.INSTANCE) {
        modulesForGraph.add(credentials(credentials));
      }
      DNSApiManager mgr = null;
      try {
        mgr = ObjectGraph.create(modulesForGraph.build().toArray()).get(DNSApiManager.class);
        for (Iterator<String> i = doRun(mgr); i.hasNext(); ) {
          System.out.println(i.next());
        }
      } finally {
        if (mgr != null) {
          try {
            mgr.close();
          } catch (IOException ignored) {

          }
        }
      }
    }

    private static final Function<Object, Object> maybeDecodeX509Pem = new Function<Object, Object>() {
      @Override
      public Object apply(Object input) {
        if (input instanceof String && input.toString().contains("BEGIN CERTIFICATE")) {
          try {
            X509CertParser x509CertParser = new X509CertParser();
            x509CertParser.engineInit(new ByteArrayInputStream(input.toString().getBytes()));
            return x509CertParser.engineRead();
          } catch (Exception ex) {
            return input;
          }
        }
        return input;
      }
    };

    private static final Function<Object, Object> maybeDecodePrivateKeyPem = new Function<Object, Object>() {
      @Override
      public Object apply(Object input) {
        if (input instanceof String && input.toString().contains("BEGIN RSA PRIVATE KEY")) {
          try {
            PEMKeyPair pemKeyPair = (PEMKeyPair) new PEMParser(new StringReader(input.toString())).readObject();
            PrivateKeyInfo privateKeyInfo = pemKeyPair.getPrivateKeyInfo();
            KeyFactory keyFact = KeyFactory.getInstance(
                privateKeyInfo.getPrivateKeyAlgorithm().getAlgorithm().getId(), new BouncyCastleProvider());
            return keyFact.generatePrivate(new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded()));
          } catch (Exception ex) {
            return input;
          }
        }
        return input;
      }
    };

    private static final Function<Object, Object> decodeAnyPems =
        Functions.compose(maybeDecodeX509Pem, maybeDecodePrivateKeyPem);

    /**
     * Load configuration for given providerConfigurationName from a YAML configuration file.
     */
    Map<?, ?> getConfigFromFile() {
      if (configPath == null) {
        return null;
      }
      String configFileContent = null;
      try {
        configFileContent = getFileContentsFromPath(configPath);
      } catch (IOException e) {
        System.err.println("configuration file not found: " + e.getMessage());
        System.exit(1);
      }
      return getConfigFromYaml(configFileContent);
    }

    Map<?, ?> getConfigFromYaml(String yamlAsString) {
      Yaml yaml = new Yaml();
      Iterable<Object> configs = yaml.loadAll(yamlAsString);
      Object providerConf = FluentIterable.from(configs).firstMatch(new Predicate<Object>() {
        @Override
        public boolean apply(Object input) {
          return providerConfigurationName.equals(Map.class.cast(input).get("name"));
        }
      }).get();
      return Map.class.cast(providerConf);
    }

    String getFileContentsFromPath(String path) throws IOException {
      if (path.startsWith("~")) {
        path = System.getProperty("user.home") + path.substring(1);
      }
      return Files.toString(new File(path), Charsets.UTF_8);
    }

    void overrideFromEnv(Map<String, String> env) {
      if (providerName == null) {
        providerName = env.get(ENV_PREFIX + "PROVIDER");
      }
      if (url == null) {
        url = env.get(ENV_PREFIX + "URL");
      }

      Provider providerLoaded = Providers.getByName(providerName);
      if (providerLoaded != null) {
        Map<String, String> credentialMap = new LinkedHashMap<String, String>();
        // merge the list of possible credentials
        for (Entry<String, Collection<String>> entry :
            providerLoaded.credentialTypeToParameterNames().entrySet()) {
          for (String paramName : entry.getValue()) {
            String
                upperParamName =
                CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, paramName);
            String value = env.get(ENV_PREFIX + upperParamName);
            if (value != null) {
              credentialMap.put(paramName, value);
            }
          }
        }
        if (!credentialMap.isEmpty()) {
          credentials = MapCredentials.from(credentialMap);
        }
      }
    }

    static void setProxyFromEnv() {
      setProtocolProxyFromEnv("http", System.getenv("HTTP_PROXY"));
      setProtocolProxyFromEnv("https", System.getenv("HTTPS_PROXY"));
    }

    static void setProtocolProxyFromEnv(String proto, String envProxy) {
      if (envProxy != null && !envProxy.isEmpty()) {
        try {
          URL proxyUrl = new URL(envProxy);

          String proxyHost = System.getProperty(proto + ".proxyHost");
          if ((proxyHost == null || proxyHost.isEmpty())) {
            System.setProperty(proto + ".proxyHost", proxyUrl.getHost());
            System.setProperty(proto + ".proxyPort",
                Integer.toString(
                    proxyUrl.getPort() == -1 ? proxyUrl.getDefaultPort() : proxyUrl.getPort()));
          }
        } catch (MalformedURLException e) {
          System.err.println("invalid " + proto + " proxy configuration: " + e.getMessage());
          System.exit(1);
        }
      }
    }

    /**
     * return a lazy iterator where possible to improve the perceived responsiveness of the cli
     */
    protected abstract Iterator<String> doRun(DNSApiManager mgr);
  }

  @dagger.Module(overrides = true, library = true)
  static class LogModule {

    final Logger.Level logLevel;

    LogModule(Level logLevel) {
      this.logLevel = logLevel;
    }

    @Provides
    @Singleton
    Logger logger() {
      return new Logger.ErrorLogger();
    }

    @Provides
    @Singleton
    Logger.Level level() {
      return logLevel;
    }
  }
}
