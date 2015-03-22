package denominator.config;

import java.util.Collections;
import java.util.Iterator;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import denominator.AllProfileResourceRecordSetApi;
import denominator.DNSApiManager;
import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

/**
 * Used when the backend doesn't support any record types except basic ones.
 */
@Module(injects = DNSApiManager.class, complete = false)
public class OnlyBasicResourceRecordSets {

  @Provides
  @Singleton
  AllProfileResourceRecordSetApi.Factory provideAllProfileResourceRecordSetApi(
      final ResourceRecordSetApi.Factory factory) {
    return new AllProfileResourceRecordSetApi.Factory() {

      @Override
      public AllProfileResourceRecordSetApi create(String id) {
        return new OnlyBasicResourceRecordSetApi(factory.create(id));
      }

    };
  }

  private static class OnlyBasicResourceRecordSetApi implements AllProfileResourceRecordSetApi {

    private final ResourceRecordSetApi api;

    private OnlyBasicResourceRecordSetApi(ResourceRecordSetApi api) {
      this.api = api;
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterator() {
      return api.iterator();
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
      return api.iterateByName(name);
    }

    @Override
    public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
      ResourceRecordSet<?> rrs = api.getByNameAndType(name, type);
      if (rrs != null) {
        return Collections.<ResourceRecordSet<?>>singleton(rrs).iterator();
      }
      return Collections.<ResourceRecordSet<?>>emptySet().iterator();
    }

    @Override
    public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type,
                                                          String qualifier) {
      return null;
    }

    @Override
    public void put(ResourceRecordSet<?> rrset) {
      api.put(rrset);
    }

    @Override
    public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
      api.deleteByNameAndType(name, type);
    }

    @Override
    public void deleteByNameAndType(String name, String type) {
      api.deleteByNameAndType(name, type);
    }
  }
}
