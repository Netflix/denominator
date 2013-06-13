package denominator.ultradns;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static dagger.Provides.Type.SET;
import static org.jclouds.Constants.PROPERTY_SESSION_INTERVAL;

import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.http.HttpRequest;
import org.jclouds.location.suppliers.ProviderURISupplier;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.UltraDNSWSProviderMetadata;
import org.jclouds.ultradns.ws.binders.DirectionalRecordAndGeoGroupToXML;
import org.jclouds.ultradns.ws.domain.IdAndName;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.QualifiedResourceRecordSetApi;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.ConcatBasicAndQualifiedResourceRecordSets;
import denominator.config.WeightedUnsupported;
import denominator.profile.GeoResourceRecordSetApi;

public class UltraDNSProvider extends BasicProvider {
    private final String url;

    public UltraDNSProvider() {
        this(null);
    }

    /**
     * @param url
     *            if empty or null use default
     */
    public UltraDNSProvider(String url) {
        url = emptyToNull(url);
        this.url = url != null ? url : new UltraDNSWSProviderMetadata().getEndpoint();
    }

    @Override
    public String url() {
        return url;
    }

    /**
     * harvested from the {@code RESOURCE RECORD TYPE CODES} section of the SOAP
     * user guide, dated 2012-11-04.
     */
    @Override
    public Set<String> basicRecordTypes() {
        return ImmutableSet.of("A", "AAAA", "CNAME", "HINFO", "MX", "NAPTR", "NS", "PTR", "RP", "SOA", "SPF", "SRV",
                "TXT");
    }

    /**
     * directional pools in ultra have types {@code IPV4} and {@code IPV6} which
     * accept both CNAME and address types.
     */
    @Override
    public SetMultimap<String, String> profileToRecordTypes() {
        return ImmutableSetMultimap.<String, String> builder()
                .putAll("geo", "A", "AAAA", "CNAME", "HINFO", "MX", "NAPTR", "PTR", "RP", "SRV", "TXT")
                .putAll("roundRobin", filter(basicRecordTypes(), not(in(ImmutableSet.of("SOA", "CNAME"))))).build();
    }

    @Override
    public Multimap<String, String> credentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder().putAll("password", "username", "password").build();
    }

    @dagger.Module(injects = DNSApiManager.class,
                   complete = false, // denominator.Provider and denominator.Credentials
                   includes = { UltraDNSGeoSupport.class,
                                WeightedUnsupported.class,
                                ConcatBasicAndQualifiedResourceRecordSets.class })
    public static final class Module {

        @Provides
        @Singleton
        // Dynamic name updates are not currently possible in jclouds.
        UltraDNSWSApi provideApi(ConvertToJcloudsCredentials credentials, final Provider provider) {
            Properties overrides = new Properties();
            // disable url caching
            overrides.setProperty(PROPERTY_SESSION_INTERVAL, "0");
            return ContextBuilder.newBuilder(new UltraDNSWSProviderMetadata())
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
                                                              bind(DirectionalRecordAndGeoGroupToXML.class)
                                                               .to(ForceOverlapTransferDirectionalRecordAndGeoGroupToXML.class);
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
                                 .buildApi(UltraDNSWSApi.class);
        }

        // JCLOUDS-110
        private static class ForceOverlapTransferDirectionalRecordAndGeoGroupToXML extends
                DirectionalRecordAndGeoGroupToXML {
            @SuppressWarnings("unchecked")
            @Override
            public <R extends HttpRequest> R bindToRequest(R request, Map<String, Object> postParams) {
                request = super.bindToRequest(request, postParams);
                String patched = String.class
                        .cast(request.getPayload().getRawContent())
                        .replace("</GeolocationGroupDetails></UpdateDirectionalRecordData>",
                                 "</GeolocationGroupDetails><forceOverlapTransfer>true</forceOverlapTransfer></UpdateDirectionalRecordData>");
                return (R) request.toBuilder().payload(patched).build();
            }
        }

        @Provides
        @Singleton
        ZoneApi provideZoneApi(UltraDNSWSApi api, Supplier<IdAndName> account) {
            return new UltraDNSZoneApi(api, account);
        }

        @Provides
        @Singleton
        Supplier<IdAndName> account(final UltraDNSWSApi api) {
            return Suppliers.memoize(new Supplier<IdAndName>() {
    
                @Override
                public IdAndName get() {
                    return api.getCurrentAccount();
                }
    
                @Override
                public String toString() {
                    return "accountOf(" + api + ")";
                }
            });
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(UltraDNSWSApi api) {
            return new UltraDNSResourceRecordSetApi.Factory(api);
        }

        @Provides(type = SET)
        @Singleton
        QualifiedResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory(GeoResourceRecordSetApi.Factory in) {
            return in;
        }

        @Provides
        @Singleton
        Closeable provideCloser(UltraDNSWSApi api) {
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
            List<Object> creds = ListCredentials.asList(provider.get());
            return new org.jclouds.domain.Credentials(creds.get(0).toString(), creds.get(1).toString());
        }
    }
}
