package denominator.config;

import javax.inject.Singleton;

import com.google.common.base.Optional;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.profile.GeoResourceRecordSetApi;

/**
 * Some providers do not yet support directional DNS.
 */
@Module(entryPoints = DNSApiManager.class, complete = false)
public class GeoUnsupported {

    @Provides
    @Singleton
    GeoResourceRecordSetApi.Factory provideGeoResourceRecordSetApiFactory() {
        return new GeoResourceRecordSetApi.Factory() {

            @Override
            public Optional<GeoResourceRecordSetApi> create(String zoneName) {
                return Optional.absent();
            }

        };
    }

}
