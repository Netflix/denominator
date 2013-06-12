package denominator.config;

import javax.inject.Singleton;

import com.google.common.base.Optional;

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
            public Optional<WeightedResourceRecordSetApi> create(String idOrName) {
                return Optional.absent();
            }

        };
    }
}
