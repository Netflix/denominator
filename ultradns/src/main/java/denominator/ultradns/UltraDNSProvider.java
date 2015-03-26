package denominator.ultradns;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.QualifiedResourceRecordSetApi;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.ConcatBasicAndQualifiedResourceRecordSets;
import denominator.config.NothingToClose;
import denominator.config.WeightedUnsupported;
import denominator.profile.GeoResourceRecordSetApi;
import denominator.ultradns.UltraDNS.DirectionalGroup;
import denominator.ultradns.UltraDNS.DirectionalRecord;
import denominator.ultradns.UltraDNSContentHandlers.DirectionalGroupHandler;
import denominator.ultradns.UltraDNSContentHandlers.DirectionalPoolListHandler;
import denominator.ultradns.UltraDNSContentHandlers.DirectionalRecordListHandler;
import denominator.ultradns.UltraDNSContentHandlers.IDHandler;
import denominator.ultradns.UltraDNSContentHandlers.NetworkStatusHandler;
import denominator.ultradns.UltraDNSContentHandlers.RRPoolListHandler;
import denominator.ultradns.UltraDNSContentHandlers.RecordListHandler;
import denominator.ultradns.UltraDNSContentHandlers.RegionTableHandler;
import denominator.ultradns.UltraDNSContentHandlers.ZoneNamesHandler;
import denominator.ultradns.UltraDNSErrorDecoder.UltraDNSError;
import feign.Feign;
import feign.Logger;
import feign.Request.Options;
import feign.codec.Decoder;
import feign.sax.SAXDecoder;

import static dagger.Provides.Type.SET;

public class UltraDNSProvider extends BasicProvider {

  private final String url;

  public UltraDNSProvider() {
    this(null);
  }

  /**
   * @param url if empty or null use default
   */
  public UltraDNSProvider(String url) {
    this.url =
        url == null || url.isEmpty() ? "https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01" : url;
  }

  @Override
  public String url() {
    return url;
  }

  /**
   * harvested from the {@code RESOURCE RECORD TYPE CODES} section of the SOAP user guide, dated
   * 2012-11-04.
   */
  @Override
  public Set<String> basicRecordTypes() {
    Set<String> types = new LinkedHashSet<String>();
    types.addAll(
        Arrays.asList("A", "AAAA", "CNAME", "HINFO", "MX", "NAPTR", "NS", "PTR", "RP", "SOA", "SPF",
                      "SRV", "TXT"));
    return types;
  }

  /**
   * directional pools in ultra have types {@code IPV4} and {@code IPV6} which accept both CNAME and
   * address types.
   */
  @Override
  public Map<String, Collection<String>> profileToRecordTypes() {
    Map<String, Collection<String>>
        profileToRecordTypes =
        new LinkedHashMap<String, Collection<String>>();
    profileToRecordTypes.put("geo",
                             Arrays
                                 .asList("A", "AAAA", "CNAME", "HINFO", "MX", "NAPTR", "PTR", "RP",
                                         "SRV", "TXT"));
    profileToRecordTypes.put("roundRobin",
                             Arrays.asList("A", "AAAA", "HINFO", "MX", "NAPTR", "NS", "PTR", "RP",
                                           "SPF", "SRV", "TXT"));
    return profileToRecordTypes;
  }

  @Override
  public Map<String, Collection<String>> credentialTypeToParameterNames() {
    Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
    options.put("password", Arrays.asList("username", "password"));
    return options;
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, includes = {NothingToClose.class,
                                                                              UltraDNSGeoSupport.class,
                                                                              WeightedUnsupported.class,
                                                                              ConcatBasicAndQualifiedResourceRecordSets.class,
                                                                              FeignModule.class})
  public static final class Module {

    @Provides
    CheckConnection checkConnection(NetworkStatusReadable checkConnection) {
      return checkConnection;
    }

    @Provides
    @Singleton
    GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory(
        UltraDNSGeoResourceRecordSetApi.Factory in) {
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
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
        UltraDNSResourceRecordSetApi.Factory factory) {
      return factory;
    }

    @Provides(type = SET)
    QualifiedResourceRecordSetApi.Factory factoryToProfiles(GeoResourceRecordSetApi.Factory in) {
      return in;
    }
  }

  @dagger.Module(injects = UltraDNSResourceRecordSetApi.Factory.class,
      complete = false // doesn't bind Provider used by UltraDNSTarget
  )
  public static final class FeignModule {

    @Provides
    @Singleton
    UltraDNS ultraDNS(Feign feign, UltraDNSTarget target) {
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

      /**
       * {@link UltraDNS#updateDirectionalPoolRecord(DirectionalRecord, DirectionalGroup)} and {@link
       * UltraDNS#addDirectionalPoolRecord(DirectionalRecord, DirectionalGroup, String)} can take up
       * to 10 minutes to complete.
       */
      Options options = new Options(10 * 1000, 10 * 60 * 1000);
      Decoder decoder = decoder();
      return Feign.builder()
          .logger(logger)
          .logLevel(logLevel)
          .options(options)
          .encoder(new UltraDNSFormEncoder())
          .decoder(decoder)
          .errorDecoder(new UltraDNSErrorDecoder(decoder))
          .build();
    }

    static Decoder decoder() {
      return SAXDecoder.builder()
          .registerContentHandler(NetworkStatusHandler.class)
          .registerContentHandler(IDHandler.class)
          .registerContentHandler(ZoneNamesHandler.class)
          .registerContentHandler(RecordListHandler.class)
          .registerContentHandler(DirectionalRecordListHandler.class)
          .registerContentHandler(DirectionalPoolListHandler.class)
          .registerContentHandler(RRPoolListHandler.class)
          .registerContentHandler(RegionTableHandler.class)
          .registerContentHandler(DirectionalGroupHandler.class)
          .registerContentHandler(UltraDNSError.class)
          .build();
    }
  }
}
