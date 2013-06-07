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
 * Used when basic and geo resource record sets are distinct in the backend.
 */
@Module(injects = DNSApiManager.class, complete = false)
public class ConcatBasicAndGeoResourceRecordSets {

    @Provides
    @Singleton
    AllProfileResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(final ResourceRecordSetApi.Factory factory,
            final GeoResourceRecordSetApi.Factory geoFactory) {
        return new AllProfileResourceRecordSetApi.Factory() {

            @Override
            public AllProfileResourceRecordSetApi create(String idOrName) {
                return new ConcatBasicAndGeoResourceRecordSetApi(factory.create(idOrName), geoFactory.create(idOrName)
                        .get());
            }

        };
    }

    private static class ConcatBasicAndGeoResourceRecordSetApi implements AllProfileResourceRecordSetApi {
        private final ResourceRecordSetApi api;
        private final GeoResourceRecordSetApi geoApi;

        private ConcatBasicAndGeoResourceRecordSetApi(ResourceRecordSetApi api, GeoResourceRecordSetApi geoApi) {
            this.api = api;
            this.geoApi = geoApi;
        }

        @Override
        public Iterator<ResourceRecordSet<?>> list() {
            return iterator();
        }

        @Override
        public Iterator<ResourceRecordSet<?>> iterator() {
            return concat(api.iterator(), geoApi.iterator());
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
