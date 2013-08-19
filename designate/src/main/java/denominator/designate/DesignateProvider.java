package denominator.designate;

import static dagger.Provides.Type.SET;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.GeoUnsupported;
import denominator.config.OnlyBasicResourceRecordSets;
import denominator.config.WeightedUnsupported;
import denominator.designate.DesignateDecoders.DomainListDecoder;
import denominator.designate.DesignateDecoders.RecordDecoder;
import denominator.designate.DesignateDecoders.RecordListDecoder;
import denominator.designate.DesignateEncoders.RecordEncoder;
import denominator.designate.KeystoneV2.TokenIdAndPublicURL;
import feign.Feign;
import feign.Feign.Defaults;
import feign.ReflectiveFeign;
import feign.Target.HardCodedTarget;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.gson.GsonModule;

public class DesignateProvider extends BasicProvider {
    private final String url;

    public DesignateProvider() {
        this(null);
    }

    /**
     * @param url
     *            if empty or null use default
     */
    public DesignateProvider(String url) {
        this.url = url == null || url.isEmpty() ? "http://localhost:5000/v2.0" : url;
    }

    @Override
    public String url() {
        return url;
    }

    // http://docs.hpcloud.com/api/dns/#create_record-jumplink-span
    @Override
    public Set<String> basicRecordTypes() {
        Set<String> types = new LinkedHashSet<String>();
        types.addAll(Arrays.asList("A", "AAAA", "CNAME", "MX", "NS", "SRV", "TXT"));
        return types;
    }

    @Override
    public Map<String, Collection<String>> profileToRecordTypes() {
        Map<String, Collection<String>> profileToRecordTypes = new LinkedHashMap<String, Collection<String>>();
        profileToRecordTypes.put("roundRobin", Arrays.asList("A", "AAAA", "MX", "NS", "SRV", "TXT"));
        return profileToRecordTypes;
    }

    @Override
    public boolean supportsDuplicateZoneNames() {
        return true;
    }

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
        Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
        options.put("password", Arrays.asList("tenantId", "username", "password"));
        return options;
    }

    @dagger.Module(injects = DNSApiManager.class, complete = false, overrides = true, includes = {
            GeoUnsupported.class, WeightedUnsupported.class, OnlyBasicResourceRecordSets.class, FeignModule.class })
    public static final class Module {

        @Provides
        CheckConnection checkConnection(LimitsReadable checkConnection) {
            return checkConnection;
        }

        @Provides
        @Singleton
        Closeable provideCloser(Feign feign) {
            return feign;
        }

        @Provides
        @Singleton
        ZoneApi provideZoneApi(DesignateZoneApi in) {
            return in;
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(DesignateResourceRecordSetApi.Factory in) {
            return in;
        }
    }

    // unbound wildcards are not currently injectable in dagger.
    @SuppressWarnings("rawtypes")
    @dagger.Module(injects = DesignateResourceRecordSetApi.Factory.class, complete = false, includes = {
            Defaults.class, ReflectiveFeign.Module.class, GsonModule.class })
    public static final class FeignModule {

        @Provides
        @Singleton
        Designate cloudDNS(Feign feign, DesignateTarget target) {
            return feign.newInstance(target);
        }

        // override binding to use whatever your service type is
        @Provides
        @Named("serviceTypeSuffix")
        String serviceTypeSuffix() {
            return ":dns";
        }

        @Provides
        @Singleton
        KeystoneV2 cloudIdentity(Feign feign) {
            return feign.newInstance(new HardCodedTarget<KeystoneV2>(KeystoneV2.class, "keystone", "http://invalid"));
        }

        @Provides(type = SET)
        Decoder tokenIdAndPublicURLDecoder(KeystoneV2AccessDecoder decoder) {
            return decoder;
        }

        @Provides(type = SET)
        Decoder domainListDecoder(DomainListDecoder decoder) {
            return decoder;
        }

        @Provides(type = SET)
        Decoder recordListDecoder(RecordListDecoder decoder) {
            return decoder;
        }

        @Provides(type = SET)
        Decoder recordDecoder(RecordDecoder decoder) {
            return decoder;
        }

        @Provides(type = SET)
        Encoder recordEncoder(RecordEncoder encoder) {
            return encoder;
        }

        @Provides
        public TokenIdAndPublicURL urlAndToken(InvalidatableAuthProvider supplier) {
            return supplier.get();
        }
    }
}
