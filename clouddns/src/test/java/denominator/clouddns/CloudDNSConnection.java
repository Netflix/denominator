package denominator.clouddns;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.Denominator;
import feign.Logger;

import static denominator.CredentialsConfiguration.credentials;
import static feign.Util.emptyToNull;
import static java.lang.System.getProperty;

public class CloudDNSConnection {

  final DNSApiManager manager;
  final String mutableZone;

  CloudDNSConnection() {
    String username = emptyToNull(getProperty("clouddns.username"));
    String apiKey = emptyToNull(getProperty("clouddns.apiKey"));
    if (username != null && apiKey != null) {
      manager = create(username, apiKey);
    } else {
      manager = null;
    }
    mutableZone = emptyToNull(getProperty("clouddns.zone"));
  }

  static DNSApiManager create(String username, String apiKey) {
    CloudDNSProvider provider = new CloudDNSProvider(emptyToNull(getProperty("clouddns.url")));
    return Denominator.create(provider, credentials(username, apiKey), new Overrides());
  }

  @Module(overrides = true, library = true)
  static class Overrides {

    @Provides
    @Singleton
    Logger.Level provideLevel() {
      return Logger.Level.FULL;
    }

    @Provides
    @Singleton
    Logger provideLogger() {
      return new Logger.JavaLogger().appendToFile("build/http-wire.log");
    }
  }

}
