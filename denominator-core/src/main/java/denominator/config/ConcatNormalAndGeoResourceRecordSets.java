package denominator.config;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.profile.GeoResourceRecordSetApi;

/**
 * @deprecated Will be removed in denominator 2.0. Please use
 *             {@link ConcatBasicAndGeoResourceRecordSets}
 */
@Deprecated
@Module(injects = DNSApiManager.class, complete = false)
public class ConcatNormalAndGeoResourceRecordSets {

    @Provides
    @Singleton
    AllProfileResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
            final ResourceRecordSetApi.Factory factory, final GeoResourceRecordSetApi.Factory geoFactory) {
        return new ConcatBasicAndGeoResourceRecordSets().provideResourceRecordSetApiFactory(factory, geoFactory);
    }
}
