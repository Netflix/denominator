package denominator.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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
import denominator.profile.GeoResourceRecordSetApi;
import denominator.profile.WeightedResourceRecordSetApi;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Util.concat;

/**
 * Used when basic and qualified resource record sets are distinct in the backend.
 */
@Module(injects = DNSApiManager.class, complete = false)
public class ConcatBasicAndQualifiedResourceRecordSets {

  @Provides
  @Singleton
  AllProfileResourceRecordSetApi.Factory provideResourceRecordSetApiFactory(
      final ResourceRecordSetApi.Factory factory, final Set<Factory> factories) {
    return new AllProfileResourceRecordSetApi.Factory() {

      @Override
      public AllProfileResourceRecordSetApi create(String id) {
        Set<QualifiedResourceRecordSetApi>
            qualifiedApis =
            new LinkedHashSet<QualifiedResourceRecordSetApi>();
        for (Factory entry : factories) {
          QualifiedResourceRecordSetApi api = entry.create(id);
          if (api != null) {
            qualifiedApis.add(api);
          }
        }
        return new ConcatBasicAndGeoResourceRecordSetApi(factory.create(id), qualifiedApis);
      }
    };
  }

  private static class ConcatBasicAndGeoResourceRecordSetApi
      implements AllProfileResourceRecordSetApi {

    private final ResourceRecordSetApi api;
    private final Set<QualifiedResourceRecordSetApi> qualifiedApis;

    private ConcatBasicAndGeoResourceRecordSetApi(ResourceRecordSetApi api,
                                                  Set<QualifiedResourceRecordSetApi> qualifiedApis) {
      this.api = api;
      this.qualifiedApis = qualifiedApis;
    }

    static Iterator<ResourceRecordSet<?>> toIterator(ResourceRecordSet<?> rrs) {
      return rrs != null ? Collections.<ResourceRecordSet<?>>singleton(rrs).iterator() : Collections
          .<ResourceRecordSet<?>>emptyList().iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
      Iterator<ResourceRecordSet<?>> iterators = concat(qualifiedApis);
      if (!iterators.hasNext()) {
        return api.iterator();
      }
      return concat(api.iterator(), iterators);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(final String name) {
      List<Iterable<ResourceRecordSet<?>>>
          iterables =
          new ArrayList<Iterable<ResourceRecordSet<?>>>();
      for (final QualifiedResourceRecordSetApi api : qualifiedApis) {
        iterables.add(new Iterable<ResourceRecordSet<?>>() {
          public Iterator<ResourceRecordSet<?>> iterator() {
            return api.iterateByName(name);
          }
        });
      }
      if (iterables.isEmpty()) {
        return api.iterateByName(name);
      }
      return concat(api.iterateByName(name), concat(iterables));
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(final String name,
                                                               final String type) {
      List<Iterable<ResourceRecordSet<?>>>
          iterables =
          new ArrayList<Iterable<ResourceRecordSet<?>>>();
      for (final QualifiedResourceRecordSetApi api : qualifiedApis) {
        iterables.add(new Iterable<ResourceRecordSet<?>>() {
          public Iterator<ResourceRecordSet<?>> iterator() {
            return api.iterateByNameAndType(name, type);
          }
        });
      }
      ResourceRecordSet<?> rrs = api.getByNameAndType(name, type);
      if (iterables.isEmpty()) {
        return toIterator(rrs);
      }
      if (rrs != null) {
        return concat(toIterator(rrs), concat(iterables));
      }
      return concat(iterables);
    }

    @Override
    public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type,
                                                          String qualifier) {
      for (QualifiedResourceRecordSetApi api : qualifiedApis) {
        ResourceRecordSet<?> val = api.getByNameTypeAndQualifier(name, type, qualifier);
        if (val != null) {
          return val;
        }
      }
      return null;
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
      if (rrset.qualifier() == null) {
        api.put(rrset);
        return;
      }
      for (QualifiedResourceRecordSetApi api : qualifiedApis) {
        if (api instanceof GeoResourceRecordSetApi && rrset.geo() != null) {
          api.put(rrset);
          return;
        } else if (api instanceof WeightedResourceRecordSetApi && rrset.weighted() != null) {
          api.put(rrset);
          return;
        }
      }
      Set<String> profiles = new LinkedHashSet<String>();
      if (rrset.geo() != null) {
        profiles.add("geo");
      }
      if (rrset.weighted() != null) {
        profiles.add("weighted");
      }
      checkArgument(false,
                    "cannot put rrset %s:%s:%s as it contains profiles %s which aren't supported %s",
                    rrset.name(), rrset.type(), rrset.qualifier(), profiles, profiles);
    }

    @Override
    public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
      for (QualifiedResourceRecordSetApi qualifiedApi : qualifiedApis) {
        qualifiedApi.deleteByNameTypeAndQualifier(name, type, qualifier);
      }
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
      api.deleteByNameAndType(name, type);
      for (QualifiedResourceRecordSetApi qualifiedApi : qualifiedApis) {
        for (Iterator<ResourceRecordSet<?>> it = qualifiedApi.iterateByNameAndType(name, type);
             it.hasNext(); ) {
          ResourceRecordSet<?> next = it.next();
          qualifiedApi.deleteByNameTypeAndQualifier(next.name(), next.type(), next.qualifier());
        }
      }
    }
  }
}
