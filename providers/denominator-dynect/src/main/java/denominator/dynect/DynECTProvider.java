package denominator.dynect;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.collect.Iterables.filter;
import static denominator.dynect.DynECTDecoder.login;
import static denominator.dynect.DynECTDecoder.parseDataWith;
import static denominator.dynect.DynECTDecoder.recordIds;
import static denominator.dynect.DynECTDecoder.records;
import static denominator.dynect.DynECTDecoder.resourceRecordSets;
import static denominator.dynect.DynECTDecoder.zones;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
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
import denominator.dynect.InvalidatableTokenSupplier.Session;
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
        url = emptyToNull(url);
        this.url = url != null ? url : "https://api2.dynect.net/REST";
    }

    @Override
    public String url() {
        return url;
    }

    // https://manage.dynect.net/help/docs/api2/rest/resources/index.html
    @Override
    public Set<String> basicRecordTypes() {
        return ImmutableSet.of("A", "AAAA", "CERT", "CNAME", "DHCID", "DNAME", "DNSKEY", "DS", "IPSECKEY", "KEY", "KX",
                "LOC", "MX", "NAPTR", "NS", "NSAP", "PTR", "PX", "RP", "SOA", "SPF", "SRV", "SSHFP", "TXT");
    }

    @Override
    public SetMultimap<String, String> profileToRecordTypes() {
        return ImmutableSetMultimap
                .<String, String> builder()
                // https://manage.dynect.net/help/docs/api2/rest/resources/Geo.html
                .putAll("geo", "A", "AAAAA", "CNAME", "CERT", "MX", "TXT", "SPF", "PTR", "LOC", "SRV", "RP", "KEY",
                        "DNSKEY", "SSHFP", "DHCID", "NSAP", "PX")
                .putAll("roundRobin", filter(basicRecordTypes(), not(in(ImmutableSet.of("SOA", "CNAME"))))).build();
    }

    @Override
    public Multimap<String, String> credentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder().putAll("password", "customer", "username", "password")
                .build();
    }

    @dagger.Module(injects = DNSApiManager.class, complete = false, overrides = true, includes = {
            NothingToClose.class, WeightedUnsupported.class, ConcatBasicAndQualifiedResourceRecordSets.class,
            FeignModule.class })
    public static final class Module {

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
        SetMultimap<Factory, String> factoryToProfiles(GeoResourceRecordSetApi.Factory in) {
            return ImmutableSetMultimap.<Factory, String> of(in, "geo");
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
        public String authToken(InvalidatableTokenSupplier supplier) {
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
            Builder<String, Decoder> decoders = ImmutableMap.<String, Decoder> builder();
            decoders.put("Session#login(String,String,String)", login(sessionValid));
            decoders.put("DynECT", resourceRecordSets(sessionValid));
            decoders.put("DynECT#zones()", zones(sessionValid));
            decoders.put("DynECT#geoRRSetsByZone()", parseDataWith(sessionValid, geoDecoder));
            decoders.put("DynECT#recordIdsInZoneByNameAndType(String,String,String)", recordIds(sessionValid));
            decoders.put("DynECT#recordsInZoneByNameAndType(String,String,String)", records(sessionValid));
            return decoders.build();
        }

        @Provides
        @Singleton
        Map<String, FormEncoder> gsonEncoder() {
            return ImmutableMap.<String, FormEncoder> of("DynECT", new FormEncoder() {
                final Gson gson = new Gson();

                @Override
                public void encodeForm(Map<String, ?> formParams, RequestTemplate base) {
                    base.body(gson.toJson(formParams));
                }
            });
        }
    }

}
