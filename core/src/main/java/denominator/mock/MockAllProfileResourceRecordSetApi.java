package denominator.mock;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

import javax.inject.Inject;

import denominator.AllProfileResourceRecordSetApi;
import denominator.Provider;
import denominator.common.Filter;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.and;
import static denominator.common.Util.filter;
import static denominator.common.Util.nextOrNull;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.nameTypeAndQualifierEqualTo;
import static denominator.model.ResourceRecordSets.notNull;

public class MockAllProfileResourceRecordSetApi
    implements denominator.AllProfileResourceRecordSetApi {

  protected final Provider provider;
  protected final SortedSet<ResourceRecordSet<?>> records;
  protected final Filter<ResourceRecordSet<?>> filter;

  MockAllProfileResourceRecordSetApi(Provider provider, SortedSet<ResourceRecordSet<?>> records,
                                     Filter<ResourceRecordSet<?>> filter) {
    this.provider = provider;
    this.records = records;
    this.filter = filter;
  }

  /**
   * sorted to help tests from breaking
   */
  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    return filter(records.iterator(), filter);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    return filter(records.iterator(), and(nameEqualTo(name), filter));
  }

  protected void put(Filter<ResourceRecordSet<?>> valid, ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(rrset.qualifier() != null, "no qualifier on: %s", rrset);
    checkArgument(valid.apply(rrset), "%s failed on: %s", valid, rrset);
    ResourceRecordSet<?>
        rrsMatch =
        getByNameTypeAndQualifier(rrset.name(), rrset.type(), rrset.qualifier());
    if (rrsMatch != null) {
      records.remove(rrsMatch);
    }
    records.add(rrset);
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    put(notNull(), rrset);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
    return filter(records.iterator(), and(nameAndTypeEqualTo(name, type), filter));
  }

  @Override
  public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type,
                                                        String qualifier) {
    Filter<ResourceRecordSet<?>>
        scoped =
        and(nameTypeAndQualifierEqualTo(name, type, qualifier), filter);
    return nextOrNull(filter(records.iterator(), scoped));
  }

  @Override
  public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
    ResourceRecordSet<?> rrsMatch = getByNameTypeAndQualifier(name, type, qualifier);
    if (rrsMatch != null) {
      records.remove(rrsMatch);
    }
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    for (Iterator<ResourceRecordSet<?>> it = iterateByNameAndType(name, type); it.hasNext(); ) {
      records.remove(it.next());
    }
  }

  static class Factory implements denominator.AllProfileResourceRecordSetApi.Factory {

    private final Provider provider;
    private final Map<Zone, SortedSet<ResourceRecordSet<?>>> records;

    // unbound wildcards are not currently injectable in dagger
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject
    Factory(Provider provider, Map<Zone, SortedSet<ResourceRecordSet>> records) {
      this.provider = provider;
      this.records = Map.class.cast(records);
    }

    @Override
    public AllProfileResourceRecordSetApi create(String idOrName) {
      Zone zone = Zone.create(idOrName);
      checkArgument(records.keySet().contains(zone), "zone %s not found", idOrName);
      return new MockAllProfileResourceRecordSetApi(provider, records.get(zone), notNull());
    }
  }
}
