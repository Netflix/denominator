package denominator.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterators.concat;
import static denominator.model.ResourceRecordSets.toProfileTypes;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Singleton;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSetMultimap.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;

import dagger.Module;
import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.QualifiedResourceRecordSetApi;
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
    AllProfileResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
            final ResourceRecordSetApi.Factory factory, final SetMultimap<Factory, String> factoryToProfiles) {
        return new AllProfileResourceRecordSetApi.Factory() {

            @Override
            public AllProfileResourceRecordSetApi create(String idOrName) {
                Builder<QualifiedResourceRecordSetApi, String> apiToProfiles = ImmutableSetMultimap.builder();
                for (Entry<Factory, Collection<String>> entry : factoryToProfiles.asMap().entrySet()) {
                    Optional<? extends QualifiedResourceRecordSetApi> api = entry.getKey().create(idOrName);
                    if (api.isPresent())
                        apiToProfiles.putAll(api.get(), entry.getValue());
                }
                return new ConcatBasicAndGeoResourceRecordSetApi(factory.create(idOrName), apiToProfiles.build());
            }
        };
    }

    private static class ConcatBasicAndGeoResourceRecordSetApi implements AllProfileResourceRecordSetApi {
        private final ResourceRecordSetApi api;
        private final SetMultimap<QualifiedResourceRecordSetApi, String> apiToProfiles;

        private ConcatBasicAndGeoResourceRecordSetApi(ResourceRecordSetApi api,
                ImmutableSetMultimap<QualifiedResourceRecordSetApi, String> apiToProfiles) {
            this.api = api;
            this.apiToProfiles = apiToProfiles;
        }

        @Override
        public Iterator<ResourceRecordSet<?>> list() {
            return iterator();
        }

        @Override
        public Iterator<ResourceRecordSet<?>> iterator() {
            Iterator<ResourceRecordSet<?>> iterators = Iterables.concat(apiToProfiles.keySet()).iterator();
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
            Iterator<ResourceRecordSet<?>> iterators = concat(FluentIterable.from(apiToProfiles.keySet())
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
            Iterator<ResourceRecordSet<?>> iterators = concat(FluentIterable.from(apiToProfiles.keySet())
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
            return FluentIterable.from(apiToProfiles.keySet())
                    .transformAndConcat(new Function<QualifiedResourceRecordSetApi, Set<ResourceRecordSet<?>>>() {

                        @Override
                        public Set<ResourceRecordSet<?>> apply(QualifiedResourceRecordSetApi input) {
                            return input.getByNameTypeAndQualifier(name, type, qualifier).asSet();
                        }

                    }).first();
        }

        @Override
        public void put(ResourceRecordSet<?> rrset) {
            if (rrset.profiles().isEmpty()) {
                api.put(rrset);
            } else {
                Set<String> profiles = toProfileTypes(rrset);
                Optional<QualifiedResourceRecordSetApi> qualifiedApi = tryFindQualifiedApiForProfiles(profiles);
                checkArgument(qualifiedApi.isPresent(),
                        "cannot put rrset %s:%s%s as it contains profiles %s which aren't supported %s",
                        rrset.name(), rrset.type(), rrset.qualifier().isPresent() ? ":" + rrset.qualifier().get() : "",
                        profiles, apiToProfiles);
                qualifiedApi.get().put(rrset);
            }
        }

        @Override
        public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
            for (QualifiedResourceRecordSetApi qualifiedApi : apiToProfiles.keySet()) {
                qualifiedApi.deleteByNameTypeAndQualifier(name, type, qualifier);
            }
        }

        @Override
        public void deleteByNameAndType(String name, String type) {
            api.deleteByNameAndType(name, type);
            for (QualifiedResourceRecordSetApi qualifiedApi : apiToProfiles.keySet()) {
                for (Iterator<ResourceRecordSet<?>> it = qualifiedApi.iterateByNameAndType(name, type); it.hasNext();) {
                    ResourceRecordSet<?> next = it.next();
                    qualifiedApi.deleteByNameTypeAndQualifier(next.name(), next.type(), next.qualifier().get());
                }
            }
        }

        private Optional<QualifiedResourceRecordSetApi> tryFindQualifiedApiForProfiles(Set<String> profiles) {
            for (Entry<QualifiedResourceRecordSetApi, Collection<String>> entry : apiToProfiles.asMap().entrySet()) {
                if (entry.getValue().containsAll(profiles)) {
                    return Optional.of(entry.getKey());
                }
            }
            return Optional.absent();
        }
    }
}
