package denominator.config;

import static com.google.common.collect.Iterators.concat;

import java.util.Iterator;
import java.util.Set;

import javax.inject.Singleton;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Iterables;

import dagger.Module;
import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.QualifiedResourceRecordSetApi;
import denominator.ReadOnlyResourceRecordSetApi;
import denominator.QualifiedResourceRecordSetApi.Factory;
import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

/**
 * Used when basic and qualified resource record sets are distinct in the
 * backend.
 */
@Module(injects = DNSApiManager.class, complete = false)
public class ConcatBasicAndQualifiedResourceRecordSets {

    @Provides
    @Singleton
    ReadOnlyResourceRecordSetApi.Factory provideReadOnlyResourceRecordSetApiFactory(
            AllProfileResourceRecordSetApi.Factory factory) {
        return factory;
    }

    @Provides
    @Singleton
    AllProfileResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
            final ResourceRecordSetApi.Factory factory,
            final Set<QualifiedResourceRecordSetApi.Factory> qualifiedFactories) {
        return new AllProfileResourceRecordSetApi.Factory() {

            @Override
            public AllProfileResourceRecordSetApi create(String idOrName) {
                Builder<QualifiedResourceRecordSetApi> qualifiedApis = ImmutableSet.builder();
                for (Factory factory : qualifiedFactories) {
                    qualifiedApis.add(factory.create(idOrName).get());
                }
                return new ConcatBasicAndGeoResourceRecordSetApi(factory.create(idOrName), qualifiedApis.build());
            }
        };
    }

    private static class ConcatBasicAndGeoResourceRecordSetApi implements AllProfileResourceRecordSetApi {
        private final ResourceRecordSetApi api;
        private final Set<QualifiedResourceRecordSetApi> qualifiedApis;

        private ConcatBasicAndGeoResourceRecordSetApi(ResourceRecordSetApi api,
                Set<QualifiedResourceRecordSetApi> qualifiedApis) {
            this.api = api;
            this.qualifiedApis = qualifiedApis;
        }

        @Override
        public Iterator<ResourceRecordSet<?>> list() {
            return iterator();
        }

        @Override
        public Iterator<ResourceRecordSet<?>> iterator() {
            Iterator<ResourceRecordSet<?>> iterators = Iterables.concat(qualifiedApis).iterator();
            if (!iterators.hasNext())
                return api.iterator();
            return concat(api.iterator(), iterators);
        }

        @Override
        @Deprecated
        public Iterator<ResourceRecordSet<?>> listByName(String name) {
            return iterateByName(name);
        }

        @Override
        public Iterator<ResourceRecordSet<?>> iterateByName(final String name) {
            Iterator<ResourceRecordSet<?>> iterators = concat(FluentIterable.from(qualifiedApis)
                    .transform(new Function<QualifiedResourceRecordSetApi, Iterator<ResourceRecordSet<?>>>() {

                        @Override
                        public Iterator<ResourceRecordSet<?>> apply(QualifiedResourceRecordSetApi input) {
                            return input.iterateByName(name);
                        }

                    }).iterator());
            if (!iterators.hasNext())
                return api.iterateByName(name);
            return concat(api.iterateByName(name), iterators);
        }

        @Override
        @Deprecated
        public Iterator<ResourceRecordSet<?>> listByNameAndType(String name, String type) {
            return iterateByNameAndType(name, type);
        }

        @Override
        public Iterator<ResourceRecordSet<?>> iterateByNameAndType(final String name, final String type) {
            Iterator<ResourceRecordSet<?>> iterators = concat(FluentIterable.from(qualifiedApis)
                    .transform(new Function<QualifiedResourceRecordSetApi, Iterator<ResourceRecordSet<?>>>() {

                        @Override
                        public Iterator<ResourceRecordSet<?>> apply(QualifiedResourceRecordSetApi input) {
                            return input.iterateByNameAndType(name, type);
                        }

                    }).iterator());
            Optional<ResourceRecordSet<?>> rrs = api.getByNameAndType(name, type);
            if (!iterators.hasNext())
                return rrs.asSet().iterator();
            if (rrs.isPresent())
                return concat(rrs.asSet().iterator(), iterators);
            return iterators;
        }

        @Override
        public Optional<ResourceRecordSet<?>> getByNameTypeAndQualifier(final String name, final String type,
                final String qualifier) {
            return FluentIterable.from(qualifiedApis)
                    .transformAndConcat(new Function<QualifiedResourceRecordSetApi, Set<ResourceRecordSet<?>>>() {

                        @Override
                        public Set<ResourceRecordSet<?>> apply(QualifiedResourceRecordSetApi input) {
                            return input.getByNameTypeAndQualifier(name, type, qualifier).asSet();
                        }

                    }).first();
        }
    }
}
