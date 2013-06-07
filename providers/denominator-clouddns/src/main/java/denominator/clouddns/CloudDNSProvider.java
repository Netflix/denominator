package denominator.clouddns;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static org.jclouds.Constants.PROPERTY_SESSION_INTERVAL;

import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.location.suppliers.ProviderURISupplier;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApiMetadata;
import org.jclouds.rackspace.clouddns.v1.functions.RecordsToPagedIterable;
import org.jclouds.rest.internal.GeneratedHttpRequest;

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
import denominator.config.GeoUnsupported;
import denominator.config.OnlyBasicResourceRecordSets;

public class CloudDNSProvider extends BasicProvider {
    private final String url;

    public CloudDNSProvider() {
        this(null);
    }

    /**
     * @param url
     *            if empty or null use default
     */
    public CloudDNSProvider(String url) {
        url = emptyToNull(url);
        this.url = url != null ? url : new CloudDNSApiMetadata().getDefaultEndpoint().get();
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public boolean supportsDuplicateZoneNames() {
        return true;
    }

    @Override
    public Multimap<String, String> credentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder().putAll("apiKey", "username", "apiKey").build();
    }

    @dagger.Module(injects = DNSApiManager.class,
                   complete = false, // denominator.Provider and denominator.Credentials
                   includes = { GeoUnsupported.class, 
                                OnlyBasicResourceRecordSets.class } )
    public static final class Module {

        @Provides
        @Singleton
        ZoneApi provideZoneApi(CloudDNSApi api) {
            return new CloudDNSZoneApi(api);
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(CloudDNSApi api) {
            return new CloudDNSResourceRecordSetApi.Factory(api);
        }

        @Provides
        @Singleton
        // Dynamic name updates are not currently possible in jclouds.
        CloudDNSApi provideCloudDNSApi(ConvertToJcloudsCredentials credentials, final Provider provider) {
            Properties overrides = new Properties();
            // disable url caching
            overrides.setProperty(PROPERTY_SESSION_INTERVAL, "0");
            return ContextBuilder.newBuilder(new CloudDNSApiMetadata())
                                 .name(provider.name())
                                 .credentialsSupplier(credentials)
                                 .overrides(overrides)
                                 .modules(ImmutableSet.<com.google.inject.Module> builder()
                                                      .add(new SLF4JLoggingModule())
                                                      .add(new ExecutorServiceModule(sameThreadExecutor(),
                                                                                     sameThreadExecutor()))
                                                      .add(new com.google.inject.AbstractModule() {

                                                          @Override
                                                          protected void configure() {
                                                              bind(RecordsToPagedIterable.class)
                                                               .to(PaginationFixRecordsToPagedIterable.class);
                                                              bind(ProviderURISupplier.class).toInstance(new ProviderURISupplier() {

                                                                  @Override
                                                                  public URI get() {
                                                                      return URI.create(provider.url());
                                                                  }

                                                                  @Override
                                                                  public String toString() {
                                                                      return "DynamicURIFrom(" + provider + ")";
                                                                  }
                                                              });
                                                          }
                                                      })
                                                      .build())
                                 .buildApi(CloudDNSApi.class);
        }

        @Provides
        @Singleton
        Closeable provideCloseable(CloudDNSApi api) {
            return api;
        }
    }

    // fixed in jclouds 1.6.1
    static class PaginationFixRecordsToPagedIterable extends RecordsToPagedIterable {
        @Inject
        protected PaginationFixRecordsToPagedIterable(CloudDNSApi api) {
            super(api);
        }

        @Override
        protected List<Object> getArgs(GeneratedHttpRequest request) {
            return request.getCaller().get().getArgs();
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
            List<Object> creds = ListCredentials.asList(provider.get());
            return new org.jclouds.domain.Credentials(creds.get(0).toString(), creds.get(1).toString());
        }
    }
}
