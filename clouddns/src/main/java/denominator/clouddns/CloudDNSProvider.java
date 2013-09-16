package denominator.clouddns;

import static dagger.Provides.Type.SET;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import com.google.gson.TypeAdapter;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.clouddns.RackspaceAdapters.DomainListAdapter;
import denominator.clouddns.RackspaceAdapters.RecordListAdapter;
import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.clouddns.RackspaceApis.CloudIdentity;
import denominator.clouddns.RackspaceApis.TokenIdAndPublicURL;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyBasicResourceRecordSets;
import denominator.config.WeightedUnsupported;
import feign.Feign;
import feign.Target.HardCodedTarget;
import feign.gson.GsonModule;

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
        this.url = url == null || url.isEmpty() ? "https://identity.api.rackspacecloud.com/v2.0" : url;
    }

    @Override
    public String url() {
        return url;
    }

    // TODO: verify when write support is added
    // http://docs.rackspace.com/cdns/api/v1.0/cdns-devguide/content/supported_record_types.htm
    @Override
    public Set<String> basicRecordTypes() {
        Set<String> types = new LinkedHashSet<String>();
        types.addAll(Arrays.asList("A", "AAAA", "CNAME", "MX", "NS", "PTR", "SRV", "TXT"));
        return types;
    }

    // TODO: verify when write support is added
    @Override
    public Map<String, Collection<String>> profileToRecordTypes() {
        Map<String, Collection<String>> profileToRecordTypes = new LinkedHashMap<String, Collection<String>>();
        profileToRecordTypes.put("roundRobin", basicRecordTypes());
        return profileToRecordTypes;
    }

    @Override
    public boolean supportsDuplicateZoneNames() {
        return true;
    }

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
        Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
        options.put("password", Arrays.asList("username", "password"));
        options.put("apiKey", Arrays.asList("username", "apiKey"));
        return options;
    }

    @dagger.Module(injects = DNSApiManager.class, complete = false, overrides = true, includes = {
            NothingToClose.class, GeoUnsupported.class, WeightedUnsupported.class, OnlyBasicResourceRecordSets.class,
            FeignModule.class })
    public static final class Module {

        @Provides
        CheckConnection checkConnection(LimitsReadable checkConnection) {
            return checkConnection;
        }

        @Provides
        @Singleton
        ZoneApi provideZoneApi(CloudDNSZoneApi api) {
            return api;
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(CloudDNSResourceRecordSetApi.Factory api) {
            return api;
        }
    }

    @dagger.Module(//
    injects = CloudDNSResourceRecordSetApi.Factory.class, //
    complete = false, // doesn't bind Provider used by CloudDNSTarget
    includes = { Feign.Defaults.class, GsonModule.class })
    @SuppressWarnings("rawtypes")
    public static final class FeignModule {

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

        @Provides(type = SET)
        TypeAdapter tokenIdAndPublicURLAdapter() {
            return new KeystoneAccessAdapter("rax:dns");
        }

        @Provides(type = SET)
        TypeAdapter domainListAdapter(DomainListAdapter adapter) {
            return adapter;
        }

        @Provides(type = SET)
        TypeAdapter recordListAdapter(RecordListAdapter adapter) {
            return adapter;
        }

        @Provides
        public TokenIdAndPublicURL urlAndToken(InvalidatableAuthProvider supplier) {
            return supplier.get();
        }
    }
}
