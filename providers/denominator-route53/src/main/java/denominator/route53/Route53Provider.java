package denominator.route53;

import java.io.Closeable;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.aws.domain.SessionCredentials;
import org.jclouds.aws.route53.AWSRoute53ProviderMetadata;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.route53.Route53Api;

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

public class Route53Provider extends BasicProvider {

    @Override
    public Multimap<String, String> getCredentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder()
                .putAll("accessKey", "accessKey", "secretKey")
                .putAll("session", "accessKey", "secretKey", "sessionToken").build();
    }

    @dagger.Module(injects = DNSApiManager.class,
                   includes = { GeoUnsupported.class, 
                                OnlyNormalResourceRecordSets.class,
                                InstanceProfileCredentialsProvider.class })
    public static final class Module {

        @Provides
        public Provider provider() {
            return new Route53Provider();
        }

        @Provides
        @Singleton
        Route53Api provideApi(ConvertToJcloudsCredentials credentials) {
            return ContextBuilder.newBuilder(new AWSRoute53ProviderMetadata())
                                 .credentialsSupplier(credentials)
                                 .modules(ImmutableSet.<com.google.inject.Module> of(new SLF4JLoggingModule()))
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
