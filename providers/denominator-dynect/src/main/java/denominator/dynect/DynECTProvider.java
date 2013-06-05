package denominator.dynect;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static org.jclouds.Constants.PROPERTY_SESSION_INTERVAL;
import static org.jclouds.rest.config.BinderUtils.bindHttpApi;

import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.jclouds.ContextBuilder;
import org.jclouds.Fallbacks.EmptyFluentIterableOnNotFoundOr404;
import org.jclouds.Fallbacks.EmptyMultimapOnNotFoundOr404;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.dynect.v3.DynECTApi;
import org.jclouds.dynect.v3.DynECTExceptions.JobStillRunningException;
import org.jclouds.dynect.v3.DynECTProviderMetadata;
import org.jclouds.dynect.v3.domain.GeoService;
import org.jclouds.dynect.v3.domain.Record;
import org.jclouds.dynect.v3.filters.AlwaysAddContentType;
import org.jclouds.dynect.v3.filters.SessionManager;
import org.jclouds.location.suppliers.ProviderURISupplier;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.rest.annotations.Fallback;
import org.jclouds.rest.annotations.Headers;
import org.jclouds.rest.annotations.QueryParams;
import org.jclouds.rest.annotations.RequestFilters;
import org.jclouds.rest.annotations.SelectJson;

import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.inject.Injector;

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
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(DynECTApi api, ReadOnlyApi roApi) {
            return new DynECTResourceRecordSetApi.Factory(api, roApi);
        }

        @Provides
        @Singleton
        // Dynamic name updates are not currently possible in jclouds.
        Injector provideInjector(ConvertToJcloudsCredentials credentials, final Provider provider) {
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
                                                              bindHttpApi(binder(), ReadOnlyApi.class);
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
                                 .buildInjector();
        }

        @Provides
        @Singleton
        DynECTApi provideNormalApi(Injector injector) {
           return injector.getInstance(DynECTApi.class);
        }

        @Provides
        @Singleton
        ReadOnlyApi provideReadOnlyApi(Injector injector) {
           return injector.getInstance(ReadOnlyApi.class);
        }

        @Provides
        @Singleton
        Closeable provideCloseable(DynECTApi api) {
            return api;
        }
    }

    /**
     * Temporary api to reduce network calls
     */
    @Headers(keys = "API-Version", values = "3.5.0")
    @RequestFilters({ AlwaysAddContentType.class, SessionManager.class })
    static interface ReadOnlyApi extends Closeable {
        @GET
        @Path("/Geo")
        @SelectJson("data")
        @QueryParams(keys = "detail", values = "Y")
        FluentIterable<GeoService> geos() throws JobStillRunningException;

        @GET
        @Path("/AllRecord/{zone}")
        @QueryParams(keys = "detail", values = "Y")
        @SelectJson("data")
        @Fallback(EmptyMultimapOnNotFoundOr404.class)
        Multimap<String, Record<? extends Map<String, Object>>> recordsInZone(@PathParam("zone") String zone)
                throws JobStillRunningException;

        @GET
        @Path("/AllRecord/{zone}/{fqdn}")
        @QueryParams(keys = "detail", values = "Y")
        @SelectJson("data")
        @Fallback(EmptyMultimapOnNotFoundOr404.class)
        Multimap<String, Record<? extends Map<String, Object>>> recordsInZoneByName(@PathParam("zone") String zone,
                @PathParam("fqdn") String fqdn) throws JobStillRunningException;

        @GET
        @Path("/{type}Record/{zone}/{fqdn}")
        @QueryParams(keys = "detail", values = "Y")
        @SelectJson("data")
        @Fallback(EmptyFluentIterableOnNotFoundOr404.class)
        FluentIterable<Record<? extends Map<String, Object>>> recordsInZoneByNameAndType(
                @PathParam("zone") String zone, @PathParam("fqdn") String fqdn, @PathParam("type") String type)
                throws JobStillRunningException;
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
            return new org.jclouds.domain.Credentials(creds.get(0) + ":" + creds.get(1), creds.get(2).toString());
        }
    }
}
