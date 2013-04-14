package denominator.config;

import static com.google.common.collect.Iterators.concat;

import java.util.Iterator;

import javax.inject.Singleton;

import com.google.common.base.Optional;

import dagger.Module;
import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.profile.GeoResourceRecordSetApi;

/**
 * Used when normal and geo resource record sets are distinct in the backend.
 */
@Module(entryPoints = DNSApiManager.class, complete = false)
public class ConcatNormalAndGeoResourceRecordSets {

    @Provides
    @Singleton
    AllProfileResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(final ResourceRecordSetApi.Factory factory,
            final GeoResourceRecordSetApi.Factory geoFactory) {
        return new AllProfileResourceRecordSetApi.Factory() {

            @Override
            public AllProfileResourceRecordSetApi create(String zoneName) {
                return new ConcatNormalAndGeoResourceRecordSetApi(factory.create(zoneName), geoFactory.create(zoneName)
                        .get());
            }

        };
    }

    private static class ConcatNormalAndGeoResourceRecordSetApi implements AllProfileResourceRecordSetApi {
        private final ResourceRecordSetApi api;
        private final GeoResourceRecordSetApi geoApi;

        private ConcatNormalAndGeoResourceRecordSetApi(ResourceRecordSetApi api, GeoResourceRecordSetApi geoApi) {
            this.api = api;
            this.geoApi = geoApi;
        }

        @Override
        public Iterator<ResourceRecordSet<?>> list() {
            return concat(api.list(), geoApi.list());
        }

        @Override
        public Iterator<ResourceRecordSet<?>> listByName(String name) {
            return concat(api.listByName(name), geoApi.listByName(name));
        }

        @Override
        public Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type) {
            Optional<ResourceRecordSet<?>> rrs = api.getByNameAndType(name, type);
            if (!geoApi.getSupportedTypes().contains(type))
                return rrs.asSet().iterator();
            if (rrs.isPresent())
                return concat(rrs.asSet().iterator(), geoApi.listByNameAndType(name, type));
            return geoApi.listByNameAndType(name, type);
        }
    }
}
