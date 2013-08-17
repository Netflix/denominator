package denominator.config;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Util.concat;
import static denominator.model.ResourceRecordSets.toProfileTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Singleton;

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
            final ResourceRecordSetApi.Factory factory, final Map<Factory, Collection<String>> factoryToProfiles) {
        return new AllProfileResourceRecordSetApi.Factory() {

            @Override
            public AllProfileResourceRecordSetApi create(String idOrName) {
                Map<QualifiedResourceRecordSetApi, Collection<String>> apiToProfiles = new LinkedHashMap<QualifiedResourceRecordSetApi, Collection<String>>();
                for (Entry<Factory, Collection<String>> entry : factoryToProfiles.entrySet()) {
                    QualifiedResourceRecordSetApi api = entry.getKey().create(idOrName);
                    if (api != null)
                        apiToProfiles.put(api, entry.getValue());
                }
                return new ConcatBasicAndGeoResourceRecordSetApi(factory.create(idOrName), apiToProfiles);
            }
        };
    }

    private static class ConcatBasicAndGeoResourceRecordSetApi implements AllProfileResourceRecordSetApi {
        private final ResourceRecordSetApi api;
        private final Map<QualifiedResourceRecordSetApi, Collection<String>> apiToProfiles;

        private ConcatBasicAndGeoResourceRecordSetApi(ResourceRecordSetApi api,
                Map<QualifiedResourceRecordSetApi, Collection<String>> apiToProfiles) {
            this.api = api;
            this.apiToProfiles = apiToProfiles;
        }

        @Override
        public Iterator<ResourceRecordSet<?>> iterator() {
            Iterator<ResourceRecordSet<?>> iterators = concat(apiToProfiles.keySet());
            if (!iterators.hasNext())
                return api.iterator();
            return concat(api.iterator(), iterators);
        }

        @Override
        public Iterator<ResourceRecordSet<?>> iterateByName(final String name) {
            List<Iterable<ResourceRecordSet<?>>> iterables = new ArrayList<Iterable<ResourceRecordSet<?>>>();
            for (final QualifiedResourceRecordSetApi profile : apiToProfiles.keySet()) {
                iterables.add(new Iterable<ResourceRecordSet<?>>() {
                    public Iterator<ResourceRecordSet<?>> iterator() {
                        return profile.iterateByName(name);
                    }
                });
            }
            if (iterables.isEmpty())
                return api.iterateByName(name);
            return concat(api.iterateByName(name), concat(iterables));
        }

        @Override
        public Iterator<ResourceRecordSet<?>> iterateByNameAndType(final String name, final String type) {
            List<Iterable<ResourceRecordSet<?>>> iterables = new ArrayList<Iterable<ResourceRecordSet<?>>>();
            for (final QualifiedResourceRecordSetApi profile : apiToProfiles.keySet()) {
                iterables.add(new Iterable<ResourceRecordSet<?>>() {
                    public Iterator<ResourceRecordSet<?>> iterator() {
                        return profile.iterateByNameAndType(name, type);
                    }
                });
            }
            ResourceRecordSet<?> rrs = api.getByNameAndType(name, type);
            if (iterables.isEmpty()) {
                return toIterator(rrs);
            }
            if (rrs != null)
                return concat(toIterator(rrs), concat(iterables));
            return concat(iterables);
        }

        Iterator<ResourceRecordSet<?>> toIterator(ResourceRecordSet<?> rrs) {
            return rrs != null ? Collections.<ResourceRecordSet<?>> singleton(rrs).iterator() : Collections
                    .<ResourceRecordSet<?>> emptyList().iterator();
        }

        @Override
        public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type, String qualifier) {
            for (QualifiedResourceRecordSetApi profile : apiToProfiles.keySet()) {
                ResourceRecordSet<?> val = profile.getByNameTypeAndQualifier(name, type, qualifier);
                if (val != null)
                    return val;
            }
            return null;
        }

        @Override
        public void put(ResourceRecordSet<?> rrset) {
            if (rrset.profiles().isEmpty()) {
                api.put(rrset);
            } else {
                Set<String> profiles = toProfileTypes(rrset);
                QualifiedResourceRecordSetApi qualifiedApi = tryFindQualifiedApiForProfiles(profiles);
                checkArgument(qualifiedApi != null,
                        "cannot put rrset %s:%s%s as it contains profiles %s which aren't supported %s", rrset.name(),
                        rrset.type(), rrset.qualifier() != null ? ":" + rrset.qualifier() : "", profiles, apiToProfiles);
                qualifiedApi.put(rrset);
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
                    qualifiedApi.deleteByNameTypeAndQualifier(next.name(), next.type(), next.qualifier());
                }
            }
        }

        private QualifiedResourceRecordSetApi tryFindQualifiedApiForProfiles(Set<String> profiles) {
            for (Entry<QualifiedResourceRecordSetApi, Collection<String>> entry : apiToProfiles.entrySet()) {
                if (entry.getValue().containsAll(profiles)) {
                    return entry.getKey();
                }
            }
            return null;
        }
    }
}
