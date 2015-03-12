package denominator.discoverydns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.Credentials;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyBasicResourceRecordSets;
import denominator.config.WeightedUnsupported;
import denominator.discoverydns.DiscoveryDNS.ResourceRecords;
import denominator.discoverydns.DiscoveryDNSAdapters.ResourceRecordsAdapter;
import feign.Client;
import feign.Feign;
import feign.Logger;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;

import static denominator.common.Preconditions.checkNotNull;

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
    options.put("clientCertificate", Arrays.asList("x509Certificate", "privateKey"));
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


  @dagger.Module(injects = DiscoveryDNS.class, //
      complete = false // doesn't bind Provider used by DiscoveryDNSTarget
  )
  public static final class FeignModule {

    @Provides
    @Singleton
    DiscoveryDNS discoverydns(Feign feign, DiscoveryDNSTarget target) {
      return feign.newInstance(target);
    }

    @Provides
    Logger logger() {
      return new Logger.NoOpLogger();
    }

    @Provides
    Logger.Level logLevel() {
      return Logger.Level.NONE;
    }

    @Provides
    @Singleton
    Feign feign(Logger logger, Logger.Level logLevel, Client client) {
      Gson gson = new GsonBuilder().setPrettyPrinting()
          .registerTypeAdapter(ResourceRecords.class, new ResourceRecordsAdapter()).create();
      return Feign.builder()
          .logger(logger)
          .logLevel(logLevel)
          .client(client)
          .encoder(new GsonEncoder(gson))
          .decoder(new GsonDecoder(gson))
          .build();
    }

    /**
     * Returns a custom client which uses TLS client certificate authentication.
     *
     * <p/>
     * Note: this eagerly fetches credentials in order to construct the SSL context.
     */
    @Provides
    Client client(Credentials creds) {
      return new Client.Default(sslSocketFactory(creds), null);
    }

    static SSLSocketFactory sslSocketFactory(Credentials creds) {
      final X509Certificate certificate;
      final PrivateKey privateKey;
      if (creds instanceof List) {
        @SuppressWarnings("unchecked")
        List<Object> listCreds = (List<Object>) creds;
        certificate = (X509Certificate) listCreds.get(0);
        privateKey = (PrivateKey) listCreds.get(1);
      } else if (creds instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> mapCreds = (Map<String, Object>) creds;
        certificate =
            (X509Certificate) checkNotNull(mapCreds.get("x509Certificate"), "x509Certificate");
        privateKey = (PrivateKey) checkNotNull(mapCreds.get("privateKey"), "privateKey");
      } else {
        throw new IllegalArgumentException("Unsupported credential type: " + creds);
      }

      KeyStore keyStore = keyStore(certificate, privateKey);
      return sslSocketFactory(keyStore);
    }

    private static final char[] KEYSTORE_PASSWORD = "password".toCharArray();

    static KeyStore keyStore(X509Certificate certificate, PrivateKey privateKey) {
      try {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, KEYSTORE_PASSWORD);
        Certificate[] certificateChain = {certificate};
        keyStore.setKeyEntry("privateKey", privateKey, KEYSTORE_PASSWORD, certificateChain);
        keyStore.setCertificateEntry("x509Certificate", certificate);
        return keyStore;
      } catch (GeneralSecurityException e) {
        throw new IllegalArgumentException("Could not create a KeyStore", e);
      } catch (IOException e) {
        throw new IllegalArgumentException("Could not create a KeyStore", e);
      }
    }

    static SSLSocketFactory sslSocketFactory(KeyStore keyStore) {
      try {
        KeyManagerFactory keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD);
        TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.1");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(),
                        new SecureRandom());
        return sslContext.getSocketFactory();
      } catch (GeneralSecurityException e) {
        throw new IllegalArgumentException("Could not initialize SSLSocketFactory!", e);
      }
    }
  }
}
