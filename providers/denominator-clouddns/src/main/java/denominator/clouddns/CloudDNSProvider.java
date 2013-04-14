package denominator.clouddns;

import static com.google.common.base.Suppliers.compose;

import java.io.Closeable;
import java.util.List;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApi;
import org.jclouds.rackspace.clouddns.v1.CloudDNSApiMetadata;

import com.google.common.base.Function;
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
public class CloudDNSProvider extends Provider {

    @Provides
    protected Provider provideThis() {
        return this;
    }

    @Provides
    @Singleton
    Supplier<Credentials> toJcloudsCredentials(CredentialsAsList supplier) {
        return compose(new ToJcloudsCredentials(), supplier);
    }

    @Override
    public Multimap<String, String> getCredentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder().putAll("apiKey", "username", "apiKey").build();
    }

    private static class ToJcloudsCredentials implements Function<List<Object>, Credentials> {
        public Credentials apply(List<Object> creds) {
            return new Credentials(creds.get(0).toString(), creds.get(1).toString());
        }
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
    CloudDNSApi provideCloudDNSApi(Supplier<Credentials> credentials) {
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
