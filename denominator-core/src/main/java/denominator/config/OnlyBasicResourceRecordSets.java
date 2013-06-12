package denominator.config;

import java.util.Iterator;

import javax.inject.Singleton;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;

import dagger.Module;
import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.ReadOnlyResourceRecordSetApi;
import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

/**
 * Used when the backend doesn't support any record types except basic ones.
 */
@Module(injects = DNSApiManager.class, complete = false)
public class OnlyBasicResourceRecordSets {

    @Provides
    @Singleton
    ReadOnlyResourceRecordSetApi.Factory provideReadOnlyResourceRecordSetApiFactory(
            AllProfileResourceRecordSetApi.Factory factory) {
        return factory;
    }

    @Provides
    @Singleton
    AllProfileResourceRecordSetApi.Factory provideAllProfileResourceRecordSetApi(
            final ResourceRecordSetApi.Factory factory) {
        return new AllProfileResourceRecordSetApi.Factory() {

            @Override
            public AllProfileResourceRecordSetApi create(String idOrName) {
                return new OnlyBasicResourceRecordSetApi(factory.create(idOrName));
            }

        };
    }

    private static class OnlyBasicResourceRecordSetApi implements AllProfileResourceRecordSetApi {
        private final ResourceRecordSetApi api;

        private OnlyBasicResourceRecordSetApi(ResourceRecordSetApi api) {
            this.api = api;
        }

        @Override
        public Iterator<ResourceRecordSet<?>> list() {
            return iterator();
        }

        @Override
        public Iterator<ResourceRecordSet<?>> iterator() {
            return api.iterator();
        }

        @Override
        @Deprecated
        public Iterator<ResourceRecordSet<?>> listByName(String name) {
            return iterateByName(name);
        }

        @Override
        public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
            return api.iterateByName(name);
        }

        @Override
        @Deprecated
        public Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type) {
            return iterateByNameAndType(name, type);
        }

        @Override
        public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
            Optional<ResourceRecordSet<?>> rrs = api.getByNameAndType(name, type);
            if (rrs.isPresent())
                return Iterators.<ResourceRecordSet<?>> forArray(rrs.get());
            return Iterators.emptyIterator();
        }

        @Override
        public Optional<ResourceRecordSet<?>> getByNameTypeAndQualifier(String name, String type, String qualifier) {
            return Optional.absent();
        }
    }
}
