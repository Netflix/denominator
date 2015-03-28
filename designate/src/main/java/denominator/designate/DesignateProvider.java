package denominator.designate;

import com.google.gson.TypeAdapter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import dagger.Provides;
import denominator.BasicProvider;
import denominator.CheckConnection;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.config.GeoUnsupported;
import denominator.config.NothingToClose;
import denominator.config.OnlyBasicResourceRecordSets;
import denominator.config.WeightedUnsupported;
import denominator.designate.DesignateAdapters.DomainListAdapter;
import denominator.designate.DesignateAdapters.RecordAdapter;
import denominator.designate.DesignateAdapters.RecordListAdapter;
import feign.Feign;
import feign.Logger;
import feign.Target.EmptyTarget;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;

public class DesignateProvider extends BasicProvider {

  private final String url;

  public DesignateProvider() {
    this(null);
  }

  /**
   * @param url if empty or null use default
   */
  public DesignateProvider(String url) {
    this.url = url == null || url.isEmpty() ? "http://localhost:5000/v2.0" : url;
  }

  @Override
  public String url() {
    return url;
  }

  // http://docs.hpcloud.com/api/dns/#create_record-jumplink-span
  @Override
  public Set<String> basicRecordTypes() {
    Set<String> types = new LinkedHashSet<String>();
    types.addAll(Arrays.asList("A", "AAAA", "CNAME", "MX", "NS", "SRV", "TXT"));
    return types;
  }

  @Override
  public Map<String, Collection<String>> profileToRecordTypes() {
    Map<String, Collection<String>>
        profileToRecordTypes =
        new LinkedHashMap<String, Collection<String>>();
    profileToRecordTypes.put("roundRobin", Arrays.asList("A", "AAAA", "MX", "NS", "SRV", "TXT"));
    return profileToRecordTypes;
  }

  @Override
  public Map<String, Collection<String>> credentialTypeToParameterNames() {
    Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
    options.put("password", Arrays.asList("tenantId", "username", "password"));
    return options;
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, overrides = true, includes = {
      NothingToClose.class, GeoUnsupported.class, WeightedUnsupported.class,
      OnlyBasicResourceRecordSets.class,
      FeignModule.class})
  public static final class Module {

    @Provides
    CheckConnection checkConnection(LimitsReadable checkConnection) {
      return checkConnection;
    }

    @Provides
    @Singleton
    ZoneApi provideZoneApi(DesignateZoneApi in) {
      return in;
    }

    @Provides
    @Singleton
    ResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
        DesignateResourceRecordSetApi.Factory in) {
      return in;
    }
  }

  @dagger.Module(injects = DesignateResourceRecordSetApi.Factory.class,
      complete = false // doesn't bind Provider used by DesignateTarget
  )
  public static final class FeignModule {

    @Provides
    @Singleton
    Designate designate(Feign feign, DesignateTarget target) {
      return feign.newInstance(target);
    }

    @Provides
    @Singleton
    KeystoneV2 keystoneV2(Feign feign) {
      return feign.newInstance(EmptyTarget.create(KeystoneV2.class, "keystone"));
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
      RecordAdapter recordAdapter = new RecordAdapter();
      return Feign.builder()
          .logger(logger)
          .logLevel(logLevel)
          .encoder(new GsonEncoder(Collections.<TypeAdapter<?>>singleton(recordAdapter)))
          .decoder(new GsonDecoder(Arrays.asList(
                       new KeystoneV2AccessAdapter(),
                       recordAdapter,
                       new DomainListAdapter(),
                       new RecordListAdapter()))
          )
          .build();
    }
  }
}
