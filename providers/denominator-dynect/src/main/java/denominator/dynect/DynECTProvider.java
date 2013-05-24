package denominator.dynect;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static org.jclouds.Constants.PROPERTY_SESSION_INTERVAL;

import java.io.Closeable;
import java.net.URI;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.dynect.v3.DynECTApi;
import org.jclouds.dynect.v3.DynECTProviderMetadata;
import org.jclouds.location.suppliers.ProviderURISupplier;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.ConcatNormalAndGeoResourceRecordSets;

public class DynECTProvider extends BasicProvider {
    private final String url;

    public DynECTProvider() {
        this(null);
    }

    /**
     * @param url
     *            if empty or null use default
     */
    public DynECTProvider(String url) {
        url = emptyToNull(url);
        this.url = url != null ? url : new DynECTProviderMetadata().getEndpoint();
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public Multimap<String, String> getCredentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder()
                .putAll("password", "customer", "username", "password").build();
    }

    @dagger.Module(injects = DNSApiManager.class, 
                   complete = false, // denominator.Provider and denominator.Credentials
                   includes = { DynECTGeoSupport.class, 
                                ConcatNormalAndGeoResourceRecordSets.class })
    public static final class Module {

        @Provides
        @Singleton
        ZoneApi provideZoneApi(DynECTApi api) {
            return new DynECTZoneApi(api);
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(DynECTApi api) {
            return new DynECTResourceRecordSetApi.Factory(api);
        }

        @Provides
        @Singleton
        // Dynamic name updates are not currently possible in jclouds.
        DynECTApi provideApi(ConvertToJcloudsCredentials credentials, final Provider provider) {
            Properties overrides = new Properties();
            // disable url caching
            overrides.setProperty(PROPERTY_SESSION_INTERVAL, "0");
            return ContextBuilder.newBuilder(new DynECTProviderMetadata())
                                 .name(provider.getName())
                                 .credentialsSupplier(credentials)
                                 .overrides(overrides)
                                 .modules(ImmutableSet.<com.google.inject.Module> builder()
                                                      .add(new SLF4JLoggingModule())
                                                      .add(new ExecutorServiceModule(sameThreadExecutor(),
                                                                                     sameThreadExecutor()))
                                                      .add(new com.google.inject.AbstractModule() {

                                                          @Override
                                                          protected void configure() {
                                                              bind(ProviderURISupplier.class).toInstance(new ProviderURISupplier() {

                                                                  @Override
                                                                  public URI get() {
                                                                      return URI.create(provider.getUrl());
                                                                  }

                                                                  @Override
                                                                  public String toString() {
                                                                      return "DynamicURIFrom(" + provider + ")";
                                                                  }
                                                              });
                                                          }
                                                      })
                                                      .build())
                                 .buildApi(DynECTApi.class);
        }

        @Provides
        @Singleton
        Closeable provideCloseable(DynECTApi api) {
            return api;
        }
    }

    static final class ConvertToJcloudsCredentials implements Supplier<org.jclouds.domain.Credentials> {
        private javax.inject.Provider<Credentials> provider;

        @Inject
        ConvertToJcloudsCredentials(javax.inject.Provider<Credentials> provider) {
            this.provider = provider;
        }

        @Override
        public org.jclouds.domain.Credentials get() {
            ListCredentials creds = ListCredentials.class.cast(provider.get());
            return new org.jclouds.domain.Credentials(creds.get(0) + ":" + creds.get(1), creds.get(2).toString());
        }
    }
}
