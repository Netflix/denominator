package denominator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import feign.Logger;

import static feign.Util.emptyToNull;

public final class DNSApiManagerFactory {

  public static DNSApiManager create(denominator.Provider provider) {
    List<Object> modules = new ArrayList<Object>(2);
    modules.add(credentialsModule(provider));
    for (Class<?> inner : provider.getClass().getDeclaredClasses()) {
      if (inner.getSimpleName().equals("FeignModule")) {
        modules.add(new HttpLog());
      }
    }
    return Denominator.create(provider, modules.toArray());
  }

  /**
   * Looks for {@link denominator.Provider#credentialTypeToParameterNames() credential parameters}
   * in system properties as {@code ${provider.name}.${parameter}.
   */
  static Object credentialsModule(denominator.Provider provider) {
    Map<String, String> credentials = new LinkedHashMap<String, String>(3);
    for (Collection<String> parameters : provider.credentialTypeToParameterNames().values()) {
      for (String parameter : parameters) {
        String systemProperty = provider.name() + "." + parameter;
        String value = emptyToNull(System.getProperty(systemProperty));
        if (value != null) {
          credentials.put(parameter, value);
        }
      }
    }
    return CredentialsConfiguration.credentials(Credentials.MapCredentials.from(credentials));
  }

  @Module(overrides = true, library = true)
  public static final class HttpLog {

    @Provides
    @Singleton
    Logger.Level provideLevel() {
      return Logger.Level.FULL;
    }

    @Provides
    @Singleton
    Logger provideLogger() {
      new File(System.getProperty("user.dir"), "build").mkdirs();
      return new Logger.JavaLogger().appendToFile("build/http-wire.log");
    }
  }
}
