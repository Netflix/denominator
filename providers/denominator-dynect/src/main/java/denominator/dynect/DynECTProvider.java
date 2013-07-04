package denominator.dynect;

import static denominator.dynect.DynECTDecoder.login;
import static denominator.dynect.DynECTDecoder.parseDataWith;
import static denominator.dynect.DynECTDecoder.recordIds;
import static denominator.dynect.DynECTDecoder.records;
import static denominator.dynect.DynECTDecoder.resourceRecordSets;
import static denominator.dynect.DynECTDecoder.zones;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.gson.Gson;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.DNSApiManager;
import denominator.QualifiedResourceRecordSetApi.Factory;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.ConcatBasicAndQualifiedResourceRecordSets;
import denominator.config.NothingToClose;
import denominator.config.WeightedUnsupported;
import denominator.dynect.InvalidatableTokenProvider.Session;
import denominator.profile.GeoResourceRecordSetApi;
import feign.Feign;
import feign.Feign.Defaults;
import feign.ReflectiveFeign;
import feign.RequestTemplate;
import feign.codec.Decoder;
import feign.codec.FormEncoder;

public class DynECTProvider extends BasicProvider {
    private final String url;

    public DynECTProvider() {
        this(null);
    }

    /**
     * @param url
     *            if empty or null use default
     */
    public DynECTProvider(String url) {
        this.url = url == null || url.isEmpty() ? "https://api2.dynect.net/REST" : url;
    }

    @Override
    public String url() {
        return url;
    }

    // https://manage.dynect.net/help/docs/api2/rest/resources/index.html
    @Override
    public Set<String> basicRecordTypes() {
        Set<String> types = new LinkedHashSet<String>();
        types.addAll(Arrays.asList("A", "AAAA", "CERT", "CNAME", "DHCID", "DNAME", "DNSKEY", "DS", "IPSECKEY", "KEY", "KX",
                "LOC", "MX", "NAPTR", "NS", "NSAP", "PTR", "PX", "RP", "SOA", "SPF", "SRV", "SSHFP", "TXT"));
        return types;
    }

    // https://manage.dynect.net/help/docs/api2/rest/resources/Geo.html
    @Override
    public Map<String, Collection<String>> profileToRecordTypes() {
        Map<String, Collection<String>> profileToRecordTypes = new LinkedHashMap<String, Collection<String>>();
        profileToRecordTypes.put("geo", Arrays.asList("A", "AAAAA", "CNAME", "CERT", "MX", "TXT", "SPF", "PTR", "LOC", "SRV", "RP", "KEY",
                        "DNSKEY", "SSHFP", "DHCID", "NSAP", "PX"));
        profileToRecordTypes.put("roundRobin", Arrays.asList("A", "AAAA", "CERT", "DHCID", "DNAME", "DNSKEY", "DS", "IPSECKEY", "KEY", "KX",
                "LOC", "MX", "NAPTR", "NS", "NSAP", "PTR", "PX", "RP", "SPF", "SRV", "SSHFP", "TXT"));
        return profileToRecordTypes;
    }

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
        Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
        options.put("password", Arrays.asList("customer", "username", "password"));
        return options;
    }

    @dagger.Module(injects = DNSApiManager.class, complete = false, overrides = true, includes = {
            NothingToClose.class, WeightedUnsupported.class, ConcatBasicAndQualifiedResourceRecordSets.class,
            FeignModule.class })
    public static final class Module {

        @Provides
        @Singleton
        Gson json() {
            return new Gson();
        }

        @Provides
        @Singleton
        ZoneApi provideZoneApi(DynECT api) {
            return new DynECTZoneApi(api);
        }

        @Provides
        @Singleton
        GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory(DynECTGeoResourceRecordSetApi.Factory in) {
            return in;
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(DynECT api) {
            return new DynECTResourceRecordSetApi.Factory(api);
        }

        @Provides
        @Singleton
        Map<Factory, Collection<String>> factoryToProfiles(GeoResourceRecordSetApi.Factory in) {
            Map<Factory, Collection<String>> factories = new LinkedHashMap<Factory, Collection<String>>();
            factories.put(in, Arrays.asList("geo"));
            return factories;
        }
    }

    @dagger.Module(injects = DynECTResourceRecordSetApi.Factory.class, complete = false, overrides = true, includes = {
            CountryToRegions.class, Defaults.class, ReflectiveFeign.Module.class })
    public static final class FeignModule {

        @Provides
        @Singleton
        Session session(Feign feign, SessionTarget target) {
            return feign.newInstance(target);
        }

        @Provides
        @Named("Auth-Token")
        public String authToken(InvalidatableTokenProvider supplier) {
            return supplier.get();
        }

        @Provides
        @Singleton
        DynECT dynECT(Feign feign, DynECTTarget target) {
            return feign.newInstance(target);
        }

        @Provides
        @Singleton
        AtomicReference<Boolean> sessionValid(){
            return new AtomicReference<Boolean>(false);
        }

        @Provides
        @Singleton
        Map<String, Decoder> decoders(AtomicReference<Boolean> sessionValid, GeoResourceRecordSetsDecoder geoDecoder) {
            Map<String, Decoder> decoders = new LinkedHashMap<String, Decoder>();
            decoders.put("Session#login(String,String,String)", login(sessionValid));
            decoders.put("DynECT", resourceRecordSets(sessionValid));
            decoders.put("DynECT#zones()", zones(sessionValid));
            decoders.put("DynECT#geoRRSetsByZone()", parseDataWith(sessionValid, geoDecoder));
            decoders.put("DynECT#recordIdsInZoneByNameAndType(String,String,String)", recordIds(sessionValid));
            decoders.put("DynECT#recordsInZoneByNameAndType(String,String,String)", records(sessionValid));
            return decoders;
        }

        @Provides
        @Singleton
        Map<String, FormEncoder> formEncoders(final Gson gson) {
            Map<String, FormEncoder> formEncoders = new LinkedHashMap<String, FormEncoder>();
            formEncoders.put("DynECT", new FormEncoder() {
                @Override
                public void encodeForm(Map<String, ?> formParams, RequestTemplate base) {
                    base.body(gson.toJson(formParams));
                }
            });
            return formEncoders;
        }
    }
}
