package denominator.ultradns;

import static dagger.Provides.Type.SET;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.slurp;
import static feign.codec.Decoders.firstGroup;
import static java.lang.String.format;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.QualifiedResourceRecordSetApi;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.ConcatBasicAndQualifiedResourceRecordSets;
import denominator.config.WeightedUnsupported;
import denominator.model.Zone;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.ultradns.UltraDNS.DirectionalGroup;
import denominator.ultradns.UltraDNS.DirectionalRecord;
import denominator.ultradns.UltraDNS.NameAndType;
import denominator.ultradns.UltraDNS.NetworkStatus;
import denominator.ultradns.UltraDNS.Record;
import denominator.ultradns.UltraDNSContentHandlers.DirectionalGroupHandler;
import denominator.ultradns.UltraDNSContentHandlers.DirectionalRecordListHandler;
import denominator.ultradns.UltraDNSContentHandlers.RRPoolListHandler;
import denominator.ultradns.UltraDNSContentHandlers.RecordListHandler;
import denominator.ultradns.UltraDNSContentHandlers.RegionTableHandler;
import feign.Feign;
import feign.Feign.Defaults;
import feign.Request.Options;
import feign.FeignException;
import feign.ReflectiveFeign;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.codec.Decoders.ApplyFirstGroup;
import feign.codec.Decoders.TransformEachFirstGroup;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.codec.SAXDecoder;
import feign.codec.StringDecoder;

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

    @dagger.Module(injects = DNSApiManager.class, complete = false, includes = { UltraDNSGeoSupport.class,
            WeightedUnsupported.class, ConcatBasicAndQualifiedResourceRecordSets.class, FeignModule.class })
    public static final class Module {

        @Provides
        CheckConnection checkConnection(NetworkStatusReadable checkConnection) {
            return checkConnection;
        }

        @Provides
        @Singleton
        Closeable provideCloser(Feign feign) {
            return feign;
        }

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

        @Provides(type = SET)
        QualifiedResourceRecordSetApi.Factory factoryToProfiles(GeoResourceRecordSetApi.Factory in) {
            return in;
        }
    }

    // unbound wildcards are not currently injectable in dagger.
    @SuppressWarnings("rawtypes")
    @dagger.Module(injects = UltraDNSResourceRecordSetApi.Factory.class, complete = false, overrides = true, includes = {
            Defaults.class, ReflectiveFeign.Module.class })
    public static final class FeignModule {

        /**
         * {@link UltraDNS#updateDirectionalPoolRecord(DirectionalRecord, DirectionalGroup)}
         * and
         * {@link UltraDNS#addDirectionalPoolRecord(DirectionalRecord, DirectionalGroup, String)}
         * can take up to 10 minutes to complete.
         */
        @Provides
        Options options() {
            return new Options(10 * 1000, 10 * 60 * 1000);
        }

        @Provides(type = SET)
        Decoder networkStatusDecoder() {
            return new Decoder.TextStream<NetworkStatus>() {
                private StringDecoder decoder = new StringDecoder();

                @Override
                public NetworkStatus decode(Reader input, Type type) throws IOException, DecodeException,
                        FeignException {
                    String body = decoder.decode(input, String.class);
                    if (body == null)
                        throw new DecodeException("no response body parsing network status");
                    if (body.contains("Good")) {
                        return NetworkStatus.GOOD;
                    } else if (body.contains("Failed")) {
                        return NetworkStatus.FAILED;
                    }
                    throw new DecodeException(format("couldn't parse networkstatus from: %s", body));
                }
            };
        }

        @Provides(type = SET)
        Decoder recordListDecoder(Provider<RecordListHandler> handlers) {
            return new SAXDecoder<List<Record>>(handlers) {
            };
        }

        @Provides(type = SET)
        Decoder directionalRecordListDecoder(Provider<DirectionalRecordListHandler> handlers) {
            return new SAXDecoder<List<DirectionalRecord>>(handlers) {
            };
        }

        @Provides(type = SET)
        Decoder rrPoolListDecoder(Provider<RRPoolListHandler> handlers) {
            return new SAXDecoder<Map<NameAndType, String>>(handlers) {
            };
        }

        @Provides(type = SET)
        Decoder regionTableDecoder(Provider<RegionTableHandler> handlers) {
            return new SAXDecoder<Map<String, Collection<String>>>(handlers) {
            };
        }

        @Provides(type = SET)
        Decoder directionalGroupDecoder(Provider<DirectionalGroupHandler> handlers) {
            return new SAXDecoder<DirectionalGroup>(handlers) {
            };
        }

        @Provides(type = SET)
        Decoder idDecoder() {
            // text of <DirPoolID> <RRPoolID> <DirectionalPoolRecordID
            // xmlns:ns2=...>
            // or attribute accountID, where ids are uppercase hex
            return firstGroup("ID.*[\">]([0-9A-F]+)[\"<]");
        }

        @Provides(type = SET)
        Decoder zonesDecoder() {
            return new TransformEachFirstGroup<Zone>("zoneName=\"([^\"]+)\"", ZoneFactory.INSTANCE) {
            };
        }

        @Provides(type = SET)
        Decoder directionalPoolsDecoder() {
            return mapEntriesAreGroups(
                    "dirpoolid=\"([^\"]+)\"[^>]+Pooldname=\"([^\"]+)\"[^>]+DirPoolType=\"GEOLOCATION\"", 2, 1);
        }

        @Provides(type = SET)
        Encoder formEncoder() {
            return new UltraDNSFormEncoder();
        }

        // This is why we need overrides = true
        @Provides
        ErrorDecoder errorDecoders(UltraDNSErrorDecoder errorDecoder) {
            return errorDecoder;
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
    private static Decoder.TextStream<Map<String, String>> mapEntriesAreGroups(String pattern, final int keyGroup,
            final int valueGroup) {
        final Pattern patternForMatcher = compile(checkNotNull(pattern, "pattern"), DOTALL);
        checkNotNull(pattern, "pattern");
        return new Decoder.TextStream<Map<String, String>>() {
            @Override
            public Map<String, String> decode(Reader reader, Type type) throws IOException {
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
