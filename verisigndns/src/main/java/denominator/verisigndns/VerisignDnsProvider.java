package denominator.verisigndns;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.WeightedUnsupported;
import denominator.verisigndns.VerisignDnsContentHandlers.RRHandler;
import denominator.verisigndns.VerisignDnsContentHandlers.ZoneHandler;
import denominator.verisigndns.VerisignDnsContentHandlers.ZoneListHandler;
import denominator.verisigndns.VerisignDnsErrorDecoder.VerisignDnsError;
import feign.Feign;
import feign.Logger;
import feign.Request.Options;
import feign.codec.Decoder;
import feign.sax.SAXDecoder;

public class VerisignDnsProvider extends BasicProvider {
  private static final String DEFAULT_URL = "https://api.verisigndns.com/dnsa-ws/V2.0/dnsaapi?wsdl";
  private final String url;

  public VerisignDnsProvider() {
    this(null);
  }

  /**
   * Construct a new provider for the Verisign Dns service.
   *
   * @param url if empty or null use default
   */
  public VerisignDnsProvider(String url) {
    this.url = url == null || url.isEmpty() ? DEFAULT_URL : url;
  }

  @Override
  public String url() {
    return url;
  }

  @Override
  public Set<String> basicRecordTypes() {
    Set<String> types = new LinkedHashSet<String>();
    types.addAll(Arrays.asList("A", "AAAA", "CNAME", "MX", "NAPTR", "NS", "PTR", "SRV", "TXT"));
    return types;
  }

  @Override
  public Map<String, Collection<String>> credentialTypeToParameterNames() {
    Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
    options.put("password", Arrays.asList("username", "password"));
    return options;
  }

  @dagger.Module(injects = { DNSApiManager.class },
      complete = false,
      includes = { NothingToClose.class, WeightedUnsupported.class, GeoUnsupported.class, FeignModule.class })
  public static final class Module {

    @Provides
    CheckConnection checkConnection(HostedZonesReadable checkConnection) {
      return checkConnection;
    }

    @Provides
    @Singleton
    ZoneApi provideZoneApi(VerisignDnsZoneApi api) {
      return api;
    }


    @Provides
    @Singleton
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
        VerisignDnsResourceRecordSetApi.Factory factory) {
      return factory;
    }

    @Provides
    @Singleton
    AllProfileResourceRecordSetApi.Factory provideAllProfileResourceRecordSetApiFactory(
        VerisignDnsAllProfileResourceRecordSetApi.Factory factory) {
      return factory;
    }

  }

  @dagger.Module(injects = VerisignDnsResourceRecordSetApi.Factory.class, complete = false)
  public static final class FeignModule {

    @Provides
    @Singleton
    VerisignDns verisignDns(Feign feign, VerisignDnsTarget target) {
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
    Feign feign(Logger logger, Logger.Level logLevel) {

      Options options = new Options(10 * 1000, 10 * 60 * 1000);
      Decoder decoder = decoder();

      return Feign.builder()
          .logger(logger)
          .logLevel(logLevel)
          .options(options)
          .encoder(new VerisignDnsEncoder())
          .decoder(decoder)
          .errorDecoder(new VerisignDnsErrorDecoder(decoder))
          .build();
    }

    static Decoder decoder() {
      return SAXDecoder.builder()
          .registerContentHandler(RRHandler.class)
          .registerContentHandler(ZoneHandler.class)
          .registerContentHandler(ZoneListHandler.class)
          .registerContentHandler(VerisignDnsError.class)
          .build();
    }
  }
}
