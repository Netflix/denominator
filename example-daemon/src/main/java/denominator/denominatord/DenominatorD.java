package denominator.denominatord;

import com.squareup.okhttp.mockwebserver.MockWebServer;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.Provider;
import denominator.Providers;
import feign.Feign;

import static denominator.CredentialsConfiguration.anonymous;
import static denominator.CredentialsConfiguration.credentials;
import static denominator.common.Preconditions.checkArgument;
import static java.lang.System.currentTimeMillis;

public class DenominatorD {

  private static final String SYNTAX = "syntax: provider credentialArg1 credentialArg2 ...";
  private static final Logger log = Logger.getLogger(DenominatorD.class.getName());

  private final MockWebServer server;

  public DenominatorD(DNSApiManager mgr) {
    this.server = new MockWebServer();
    server.setDispatcher(new DenominatorDispatcher(mgr, new JsonCodec()));
  }

  public int start() throws IOException {
    server.start();
    return server.getPort();
  }

  public void start(int port) throws IOException {
    server.start(port);
  }

  public void shutdown() throws IOException {
    server.shutdown();
  }

  /**
   * Presents a {@link DenominatorDApi REST api} to users, by default listening on port 8080.
   */
  public static void main(final String... args) throws IOException {
    checkArgument(args.length > 0, SYNTAX);
    setupLogging();

    String portOverride = System.getenv("DENOMINATORD_PORT");
    int port = portOverride != null ? Integer.parseInt(portOverride) : 8080;
    Provider provider = Providers.getByName(args[0]);
    log.info("proxying " + provider);
    Object credentials = credentialsFromArgs(args);

    DNSApiManager mgr = Denominator.create(provider, credentials, new JavaLogger());
    new DenominatorD(mgr).start(port);
  }

  static Object credentialsFromArgs(String[] args) {
    switch (args.length) {
      case 4:
        return credentials(args[1], args[2], args[3]);
      case 3:
        return credentials(args[1], args[2]);
      case 1:
        return anonymous();
      default:
        throw new IllegalArgumentException(SYNTAX);
    }
  }

  @Module(library = true, overrides = true)
  static class JavaLogger {

    @Provides
    feign.Logger.Level provideLevel() {
      return feign.Logger.Level.BASIC;
    }

    @Provides
    feign.Logger logger() {
      return new feign.Logger.JavaLogger();
    }
  }

  static void setupLogging() {
    final long start = currentTimeMillis();
    ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(Level.FINE);
    handler.setFormatter(new Formatter() {
      @Override
      public String format(LogRecord record) {
        return String.format("%7d - %s%n", record.getMillis() - start, record.getMessage());
      }
    });

    Logger[] loggers = {
        Logger.getLogger(DenominatorD.class.getPackage().getName()),
        Logger.getLogger(feign.Logger.class.getName()),
        Logger.getLogger(MockWebServer.class.getName())
    };

    for (Logger logger : loggers) {
      logger.setLevel(Level.FINE);
      logger.setUseParentHandlers(false);
      logger.addHandler(handler);
    }
  }
}
