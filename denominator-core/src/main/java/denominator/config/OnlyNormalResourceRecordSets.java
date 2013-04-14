package denominator.config;

import java.util.Iterator;

import javax.inject.Singleton;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;

import dagger.Module;
import dagger.Provides;
import denominator.DNSApiManager;
import denominator.AllProfileResourceRecordSetApi;
import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

/**
 * Used when the backend doesn't support any record types except normal ones.
 */
@Module(entryPoints = DNSApiManager.class, complete = false)
public class OnlyNormalResourceRecordSets {

    @Provides
    @Singleton
    AllProfileResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(final ResourceRecordSetApi.Factory factory) {
        return new AllProfileResourceRecordSetApi.Factory() {

            @Override
            public AllProfileResourceRecordSetApi create(String zoneName) {
                return new OnlyNormalResourceRecordSetApi(factory.create(zoneName));
            }

        };
    }

    private static class OnlyNormalResourceRecordSetApi implements AllProfileResourceRecordSetApi {
        private final ResourceRecordSetApi api;

        private OnlyNormalResourceRecordSetApi(ResourceRecordSetApi api) {
            this.api = api;
        }

        @Override
        public Iterator<ResourceRecordSet<?>> list() {
            return api.list();
        }

        @Override
        public Iterator<ResourceRecordSet<?>> listByName(String name) {
            return api.listByName(name);
        }

        @Override
        public Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type) {
            Optional<ResourceRecordSet<?>> rrs = api.getByNameAndType(name, type);
            if (rrs.isPresent())
                return Iterators.<ResourceRecordSet<?>> forArray(rrs.get());
            return Iterators.emptyIterator();
        }
    }
}
