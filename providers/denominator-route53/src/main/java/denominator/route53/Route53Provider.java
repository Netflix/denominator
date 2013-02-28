package denominator.route53;

import static com.google.common.base.Suppliers.compose;

import java.io.Closeable;
import java.util.List;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.aws.domain.SessionCredentials;
import org.jclouds.aws.route53.AWSRoute53ProviderMetadata;
import org.jclouds.domain.Credentials;
import org.jclouds.rest.RestContext;
import org.jclouds.route53.Route53AsyncApi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import dagger.Module;
import dagger.Provides;
import denominator.CredentialsConfiguration.CredentialsAsList;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;

@Module(entryPoints = DNSApiManager.class)
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
    RestContext<org.jclouds.route53.Route53Api, Route53AsyncApi> provideContext(Supplier<Credentials> credentials) {
        return ContextBuilder.newBuilder(new AWSRoute53ProviderMetadata()).credentialsSupplier(credentials).build();
    }

    @Provides
    @Singleton
    ZoneApi provideZoneApi(RestContext<org.jclouds.route53.Route53Api, Route53AsyncApi> context) {
        return new Route53ZoneApi(context.getApi());
    }

    @Provides
    @Singleton
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
            RestContext<org.jclouds.route53.Route53Api, Route53AsyncApi> context) {
        return new Route53ResourceRecordSetApi.Factory(context.getApi());
    }

    @Provides
    @Singleton
    Closeable provideCloser(RestContext<org.jclouds.route53.Route53Api, Route53AsyncApi> context) {
        return context;
    }
}
