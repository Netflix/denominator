package denominator.clouddns;

import com.google.common.collect.ImmutableMap;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials.MapCredentials;
import denominator.DNSApiManager;
import denominator.Denominator;
import feign.Logger;

import static com.google.common.base.Strings.emptyToNull;
import static denominator.CredentialsConfiguration.credentials;
import static java.lang.System.getProperty;

public class CloudDNSConnection {

  final DNSApiManager manager;
  final String mutableZone;

  CloudDNSConnection() {
    String username = emptyToNull(getProperty("clouddns.username"));
    String password = emptyToNull(getProperty("clouddns.password"));
    if (username != null && password != null) {
      manager = create(username, password);
    } else {
      manager = null;
    }
    mutableZone = emptyToNull(getProperty("clouddns.zone"));
  }

  static DNSApiManager create(String username, String password) {
    CloudDNSProvider provider = new CloudDNSProvider(emptyToNull(getProperty("clouddns.url")));
    return Denominator.create(provider,
                              credentials(MapCredentials.from(
                                  ImmutableMap.of("username", username, "password", password))),
                              new Overrides());
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
