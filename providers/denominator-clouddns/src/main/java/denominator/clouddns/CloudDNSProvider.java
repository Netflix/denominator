package denominator.clouddns;

import static com.google.common.base.Strings.emptyToNull;

import java.util.Map;
import java.util.Set;

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
import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.clouddns.RackspaceApis.CloudIdentity;
import denominator.clouddns.RackspaceApis.TokenIdAndPublicURL;
import denominator.clouddns.RackspaceDecoders.DomainListDecoder;
import denominator.clouddns.RackspaceDecoders.RecordListDecoder;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyBasicResourceRecordSets;
import denominator.config.WeightedUnsupported;
import feign.Feign;
import feign.Feign.Defaults;
import feign.ReflectiveFeign;
import feign.Target.HardCodedTarget;
import feign.codec.Decoder;

public class CloudDNSProvider extends BasicProvider {
    private final String url;

    public CloudDNSProvider() {
        this(null);
    }

    /**
     * @param url
     *            if empty or null use default
     */
    public CloudDNSProvider(String url) {
        url = emptyToNull(url);
        this.url = url != null ? url : "https://identity.api.rackspacecloud.com/v2.0";
    }

    @Override
    public String url() {
        return url;
    }

    // TODO: verify when write support is added
    // http://docs.rackspace.com/cdns/api/v1.0/cdns-devguide/content/supported_record_types.htm
    @Override
    public Set<String> basicRecordTypes() {
        return ImmutableSet.of("A", "AAAA", "CNAME", "MX", "NS", "PTR", "SRV", "TXT");
    }

    // TODO: verify when write support is added
    @Override
    public SetMultimap<String, String> profileToRecordTypes() {
        return ImmutableSetMultimap.<String, String> builder().putAll("roundRobin", basicRecordTypes()).build();
    }

    @Override
    public boolean supportsDuplicateZoneNames() {
        return true;
    }

    @Override
    public Multimap<String, String> credentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder()
                                .putAll("password", "username", "password")
                                .putAll("apiKey", "username", "apiKey").build();
    }

    @dagger.Module(injects = DNSApiManager.class, complete = false, overrides = true, //
    includes = { NothingToClose.class,//
            GeoUnsupported.class,//
            WeightedUnsupported.class, //
            OnlyBasicResourceRecordSets.class,//
            Defaults.class,//
            ReflectiveFeign.Module.class })
    public static final class Module {

        @Provides
        @Singleton
        ZoneApi provideZoneApi(CloudDNS api) {
            return new CloudDNSZoneApi(api);
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(CloudDNS api) {
            return new CloudDNSResourceRecordSetApi.Factory(api);
        }

        @Provides
        @Singleton
        CloudDNS cloudDNS(Feign feign, CloudDNSTarget target) {
            return feign.newInstance(target);
        }

        @Provides
        @Singleton
        CloudIdentity cloudIdentity(Feign feign) {
            return feign.newInstance(new HardCodedTarget<CloudIdentity>(CloudIdentity.class, "cloudidentity",
                    "http://invalid"));
        }

        @Provides
        @Singleton
        Map<String, Decoder> decoders() {
            ImmutableMap.Builder<String, Decoder> decoders = ImmutableMap.<String, Decoder> builder();
            decoders.put("CloudIdentity", new KeystoneAccessDecoder("rax:dns"));
            Decoder domainListDecoder = new DomainListDecoder();
            decoders.put("CloudDNS#domains(URI)", domainListDecoder);
            decoders.put("CloudDNS#domains()", domainListDecoder);
            Decoder recordListDecoder = new RecordListDecoder();
            decoders.put("CloudDNS#records(URI)", recordListDecoder);
            decoders.put("CloudDNS#records(int)", recordListDecoder);
            decoders.put("CloudDNS#recordsByNameAndType(int,String,String)", recordListDecoder);
            return decoders.build();
        }

        @Provides
        public TokenIdAndPublicURL urlAndToken(InvalidatableAuthSupplier supplier) {
            return supplier.get();
        }
    }
}
