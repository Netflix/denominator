package denominator.mock;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import denominator.common.Filter;
import denominator.model.ResourceRecordSet;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.and;
import static denominator.common.Util.filter;
import static denominator.common.Util.nextOrNull;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.nameTypeAndQualifierEqualTo;
import static denominator.model.ResourceRecordSets.notNull;

class MockAllProfileResourceRecordSetApi implements denominator.AllProfileResourceRecordSetApi {

  private final Map<String, Collection<ResourceRecordSet<?>>> data;
  private final String zoneName;
  private final Filter<ResourceRecordSet<?>> filter;

  MockAllProfileResourceRecordSetApi(Map<String, Collection<ResourceRecordSet<?>>> data,
                                     String zoneName, Filter<ResourceRecordSet<?>> filter) {
    this.data = data;
    this.zoneName = zoneName;
    this.filter = filter;
  }

  /**
   * sorted to help tests from breaking
   */
  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    return filter(records().iterator(), filter);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    return filter(records().iterator(), and(nameEqualTo(name), filter));
  }

  protected void put(Filter<ResourceRecordSet<?>> valid, ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(rrset.qualifier() != null, "no qualifier on: %s", rrset);
    checkArgument(valid.apply(rrset), "%s failed on: %s", valid, rrset);
    Collection<ResourceRecordSet<?>> records = records();
    synchronized (records) {
      ResourceRecordSet<?>
          rrsMatch =
          getByNameTypeAndQualifier(records, rrset.name(), rrset.type(), rrset.qualifier());
      if (rrsMatch != null) {
        records.remove(rrsMatch);
      }
      records.add(rrset);
    }
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    put(notNull(), rrset);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
    return filter(records().iterator(), and(nameAndTypeEqualTo(name, type), filter));
  }

  @Override
  public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type,
                                                        String qualifier) {
    return getByNameTypeAndQualifier(records(), name, type, qualifier);
  }

  private ResourceRecordSet<?> getByNameTypeAndQualifier(Collection<ResourceRecordSet<?>> records,
                                                         String name, String type,
                                                         String qualifier) {
    Filter<ResourceRecordSet<?>>
        scoped =
        and(nameTypeAndQualifierEqualTo(name, type, qualifier), filter);
    return nextOrNull(filter(records.iterator(), scoped));
  }

  @Override
  public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
    Collection<ResourceRecordSet<?>> records = records();
    synchronized (records) {
      ResourceRecordSet<?> rrsMatch = getByNameTypeAndQualifier(records, name, type, qualifier);
      if (rrsMatch != null) {
        records.remove(rrsMatch);
      }
    }
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    Collection<ResourceRecordSet<?>> records = records();
    synchronized (records) {
      for (Iterator<ResourceRecordSet<?>> it = iterateByNameAndType(name, type); it.hasNext(); ) {
        records.remove(it.next());
      }
    }
  }

  Collection<ResourceRecordSet<?>> records() {
    Collection<ResourceRecordSet<?>> result = data.get(zoneName);
    checkArgument(result != null, "zone %s not found", zoneName);
    return result;
  }
}
