package denominator.clouddns;

import java.io.Closeable;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApiMetadata;

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
import denominator.config.OnlyNormalResourceRecordSets;
public class CloudDNSProvider extends BasicProvider {

    @Override
    public Multimap<String, String> getCredentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder().putAll("apiKey", "username", "apiKey").build();
    }

    @dagger.Module(injects = DNSApiManager.class,
                   complete = false, // no built-in credentials provider
                   includes = { GeoUnsupported.class, 
                                OnlyNormalResourceRecordSets.class } )
    public static final class Module {

        @Provides
        public Provider provider() {
            return new CloudDNSProvider();
        }

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
        CloudDNSApi provideCloudDNSApi(ConvertToJcloudsCredentials credentials) {
            return ContextBuilder.newBuilder(new CloudDNSApiMetadata())
                    .credentialsSupplier(credentials)
                    .modules(ImmutableSet.<com.google.inject.Module> of(new SLF4JLoggingModule()))
                    .buildApi(CloudDNSApi.class);
        }

        @Provides
        @Singleton
        Closeable provideCloseable(CloudDNSApi api) {
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
            return new org.jclouds.domain.Credentials(creds.get(0).toString(), creds.get(1).toString());
        }
    }
}
