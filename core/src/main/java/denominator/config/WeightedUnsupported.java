package denominator.config;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.profile.WeightedResourceRecordSetApi;

/**
 * Some providers do not yet support weighted records.
 */
@Module(injects = DNSApiManager.class, complete = false)
public class WeightedUnsupported {

  @Provides
  @Singleton
  WeightedResourceRecordSetApi.Factory provideWeightedResourceRecordSetApiFactory() {
    return new WeightedResourceRecordSetApi.Factory() {
      @Override
      public WeightedResourceRecordSetApi create(String id) {
        return null;
      }
    };
  }
}
