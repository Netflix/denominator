package denominator.route53;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.collect.Iterables.filter;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;
import com.google.common.reflect.TypeToken;

import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.BasicProvider;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.profile.WeightedResourceRecordSetApi;
import denominator.route53.Route53.ResourceRecordSetList;
import denominator.route53.Route53.ZoneList;
import feign.Feign;
import feign.Feign.Defaults;
import feign.ReflectiveFeign;
import feign.codec.BodyEncoder;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.codec.SAXDecoder;

public class Route53Provider extends BasicProvider {
    private final String url;

    public Route53Provider() {
        this(null);
    }

    /**
     * @param url
     *            if empty or null use default
     */
    public Route53Provider(String url) {
        url = emptyToNull(url);
        this.url = url != null ? url : "https://route53.amazonaws.com";
    }

    @Override
    public String url() {
        return url;
    }

    // http://docs.aws.amazon.com/Route53/latest/APIReference/API_ChangeResourceRecordSets.html
    @Override
    public Set<String> basicRecordTypes() {
        return ImmutableSet.of("A", "AAAA", "CNAME", "MX", "NS", "PTR", "SOA", "SPF", "SRV", "TXT");
    }

    // http://docs.aws.amazon.com/Route53/latest/APIReference/API_ChangeResourceRecordSets.html
    @Override
    public SetMultimap<String, String> profileToRecordTypes() {
        return ImmutableSetMultimap.<String, String> builder()
                .putAll("weighted", "A", "AAAA", "CNAME", "MX", "PTR", "SPF", "SRV", "TXT")//
                .putAll("roundRobin", filter(basicRecordTypes(), not(in(ImmutableSet.of("SOA", "CNAME"))))).build();
    }

    @Override
    public boolean supportsDuplicateZoneNames() {
        return true;
    }

    @Override
    public Multimap<String, String> credentialTypeToParameterNames() {
        return ImmutableMultimap.<String, String> builder()//
                .putAll("accessKey", "accessKey", "secretKey")//
                .putAll("session", "accessKey", "secretKey", "sessionToken").build();
    }

    @dagger.Module(injects = DNSApiManager.class, complete = false, overrides = true, includes = {
            GeoUnsupported.class, InstanceProfileCredentialsProvider.class, NothingToClose.class, FeignModule.class })
    public static final class Module {

        @Provides
        @Singleton
        ZoneApi provideZoneApi(Route53 api) {
            return new Route53ZoneApi(api);
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
                Route53AllProfileResourceRecordSetApi.Factory roApi, Route53 api) {
            return new Route53ResourceRecordSetApi.Factory(roApi, api);
        }

        @Provides
        @Singleton
        AllProfileResourceRecordSetApi.Factory provideAllProfileResourceRecordSetApiFactory(Route53 api) {
            return new Route53AllProfileResourceRecordSetApi.Factory(api);
        }

        @Provides
        WeightedResourceRecordSetApi.Factory provideWeightedResourceRecordSetApiFactory(
                Route53WeightedResourceRecordSetApi.Factory in) {
            return in;
        }

        /**
         * See <a
         *      href="http://docs.aws.amazon.com/Route53/latest/APIReference/API_ChangeResourceRecordSets.html">valid
         *      weights</a>
         */
        @Provides
        @Singleton
        @Named("weighted")
        SortedSet<Integer> provideSupportedWeights() {
            return ContiguousSet.create(Range.closed(0, 255), DiscreteDomain.integers());
        }
    }

    @dagger.Module(injects = Route53ResourceRecordSetApi.Factory.class, complete = false, overrides = true, includes = {
            Defaults.class, ReflectiveFeign.Module.class })
    public static final class FeignModule {
        @Provides
        @Singleton
        Route53 route53(Feign feign, Route53Target target) {
            return feign.newInstance(target);
        }

        @Provides
        public Map<String, String> authHeaders(InvalidatableAuthenticationHeadersSupplier supplier) {
            return supplier.get();
        }

        @Provides
        @Singleton
        Map<String, Decoder> decoders() {
            return ImmutableMap.<String, Decoder> of("Route53", new Route53Decoder());
        }

        @Provides
        @Singleton
        Map<String, ErrorDecoder> errorDecoders() {
            return ImmutableMap.<String, ErrorDecoder> of("Route53", new Route53ErrorDecoder(),
                    "Route53#changeBatch(String,List)", new InvalidChangeBatchErrorDecoder());
        }

        @Provides
        @Singleton
        Map<String, BodyEncoder> bodyEncoders() {
            return ImmutableMap.<String, BodyEncoder> of("Route53#changeBatch(String,List)", new EncodeChanges());
        }
    }

    static class Route53Decoder extends SAXDecoder {
        @Override
        protected ContentHandlerWithResult typeToNewHandler(TypeToken<?> type) {
            if (type.getRawType() == ZoneList.class)
                return new ListHostedZonesResponseHandler();
            else if (type.getRawType() == ResourceRecordSetList.class)
                return new ListResourceRecordSetsResponseHandler();
            throw new UnsupportedOperationException(type + "");
        }
    }
}
