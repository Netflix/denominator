package denominator.route53;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static org.jclouds.Constants.PROPERTY_SESSION_INTERVAL;

import java.io.Closeable;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.aws.domain.SessionCredentials;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.location.suppliers.ProviderURISupplier;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.route53.Route53Api;
import org.jclouds.route53.Route53ApiMetadata;

import com.google.common.base.Supplier;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;

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
import denominator.config.GeoUnsupported;
import denominator.profile.WeightedResourceRecordSetApi;

public class Route53Provider extends BasicProvider {
    private final String url;

    public Route53Provider() {
        this(null);
    }

    /**
     * @param url
     *            if empty or null use default
     */
    public Route53Provider(String url) {
        url = emptyToNull(url);
        this.url = url != null ? url : new Route53ApiMetadata().getDefaultEndpoint().get();
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
        return ImmutableMultimap.<String, String> builder()
                .putAll("accessKey", "accessKey", "secretKey")
                .putAll("session", "accessKey", "secretKey", "sessionToken").build();
    }

    @dagger.Module(injects = DNSApiManager.class,
                   complete = false, // denominator.Provider
                   includes = { GeoUnsupported.class,
                                ConcatBasicAndQualifiedResourceRecordSets.class,
                                InstanceProfileCredentialsProvider.class })
    public static final class Module {

        @Provides
        @Singleton
        // Dynamic name updates are not currently possible in jclouds.
        Route53Api provideApi(ConvertToJcloudsCredentials credentials, final Provider provider) {
            Properties overrides = new Properties();
            // disable url caching
            overrides.setProperty(PROPERTY_SESSION_INTERVAL, "0");
            return ContextBuilder.newBuilder(new Route53ApiMetadata())
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
                                 .buildApi(Route53Api.class);
        }

        @Provides
        @Singleton
        ZoneApi provideZoneApi(Route53Api api) {
            return new Route53ZoneApi(api);
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(Route53Api api) {
            return new Route53ResourceRecordSetApi.Factory(api);
        }

        @Provides
        @Singleton
        Closeable provideCloser(Route53Api api) {
            return api;
        }

        @Provides
        WeightedResourceRecordSetApi.Factory provideWeightedResourceRecordSetApiFactory(
                Route53WeightedResourceRecordSetApi.Factory in) {
            return in;
        }

        @Provides(type = Provides.Type.SET)
        QualifiedResourceRecordSetApi.Factory provideWeightedResourceRecordSetApiFactory(
                WeightedResourceRecordSetApi.Factory in) {
            return in;
        }

        /**
         * @see <a
         *      href="http://docs.aws.amazon.com/Route53/latest/APIReference/API_ChangeResourceRecordSets.html">valid
         *      types</a>
         */
        @Provides
        @Singleton
        @denominator.config.profile.Weighted
        Set<String> provideSupportedWeightedRecordTypes() {
            return ImmutableSet.of("A", "AAAA", "CNAME", "MX", "PTR", "SPF", "SRV", "TXT");
        }

        /**
         * @see <a
         *      href="http://docs.aws.amazon.com/Route53/latest/APIReference/API_ChangeResourceRecordSets.html">valid
         *      weights</a>
         */
        @Provides
        @Singleton
        @denominator.config.profile.Weighted
        SortedSet<Integer> provideSupportedWeights() {
            return ContiguousSet.create(Range.closed(0, 255), DiscreteDomain.integers());
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
            if (creds.size() == 2)
                return new org.jclouds.domain.Credentials(creds.get(0).toString(), creds.get(1).toString());
            return SessionCredentials.builder()
                                     .accessKeyId(creds.get(0).toString())
                                     .secretAccessKey(creds.get(1).toString())
                                     .sessionToken(creds.get(2).toString())
                                     .build();
        }
    }
}
