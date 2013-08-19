package denominator.dynect;

import static dagger.Provides.Type.SET;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.QualifiedResourceRecordSetApi.Factory;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.ConcatBasicAndQualifiedResourceRecordSets;
import denominator.config.WeightedUnsupported;
import denominator.dynect.DynECT.Record;
import denominator.dynect.DynECTDecoder.NothingForbiddenDecoder;
import denominator.dynect.DynECTDecoder.RecordIdsDecoder;
import denominator.dynect.DynECTDecoder.RecordsByNameAndTypeDecoder;
import denominator.dynect.DynECTDecoder.TokenDecoder;
import denominator.dynect.DynECTDecoder.ZonesDecoder;
import denominator.dynect.InvalidatableTokenProvider.Session;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.profile.GeoResourceRecordSetApi;
import feign.Feign;
import feign.Feign.Defaults;
import feign.ReflectiveFeign;
import feign.codec.Decoder;
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
            WeightedUnsupported.class, ConcatBasicAndQualifiedResourceRecordSets.class, FeignModule.class })
    public static final class Module {

        @Provides
        CheckConnection checkConnection(InvalidatableTokenProvider checkConnection) {
            return checkConnection;
        }

        @Provides
        @Singleton
        Closeable provideCloser(Feign feign) {
            return feign;
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
            return api.hasAllGeoPermissions();
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

    // unbound wildcards are not currently injectable in dagger.
    @SuppressWarnings("rawtypes")
    @dagger.Module(injects = DynECTResourceRecordSetApi.Factory.class, complete = false, includes = {
            CountryToRegions.class, Defaults.class, ReflectiveFeign.Module.class, GsonModule.class })
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
        Decoder loginDecoder(AtomicReference<Boolean> sessionValid) {
            return new DynECTDecoder<String>(sessionValid, TokenDecoder.INSTANCE) {
            };
        }

        @Provides(type = SET)
        Decoder hasAllGeoPermissionsDecoder(AtomicReference<Boolean> sessionValid) {
            return new DynECTDecoder<Boolean>(sessionValid, NothingForbiddenDecoder.INSTANCE) {
            };
        }

        @Provides(type = SET)
        Decoder resourceRecordSetsDecoder(AtomicReference<Boolean> sessionValid, ResourceRecordSetsDecoder decoder) {
            return new DynECTDecoder<Iterator<ResourceRecordSet<?>>>(sessionValid, decoder) {
            };
        }

        @Provides(type = SET)
        Decoder zonesDecoder(AtomicReference<Boolean> sessionValid) {
            return new DynECTDecoder<List<Zone>>(sessionValid, ZonesDecoder.INSTANCE) {
            };
        }

        @Provides(type = SET)
        Decoder geoRRSetsDecoder(AtomicReference<Boolean> sessionValid, GeoResourceRecordSetsDecoder decoder) {
            return new DynECTDecoder<Map<String, Collection<ResourceRecordSet<?>>>>(sessionValid, decoder) {
            };
        }

        @Provides(type = SET)
        Decoder recordIdsDecoder(AtomicReference<Boolean> sessionValid) {
            return new DynECTDecoder<List<String>>(sessionValid, RecordIdsDecoder.INSTANCE) {
            };
        }

        @Provides(type = SET)
        Decoder recordsDecoder(AtomicReference<Boolean> sessionValid) {
            return new DynECTDecoder<Iterator<Record>>(sessionValid, RecordsByNameAndTypeDecoder.INSTANCE) {
            };
        }
    }
}
