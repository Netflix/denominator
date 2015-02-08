package denominator.discoverydns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyBasicResourceRecordSets;
import denominator.config.WeightedUnsupported;
import denominator.discoverydns.DiscoveryDNS.ResourceRecords;
import denominator.discoverydns.DiscoveryDNSAdapters.ResourceRecordsAdapter;
import feign.Feign;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;

public class DiscoveryDNSProvider extends BasicProvider {

  private final String url;

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
      includes = {FeignModule.class,
                  NothingToClose.class,
                  GeoUnsupported.class,
                  WeightedUnsupported.class,
                  OnlyBasicResourceRecordSets.class})
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
      return new DiscoveryDNSResourceRecordSetApi.Factory(api);
    }
  }


  @dagger.Module(//
      injects = DiscoveryDNS.class, //
      complete = false, // doesn't bind Provider used by DiscoveryDNSTarget
      overrides = true, // builds Feign directly + SSLSocketFactory
      includes = Feign.Defaults.class)
  public static final class FeignModule {

    @Provides
    @Singleton
    DiscoveryDNS discoverydns(Feign feign, DiscoveryDNSTarget target) {
      return feign.newInstance(target);
    }

    @Provides
    @Singleton
    Feign feign(Feign.Builder feignBuilder) {
      Gson gson = new GsonBuilder().setPrettyPrinting()
          .registerTypeAdapter(ResourceRecords.class, new ResourceRecordsAdapter()).create();
      return feignBuilder
          .encoder(new GsonEncoder(gson))
          .decoder(new GsonDecoder(gson))
          .build();
    }

    // TODO: this doesn't allow for dynamic credential changes.
    @Provides
    SSLSocketFactory sslSocketFactory(Provider<Credentials> credentials) {
      List<Object> creds = ListCredentials.asList(credentials.get(), new DiscoveryDNSProvider());
      String cert = creds.get(0).toString();
      String key = creds.get(1).toString();

      try {
        Certificate certObj = Pems.readCertificate(cert);
        PrivateKey keyObj = Pems.readPrivateKey(key);

        KeyManagerFactory
            kmf =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("dummy alias", keyObj, null, new Certificate[]{certObj});
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
}
