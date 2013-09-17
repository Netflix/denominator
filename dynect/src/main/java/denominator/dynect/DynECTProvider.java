package denominator.dynect;

import static dagger.Provides.Type.SET;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.gson.TypeAdapter;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.QualifiedResourceRecordSetApi;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.ConcatBasicAndQualifiedResourceRecordSets;
import denominator.config.NothingToClose;
import denominator.config.WeightedUnsupported;
import denominator.dynect.DynECTAdapters.NothingForbiddenAdapter;
import denominator.dynect.DynECTAdapters.RecordIdsAdapter;
import denominator.dynect.DynECTAdapters.RecordsByNameAndTypeAdapter;
import denominator.dynect.DynECTAdapters.TokenAdapter;
import denominator.dynect.DynECTAdapters.ZonesAdapter;
import denominator.dynect.InvalidatableTokenProvider.Session;
import denominator.profile.GeoResourceRecordSetApi;
import feign.Feign;
import feign.codec.ErrorDecoder;
import feign.gson.GsonModule;

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
        types.addAll(Arrays.asList("A", "AAAA", "CERT", "CNAME", "DHCID", "DNAME", "DNSKEY", "DS", "IPSECKEY", "KEY",
                "KX", "LOC", "MX", "NAPTR", "NS", "NSAP", "PTR", "PX", "RP", "SOA", "SPF", "SRV", "SSHFP", "TXT"));
        return types;
    }

    // https://manage.dynect.net/help/docs/api2/rest/resources/Geo.html
    @Override
    public Map<String, Collection<String>> profileToRecordTypes() {
        Map<String, Collection<String>> profileToRecordTypes = new LinkedHashMap<String, Collection<String>>();
        profileToRecordTypes.put("geo", Arrays.asList("A", "AAAAA", "CNAME", "CERT", "MX", "TXT", "SPF", "PTR", "LOC",
                "SRV", "RP", "KEY", "DNSKEY", "SSHFP", "DHCID", "NSAP", "PX"));
        profileToRecordTypes.put("roundRobin", Arrays.asList("A", "AAAA", "CERT", "DHCID", "DNAME", "DNSKEY", "DS",
                "IPSECKEY", "KEY", "KX", "LOC", "MX", "NAPTR", "NS", "NSAP", "PTR", "PX", "RP", "SPF", "SRV", "SSHFP",
                "TXT"));
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
            CountryToRegions.class, FeignModule.class })
    public static final class Module {

        @Provides
        CheckConnection checkConnection(InvalidatableTokenProvider checkConnection) {
            return checkConnection;
        }

        @Provides
        @Singleton
        ZoneApi provideZoneApi(DynECT api) {
            return new DynECTZoneApi(api);
        }

        @Provides
        @Singleton
        @Named("hasAllGeoPermissions")
        Boolean hasAllGeoPermissions(DynECT api) {
            return api.hasAllGeoPermissions().data;
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

        @Provides(type = SET)
        QualifiedResourceRecordSetApi.Factory factoryToProfiles(GeoResourceRecordSetApi.Factory in) {
            return in;
        }
    }

    @dagger.Module(//
    injects = DynECTResourceRecordSetApi.Factory.class, //
    complete = false, // doesn't bind Provider used by SessionTarget
    overrides = true, // ErrorDecoder
    includes = { Feign.Defaults.class, GsonModule.class })
    @SuppressWarnings("rawtypes")
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
        AtomicReference<Boolean> sessionValid() {
            return new AtomicReference<Boolean>(false);
        }

        @Provides(type = SET)
        TypeAdapter loginAdapter() {
            return new TokenAdapter();
        }

        @Provides(type = SET)
        TypeAdapter hasAllGeoPermissionsAdapter() {
            return new NothingForbiddenAdapter();
        }

        @Provides(type = SET)
        TypeAdapter resourceRecordSetsAdapter() {
            return new ResourceRecordSetsAdapter();
        }

        @Provides(type = SET)
        TypeAdapter zonesAdapter() {
            return new ZonesAdapter();
        }

        @Provides(type = SET)
        TypeAdapter recordIdsAdapter() {
            return new RecordIdsAdapter();
        }

        @Provides(type = SET)
        TypeAdapter recordsAdapter() {
            return new RecordsByNameAndTypeAdapter();
        }

        @Provides
        ErrorDecoder errorDecoder(DynECTErrorDecoder errorDecoder) {
            return errorDecoder;
        }
    }
}
