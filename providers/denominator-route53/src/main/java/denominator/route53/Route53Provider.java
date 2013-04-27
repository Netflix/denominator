package denominator.route53;

import static com.google.common.base.Suppliers.compose;

import java.io.Closeable;
import java.util.List;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.aws.domain.SessionCredentials;
import org.jclouds.aws.route53.AWSRoute53ProviderMetadata;
import org.jclouds.domain.Credentials;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.route53.Route53Api;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import dagger.Module;
import dagger.Provides;
import denominator.CredentialsConfiguration.CredentialsAsList;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.GeoUnsupported;
import denominator.config.OnlyNormalResourceRecordSets;

@Module(entryPoints = DNSApiManager.class,
           includes = { GeoUnsupported.class, 
                        OnlyNormalResourceRecordSets.class } )
public class Route53Provider extends Provider {

    @Provides
    protected Provider provideThis() {
        return this;
    }

    @Override
    public Optional<Supplier<denominator.Credentials>> defaultCredentialSupplier() {
        return Optional.<Supplier<denominator.Credentials>> of(new InstanceProfileCredentialsSupplier());
    }

    @Provides
    @Singleton
    Supplier<Credentials> toJcloudsCredentials(CredentialsAsList supplier) {
        return compose(new ToJcloudsCredentials(), supplier);
    }

    @Override
    public Multimap<String, String> getCredentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder()
                .putAll("accessKey", "accessKey", "secretKey")
                .putAll("session", "accessKey", "secretKey", "sessionToken").build();
    }

    private static class ToJcloudsCredentials implements Function<List<Object>, Credentials> {
        public Credentials apply(List<Object> creds) {
            if (creds.size() == 2)
                return new Credentials(creds.get(0).toString(), creds.get(1).toString());
            return SessionCredentials.builder()
                                     .accessKeyId(creds.get(0).toString())
                                     .secretAccessKey(creds.get(1).toString())
                                     .sessionToken(creds.get(2).toString())
                                     .build();
        }
    }

    @Provides
    @Singleton
    Route53Api provideApi(Supplier<Credentials> credentials) {
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
