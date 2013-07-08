package denominator.designate;

import static com.google.common.base.Strings.emptyToNull;

import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyBasicResourceRecordSets;
import denominator.config.WeightedUnsupported;
import denominator.designate.OpenStackApis.Designate;
import denominator.designate.OpenStackApis.KeystoneV2;
import denominator.designate.OpenStackApis.TokenIdAndPublicURL;
import denominator.designate.OpenStackDecoders.DomainListDecoder;
import denominator.designate.OpenStackDecoders.RecordDecoder;
import denominator.designate.OpenStackDecoders.RecordListDecoder;
import denominator.designate.OpenStackEncoders.RecordEncoder;
import feign.Feign;
import feign.Feign.Defaults;
import feign.ReflectiveFeign;
import feign.Target.HardCodedTarget;
import feign.codec.BodyEncoder;
import feign.codec.Decoder;

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
        url = emptyToNull(url);
        this.url = url != null ? url : "http://localhost:5000/v2.0";
    }

    @Override
    public String url() {
        return url;
    }

    // http://docs.hpcloud.com/api/dns/#create_record-jumplink-span
    @Override
    public Set<String> basicRecordTypes() {
        return ImmutableSet.of("A", "AAAA", "CNAME", "MX", "NS", "SRV", "TXT");
    }

    @Override
    public SetMultimap<String, String> profileToRecordTypes() {
        return ImmutableSetMultimap.<String, String> builder()
                .putAll("roundRobin", ImmutableSet.of("A", "AAAA", "MX", "NS", "SRV", "TXT")).build();
    }

    @Override
    public boolean supportsDuplicateZoneNames() {
        return true;
    }

    @Override
    public Multimap<String, String> credentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder()//
//                .putAll("password", "username", "password")
                .putAll("password", "tenantId", "username", "password")
                .build();
    }

    @dagger.Module(injects = DNSApiManager.class, complete = false, overrides = true, //
    includes = { NothingToClose.class,//
            GeoUnsupported.class,//
            WeightedUnsupported.class, //
            OnlyBasicResourceRecordSets.class,//
            FeignModule.class })
    public static final class Module {

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

    @dagger.Module(injects = DesignateResourceRecordSetApi.Factory.class, complete = false, overrides = true, includes = {
            Defaults.class, ReflectiveFeign.Module.class })
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

        @Provides
        @Singleton
        Map<String, Decoder> decoders(KeystoneV2AccessDecoder keyStone) {
            ImmutableMap.Builder<String, Decoder> decoders = ImmutableMap.<String, Decoder> builder();
            decoders.put("KeystoneV2", keyStone);
            Decoder domainListDecoder = new DomainListDecoder();
            decoders.put("Designate#domains()", domainListDecoder);
            Decoder recordListDecoder = new RecordListDecoder();
            decoders.put("Designate#records(String)", recordListDecoder);
            Decoder recordDecoder = new RecordDecoder();
            decoders.put("Designate#createRecord(String,Record)", recordDecoder);
            decoders.put("Designate#updateRecord(String,Record)", recordDecoder);
            return decoders.build();
        }

        @Provides
        @Singleton
        Map<String, BodyEncoder> bodyEncoders() {
            ImmutableMap.Builder<String, BodyEncoder> bodyEncoders = ImmutableMap.<String, BodyEncoder> builder();
            BodyEncoder recordEncoder = new RecordEncoder();
            bodyEncoders.put("Designate#createRecord(String,Record)", recordEncoder);
            bodyEncoders.put("Designate#updateRecord(String,Record)", recordEncoder);
            return bodyEncoders.build();
        }

        @Provides
        public TokenIdAndPublicURL urlAndToken(InvalidatableAuthSupplier supplier) {
            return supplier.get();
        }
    }
}
