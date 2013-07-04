package denominator.ultradns;

import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.slurp;
import static feign.codec.Decoders.firstGroup;
import static feign.codec.Decoders.transformEachFirstGroup;
import static java.lang.String.format;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.DNSApiManager;
import denominator.QualifiedResourceRecordSetApi.Factory;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.ConcatBasicAndQualifiedResourceRecordSets;
import denominator.config.NothingToClose;
import denominator.config.WeightedUnsupported;
import denominator.model.Zone;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.ultradns.UltraDNSFormEncoders.RecordAndDirectionalGroup;
import denominator.ultradns.UltraDNSFormEncoders.ZoneAndResourceRecord;
import feign.Feign;
import feign.Feign.Defaults;
import feign.ReflectiveFeign;
import feign.codec.Decoder;
import feign.codec.Decoders.ApplyFirstGroup;
import feign.codec.ErrorDecoder;
import feign.codec.FormEncoder;

public class UltraDNSProvider extends BasicProvider {
    private final String url;

    public UltraDNSProvider() {
        this(null);
    }

    /**
     * @param url
     *            if empty or null use default
     */
    public UltraDNSProvider(String url) {
        this.url = url == null || url.isEmpty() ? "https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01" : url;
    }

    @Override
    public String url() {
        return url;
    }

    /**
     * harvested from the {@code RESOURCE RECORD TYPE CODES} section of the SOAP
     * user guide, dated 2012-11-04.
     */
    @Override
    public Set<String> basicRecordTypes() {
        Set<String> types = new LinkedHashSet<String>();
        types.addAll(Arrays.asList("A", "AAAA", "CNAME", "HINFO", "MX", "NAPTR", "NS", "PTR", "RP", "SOA", "SPF",
                "SRV", "TXT"));
        return types;
    }

    /**
     * directional pools in ultra have types {@code IPV4} and {@code IPV6} which
     * accept both CNAME and address types.
     */
    @Override
    public Map<String, Collection<String>> profileToRecordTypes() {
        Map<String, Collection<String>> profileToRecordTypes = new LinkedHashMap<String, Collection<String>>();
        profileToRecordTypes.put("geo",
                Arrays.asList("A", "AAAA", "CNAME", "HINFO", "MX", "NAPTR", "PTR", "RP", "SRV", "TXT"));
        profileToRecordTypes.put("roundRobin",
                Arrays.asList("A", "AAAA", "HINFO", "MX", "NAPTR", "NS", "PTR", "RP", "SPF", "SRV", "TXT"));
        return profileToRecordTypes;
    }

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
        Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
        options.put("password", Arrays.asList("username", "password"));
        return options;
    }

    @dagger.Module(injects = DNSApiManager.class, complete = false, includes = { NothingToClose.class,
            UltraDNSGeoSupport.class, WeightedUnsupported.class, ConcatBasicAndQualifiedResourceRecordSets.class,
            FeignModule.class })
    public static final class Module {

        @Provides
        @Singleton
        GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory(UltraDNSGeoResourceRecordSetApi.Factory in) {
            return in;
        }

        @Provides
        @Singleton
        ZoneApi provideZoneApi(UltraDNSZoneApi api) {
            return api;
        }

        @Provides
        @Named("accountID")
        String account(InvalidatableAccountIdSupplier accountId) {
            return accountId.get();
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(UltraDNSResourceRecordSetApi.Factory factory) {
            return factory;
        }

        @Provides
        @Singleton
        Map<Factory, Collection<String>> factoryToProfiles(GeoResourceRecordSetApi.Factory in) {
            Map<Factory, Collection<String>> factories = new LinkedHashMap<Factory, Collection<String>>();
            factories.put(in, Arrays.asList("geo"));
            return factories;
        }
    }

    @dagger.Module(injects = UltraDNSResourceRecordSetApi.Factory.class, complete = false, overrides = true, includes = {
            Defaults.class, ReflectiveFeign.Module.class })
    public static final class FeignModule {

        @Provides
        @Singleton
        Map<String, Decoder> decoders() {
            Map<String, Decoder> decoders = new LinkedHashMap<String, Decoder>();
            Decoder saxDecoder = new UltraDNSSAXDecoder();
            decoders.put("UltraDNS", saxDecoder);
            decoders.put("UltraDNS#createRRPoolInZoneForNameAndType(String,String,int)",
                    firstGroup("<RRPoolID[^>]*>([^<]+)</RRPoolID>"));
            decoders.put("UltraDNS#accountId()", firstGroup("accountID=\"([^\"]+)\""));
            decoders.put("UltraDNS#zonesOfAccount(String)",
                    transformEachFirstGroup("zoneName=\"([^\"]+)\"", ZoneFactory.INSTANCE));
            decoders.put(
                    "UltraDNS#directionalPoolNameToIdsInZone(String)",
                    mapEntriesAreGroups(
                            "dirpoolid=\"([^\"]+)\"[^>]+Pooldname=\"([^\"]+)\"[^>]+DirPoolType=\"GEOLOCATION\"", 2, 1));
            decoders.put("UltraDNS#createDirectionalPoolInZoneForNameAndType(String,String,String)",
                    firstGroup("<DirPoolID[^>]*>([^<]+)</DirPoolID>"));
            decoders.put("UltraDNS#createRecordAndDirectionalGroupInPool(DirectionalRecord,DirectionalGroup,String)",
                    firstGroup("<DirectionalPoolRecordID[^>]*>([^<]+)</DirectionalPoolRecordID>"));
            return decoders;
        }

        @Provides
        @Singleton
        Map<String, ErrorDecoder> errorDecoders() {
            Map<String, ErrorDecoder> errorDecoders = new LinkedHashMap<String, ErrorDecoder>();
            errorDecoders.put("UltraDNS", new UltraDNSErrorDecoder());
            return errorDecoders;
        }

        @Provides
        @Singleton
        Map<String, FormEncoder> formEncoders() {
            Map<String, FormEncoder> formEncoders = new LinkedHashMap<String, FormEncoder>();
            FormEncoder recordEncoder = new ZoneAndResourceRecord();
            formEncoders.put("UltraDNS#createRecordInZone(Record,String)", recordEncoder);
            formEncoders.put("UltraDNS#updateRecordInZone(Record,String)", recordEncoder);
            FormEncoder recordAndDirectionalGroupEncoder = new RecordAndDirectionalGroup();
            formEncoders.put(
                    "UltraDNS#createRecordAndDirectionalGroupInPool(DirectionalRecord,DirectionalGroup,String)",
                    recordAndDirectionalGroupEncoder);
            formEncoders.put("UltraDNS#updateRecordAndDirectionalGroup(DirectionalRecord,DirectionalGroup)",
                    recordAndDirectionalGroupEncoder);
            return formEncoders;
        }

        @Provides
        @Singleton
        UltraDNS ultraDNS(Feign feign, UltraDNSTarget target) {
            return feign.newInstance(target);
        }
    }

    /**
     * On each match, group {@code keyGroup} will be the key and group
     * {@code valueGroup} will be added as an entry in the resulting map.
     */
    private static Decoder mapEntriesAreGroups(String pattern, final int keyGroup, final int valueGroup) {
        final Pattern patternForMatcher = compile(checkNotNull(pattern, "pattern"), DOTALL);
        checkNotNull(pattern, "pattern");
        return new Decoder() {
            @Override
            public Object decode(String methodKey, Reader reader, Type type) throws Throwable {
                Matcher matcher = patternForMatcher.matcher(slurp(reader));
                Map<String, String> map = new LinkedHashMap<String, String>();
                while (matcher.find()) {
                    map.put(matcher.group(keyGroup), matcher.group(valueGroup));
                }
                return map;
            }

            @Override
            public String toString() {
                return format("decode %s into Map entries (group %s -> %s)", patternForMatcher, keyGroup, valueGroup);
            }
        };
    }

    private static enum ZoneFactory implements ApplyFirstGroup<Zone> {
        INSTANCE;

        @Override
        public Zone apply(String input) {
            return Zone.create(input);
        }
    };
}
