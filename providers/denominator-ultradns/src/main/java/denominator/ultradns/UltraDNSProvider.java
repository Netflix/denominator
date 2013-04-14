package denominator.ultradns;

import static com.google.common.base.Suppliers.compose;

import java.io.Closeable;
import java.util.List;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.domain.Credentials;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.UltraDNSWSProviderMetadata;
import org.jclouds.ultradns.ws.domain.Account;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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

@Module(entryPoints = DNSApiManager.class, includes = GeoUnsupported.class)
public class UltraDNSProvider extends Provider {

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
        return ImmutableMultimap.<String, String> builder().putAll("password", "username", "password").build();
    }

    private static class ToJcloudsCredentials implements Function<List<Object>, Credentials> {
        public Credentials apply(List<Object> creds) {
            return new Credentials(creds.get(0).toString(), creds.get(1).toString());
        }
    }

    @Provides
    @Singleton
    UltraDNSWSApi provideApi(Supplier<Credentials> credentials) {
        return ContextBuilder.newBuilder(new UltraDNSWSProviderMetadata())
                             .credentialsSupplier(credentials)
                             .modules(ImmutableSet.<com.google.inject.Module> of(new SLF4JLoggingModule()))
                             .buildApi(UltraDNSWSApi.class);
    }

    @Provides
    @Singleton
    ZoneApi provideZoneApi(UltraDNSWSApi api, Supplier<Account> account) {
        return new UltraDNSZoneApi(api, account);
    }

    @Provides
    @Singleton
    Supplier<Account> account(final UltraDNSWSApi api) {
        return Suppliers.memoize(new Supplier<Account>() {

            @Override
            public Account get() {
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

    @Provides
    @Singleton
    Closeable provideCloser(UltraDNSWSApi api) {
        return api;
    }
}
