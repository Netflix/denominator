package denominator.verisignmdns;

import denominator.BasicProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Singleton;

import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.WeightedUnsupported;

import denominator.verisignmdns.VrsnMdnsContentHandler.RecordListHandler;
import denominator.verisignmdns.VrsnMdnsContentHandler.ZoneListHandler;
import denominator.verisignmdns.VrsnMdnsErrorDecoder.VrsnMdnsError;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.sax.SAXDecoder;

public class VerisignMDNSProvider extends BasicProvider {
    private final String url;

    public VerisignMDNSProvider() {
        this(null);
    }

    /**
     * @param url
     *            if empty or null use default
     */
    public VerisignMDNSProvider(String url) {
        this.url = url == null || url.isEmpty() ? "https://api.dns-tool.com/dnsa-ws/V2.0/dnsaapi" : url;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
        Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
        options.put("password", Arrays.asList("username", "password"));
        return options;
    }

    @Override
    public Map<String, Collection<String>> profileToRecordTypes() {
        Map<String, Collection<String>> profileToRecordTypes = new LinkedHashMap<String, Collection<String>>();
        return profileToRecordTypes;
    }

    @dagger.Module(injects = DNSApiManager.class, complete = false, // denominator.Provider
    includes = { NothingToClose.class, GeoUnsupported.class, WeightedUnsupported.class, FeignModule.class })
    public static final class Module {

        @Provides
        CheckConnection alwaysOK() {
            return new CheckConnection() {
                public boolean ok() {
                    return true;
                }
            };
        }

        @Provides
        @Singleton
        ZoneApi provideZoneApi(VerisignMDNSZoneApi in) {
            return in;
        }

        @Provides
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(VerisignMDNSResourceRecordSetApi.Factory in) {
            return in;
        }

        @Provides
        AllProfileResourceRecordSetApi.Factory provideAllProfileResourceRecordSetApiFactory(
                VerisignMDNSAllProfileResourceRecordSetApi.Factory in) {
            return in;
        }
    }

    @dagger.Module(//
    injects = { VerisignMDNSResourceRecordSetApi.Factory.class }, complete = false, overrides = true, includes = {
            Feign.Defaults.class, XMLCodec.class })
    public static final class FeignModule {

        @Singleton
        @Provides
        VrsnMdns vrsnMdns(Feign feign, VrsnMdnsTarget target) {
            return feign.newInstance(target);
        }
    }

    @dagger.Module(//
    injects = { Encoder.class, Decoder.class, ErrorDecoder.class },//
    overrides = true, // ErrorDecoder
    complete = false, addsTo = Feign.Defaults.class//
    )
    static final class XMLCodec {

        @Provides
        Encoder formEncoder() {
            return new VrsnMdnsFormEncoder();
        }

        @Provides
        Decoder saxDecoder() {
            return SAXDecoder.builder()//
                    .registerContentHandler(ZoneListHandler.class)//
                    .registerContentHandler(RecordListHandler.class)//
                    .registerContentHandler(VrsnMdnsError.class).build();
        }

        @Provides
        ErrorDecoder errorDecoders(VrsnMdnsErrorDecoder errorDecoder) {
            return errorDecoder;
        }
    }
}