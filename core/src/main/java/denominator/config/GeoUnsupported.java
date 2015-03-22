package denominator.config;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.profile.GeoResourceRecordSetApi;

/**
 * Some providers do not yet support directional DNS.
 */
@Module(injects = DNSApiManager.class, complete = false)
public class GeoUnsupported {

  @Provides
  @Singleton
  GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory() {
    return new GeoResourceRecordSetApi.Factory() {

      @Override
      public GeoResourceRecordSetApi create(String id) {
        return null;
      }

    };
  }

}
