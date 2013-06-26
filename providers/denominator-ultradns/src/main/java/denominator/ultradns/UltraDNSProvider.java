package denominator.ultradns;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.collect.Iterables.filter;
import static feign.codec.Decoders.firstGroup;
import static feign.codec.Decoders.transformEachFirstGroup;
import static java.lang.String.format;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;

import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.io.CharStreams;
import com.google.common.reflect.TypeToken;

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
        url = emptyToNull(url);
        this.url = url != null ? url : "https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01";
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
        return ImmutableSet.of("A", "AAAA", "CNAME", "HINFO", "MX", "NAPTR", "NS", "PTR", "RP", "SOA", "SPF", "SRV",
                "TXT");
    }

    /**
     * directional pools in ultra have types {@code IPV4} and {@code IPV6} which
     * accept both CNAME and address types.
     */
    @Override
    public SetMultimap<String, String> profileToRecordTypes() {
        return ImmutableSetMultimap.<String, String> builder()
                .putAll("geo", "A", "AAAA", "CNAME", "HINFO", "MX", "NAPTR", "PTR", "RP", "SRV", "TXT")
                .putAll("roundRobin", filter(basicRecordTypes(), not(in(ImmutableSet.of("SOA", "CNAME"))))).build();
    }

    @Override
    public Multimap<String, String> credentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder().putAll("password", "username", "password").build();
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
        String account(UltraDNS api) {
            return api.accountId();
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(UltraDNSResourceRecordSetApi.Factory factory) {
            return factory;
        }

        @Provides
        @Singleton
        SetMultimap<Factory, String> factoryToProfiles(GeoResourceRecordSetApi.Factory in) {
            return ImmutableSetMultimap.<Factory, String> of(in, "geo");
        }
    }

    @dagger.Module(injects = UltraDNSResourceRecordSetApi.Factory.class, complete = false, overrides = true, includes = {
            Defaults.class, ReflectiveFeign.Module.class })
    public static final class FeignModule {

        @Provides
        @Singleton
        Map<String, Decoder> decoders() {
            Builder<String, Decoder> decoders = ImmutableMap.<String, Decoder> builder();
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
            return decoders.build();
        }

        @Provides
        @Singleton
        Map<String, ErrorDecoder> errorDecoders() {
            return ImmutableMap.<String, ErrorDecoder> of("UltraDNS", new UltraDNSErrorDecoder());
        }

        @Provides
        @Singleton
        Map<String, FormEncoder> formEncoders() {
            Builder<String, FormEncoder> formEncoders = ImmutableMap.<String, FormEncoder> builder();
            FormEncoder recordEncoder = new ZoneAndResourceRecord();
            formEncoders.put("UltraDNS#createRecordInZone(Record,String)", recordEncoder);
            formEncoders.put("UltraDNS#updateRecordInZone(Record,String)", recordEncoder);
            FormEncoder recordAndDirectionalGroupEncoder = new RecordAndDirectionalGroup();
            formEncoders.put(
                    "UltraDNS#createRecordAndDirectionalGroupInPool(DirectionalRecord,DirectionalGroup,String)",
                    recordAndDirectionalGroupEncoder);
            formEncoders.put("UltraDNS#updateRecordAndDirectionalGroup(DirectionalRecord,DirectionalGroup)",
                    recordAndDirectionalGroupEncoder);
            return formEncoders.build();
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
            public Object decode(String methodKey, Reader reader, TypeToken<?> type) throws Throwable {
                Matcher matcher = patternForMatcher.matcher(CharStreams.toString(reader));
                ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String> builder();
                while (matcher.find()) {
                    builder.put(matcher.group(keyGroup), matcher.group(valueGroup));
                }
                return builder.build();
            }

            @Override
            public String toString() {
                return format("decode %s into Map entries (group %s -> %s)", patternForMatcher, keyGroup, valueGroup);
            }
        };
    }

    private static enum ZoneFactory implements Function<String, Zone> {
        INSTANCE;

        @Override
        public Zone apply(String input) {
            return Zone.create(input);
        }
    };
}
