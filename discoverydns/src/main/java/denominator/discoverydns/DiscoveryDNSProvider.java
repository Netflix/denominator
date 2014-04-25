package denominator.discoverydns;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.cert.Certificate;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.Credentials;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.Credentials.ListCredentials;
import denominator.common.Util;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyBasicResourceRecordSets;
import denominator.config.WeightedUnsupported;
import denominator.discoverydns.crypto.Pems;
import denominator.model.ResourceRecordSet;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;

public class DiscoveryDNSProvider extends BasicProvider {
    private final String url;

    private static final String DEFAULT_TTL = "3600";
    private static final String DEFAULT_CLASS = "IN";

    public DiscoveryDNSProvider() {
        this(null);
    }

    public DiscoveryDNSProvider(String url) {
        this.url = url == null || url.isEmpty() ? "https://api.reseller.discoverydns.com" : url;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public Set<String> basicRecordTypes() {
        Set<String> types = new LinkedHashSet<String>();
        types.addAll(Arrays.asList("NS", "A", "AAAA", "MX", "CNAME", "SRV", "TXT", "PTR",
                                   "DS", "CERT", "NAPTR", "SSHFP", "LOC", "SPF", "TLSA"));
        return types;
    }

    @Override
    public Map<String, Collection<String>> profileToRecordTypes() {
        return new LinkedHashMap<String, Collection<String>>();
    }

    @Override
    public boolean supportsDuplicateZoneNames() {
        return false;
    }

    @Override
    public Map<String, Collection<String>> credentialTypeToParameterNames() {
        Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
        options.put("clientCertificate", Arrays.asList("certificatePem", "keyPem"));
        return options;
    }

    @dagger.Module(injects = DNSApiManager.class,
                   complete = false,
                   includes = { FeignModule.class,
                                NothingToClose.class,
                                GeoUnsupported.class,
                                WeightedUnsupported.class,
                                OnlyBasicResourceRecordSets.class })
    public static final class Module {
        @Provides
        @Singleton
        CheckConnection checkConnection(DiscoveryDNS api) {
            return new DiscoveryDNSCheckConnectionApi(api);
        }

        @Provides
        @Singleton
        ZoneApi provideZoneApi(DiscoveryDNS api) {
            return new DiscoveryDNSZoneApi(api);
        }

        @Provides
        @Singleton
        ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(DiscoveryDNS api) {
            return new DiscoveryDNSResourceRecordSetApi.ResourceRecordSetApiFactory(api);
        }
    }

    @dagger.Module(injects = { DiscoveryDNSResourceRecordSetApi.ResourceRecordSetApiFactory.class,
                               DiscoveryDNSZoneApi.class },
                   complete = false,
                   overrides = true,
                   includes = { Feign.Defaults.class,
                                GsonCodec.class })
    public static final class FeignModule {
        @Provides
        @Singleton
        DiscoveryDNS discoverydns(Feign feign, DiscoveryDNSTarget target) {
            return feign.newInstance(target);
        }

        @Provides
        SSLSocketFactory sslSocketFactory(Provider<Credentials> credentials) {
            List<Object> creds = ListCredentials.asList(credentials.get());
            String cert = creds.get(0).toString();
            String key = creds.get(1).toString();

            try {
                Certificate certObj = Pems.readCertificate(cert);
                PrivateKey keyObj = Pems.readPrivateKey(key);

                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(null, null);
                keyStore.setKeyEntry("dummy alias", keyObj, null, new Certificate[] { certObj });
                kmf.init(keyStore, null);

                SSLContext context = SSLContext.getInstance("TLSv1.1");
                context.init(kmf.getKeyManagers(), null, null);
                return context.getSocketFactory();
            } catch (IOException e) {
                throw new IllegalArgumentException("Key or certificate could not be read");
            } catch (GeneralSecurityException e) {
                throw new IllegalArgumentException("Key or certificate could not be initialised");
            }
        }
    }

    @dagger.Module(injects = { Encoder.class,
                               Decoder.class },
                   overrides = true,
                   addsTo = Feign.Defaults.class)
    static final class GsonCodec {
        @Provides
        @Singleton
        Encoder encoder() {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setPrettyPrinting();
            gsonBuilder.registerTypeAdapter(DiscoveryDNS.ResourceRecords.class, new JsonSerializer<DiscoveryDNS.ResourceRecords>() {
                @Override
                public JsonElement serialize(DiscoveryDNS.ResourceRecords records, Type typeOfSrc, JsonSerializationContext context) {
                    JsonArray array = new JsonArray();
                    for (ResourceRecordSet<?> rrset : records.records) {
                        for (Map<String, Object> rdata : rrset.records()) {
                            JsonObject obj = new JsonObject();
                            obj.addProperty("name", rrset.name());
                            obj.addProperty("class", DEFAULT_CLASS);
                            Integer ttl = rrset.ttl();
                            obj.addProperty("ttl", ttl == null ? DEFAULT_TTL : ttl.toString());
                            obj.addProperty("type", rrset.type());
                            obj.addProperty("rdata", Util.flatten(rdata));
                            array.add(obj);
                        }
                    }
                    return array;
                }
            });
            return new GsonEncoder(gsonBuilder.create());
        }

        @Provides
        @Singleton
        Decoder decoder() {
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(DiscoveryDNS.ResourceRecords.class, new JsonDeserializer<DiscoveryDNS.ResourceRecords>() {
                @Override
                public DiscoveryDNS.ResourceRecords deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    Map<RecordIdentifier, Collection<String>> rrsets = new LinkedHashMap<RecordIdentifier, Collection<String>>();
                    for (JsonElement element : json.getAsJsonArray()) {
                        JsonObject obj = element.getAsJsonObject();
                        String name = obj.get("name").getAsString();
                        String type = obj.get("type").getAsString();
                        Integer ttl = obj.get("ttl").getAsInt();
                        String rdata = obj.get("rdata").getAsString();
                        RecordIdentifier recordIdentifier = new RecordIdentifier(name, type, ttl);
                        if (!rrsets.containsKey(recordIdentifier))
                            rrsets.put(recordIdentifier, new LinkedList<String>());
                        rrsets.get(recordIdentifier).add(rdata);
                    }

                    DiscoveryDNS.ResourceRecords ddnsRecords = new DiscoveryDNS.ResourceRecords();
                    for (Entry<RecordIdentifier, Collection<String>> entry : rrsets.entrySet()) {
                        RecordIdentifier id = entry.getKey();
                        ResourceRecordSet.Builder<Map<String,Object>> builder = ResourceRecordSet.builder()
                            .name(id.name)
                            .type(id.type)
                            .ttl(id.ttl);
                        for (String rdata : entry.getValue())
                            builder.add(Util.toMap(id.type, rdata));
                        ddnsRecords.records.add(builder.build());
                    }
                    return ddnsRecords;
                }

                class RecordIdentifier {
                    String name;
                    String type;
                    Integer ttl;

                    RecordIdentifier(String name, String type, Integer ttl) {
                        this.name = name;
                        this.type = type;
                        this.ttl = ttl;
                    }

                    @Override
                    public int hashCode() {
                        final int prime = 31;
                        int result = 1;
                        result = prime * result + ((name == null) ? 0 : name.hashCode());
                        result = prime * result + ((ttl == null) ? 0 : ttl.hashCode());
                        result = prime * result + ((type == null) ? 0 : type.hashCode());
                        return result;
                    }

                    @Override
                    public boolean equals(Object obj) {
                        return hashCode() == obj.hashCode();
                    }
                }
            });
            return new GsonDecoder(gsonBuilder.create());
        }
    }
}
