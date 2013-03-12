package denominator.dynect;

import static com.google.common.base.Suppliers.compose;

import java.io.Closeable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.jclouds.ContextBuilder;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.domain.Credentials;
import org.jclouds.dynect.v3.DynECTApiMetadata;
import org.jclouds.dynect.v3.DynECTAsyncApi;
import org.jclouds.dynect.v3.DynECTProviderMetadata;
import org.jclouds.dynect.v3.config.DynECTParserModule;
import org.jclouds.dynect.v3.config.DynECTRestClientModule;
import org.jclouds.dynect.v3.domain.SessionCredentials;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.rest.RestContext;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.primitives.UnsignedInteger;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import dagger.Module;
import dagger.Provides;
import denominator.CredentialsConfiguration.CredentialsAsList;
import denominator.DNSApiManager;
import denominator.Provider;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;

@Module(entryPoints = DNSApiManager.class)
public class DynECTProvider extends Provider {

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
        return ImmutableMultimap.<String, String> builder()
                .putAll("password", "customer", "username", "password").build();
    }

    private static class ToJcloudsCredentials implements Function<List<Object>, Credentials> {
        public Credentials apply(List<Object> creds) {
            return new Credentials(creds.get(0) + ":" + creds.get(1), creds.get(2).toString());
        }
    }

    @Provides
    @Singleton
    ZoneApi provideZoneApi(RestContext<org.jclouds.dynect.v3.DynECTApi, DynECTAsyncApi> context) {
        return new DynECTZoneApi(context.getApi());
    }

    @Provides
    @Singleton
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
            RestContext<org.jclouds.dynect.v3.DynECTApi, DynECTAsyncApi> context) {
        return new DynECTResourceRecordSetApi.Factory(context.getApi());
    }

    @Provides
    @Singleton
    RestContext<org.jclouds.dynect.v3.DynECTApi, DynECTAsyncApi> provideContext(Supplier<Credentials> credentials) {
        return ContextBuilder.newBuilder(provider)
                             .credentialsSupplier(credentials)
                             .modules(ImmutableSet.<com.google.inject.Module> of(new SLF4JLoggingModule())).build();
    }

    static ApiMetadata api = new DynECTApiMetadata().toBuilder()
            .defaultModules(ImmutableSet.<Class<? extends com.google.inject.Module>>of(PatchedDynECTParserModule.class, DynECTRestClientModule.class))
            .build();
    static ProviderMetadata provider = new DynECTProviderMetadata().toBuilder().apiMetadata(api).build();

    public static class PatchedDynECTParserModule extends DynECTParserModule {

        @Override
        protected void configure() {
        }

        @Override
        public Map<Type, Object> provideCustomAdapterBindings() {
            return new ImmutableMap.Builder<Type, Object>()
                    .put(SessionCredentials.class, super.provideCustomAdapterBindings().get(SessionCredentials.class))
                    .put(UnsignedInteger.class, new UnsignedIntegerAdapter()).build();
        }

        // patched as in 1.6.0-rc.1, serialize was not implemented.
        private static class UnsignedIntegerAdapter implements JsonSerializer<UnsignedInteger>,
                JsonDeserializer<UnsignedInteger> {
            public JsonElement serialize(UnsignedInteger src, Type typeOfSrc, JsonSerializationContext context) {
                return new JsonPrimitive(src);
            }

            public UnsignedInteger deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context)
                    throws JsonParseException {
                return UnsignedInteger.fromIntBits(jsonElement.getAsBigInteger().intValue());
            }
        }
    }

    @Provides
    @Singleton
    Closeable provideCloser(RestContext<org.jclouds.dynect.v3.DynECTApi, DynECTAsyncApi> context) {
        return context;
    }
}
