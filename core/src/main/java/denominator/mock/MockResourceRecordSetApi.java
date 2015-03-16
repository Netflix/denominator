package denominator.mock;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import denominator.model.ResourceRecordSet;

import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.nextOrNull;
import static denominator.model.ResourceRecordSets.alwaysVisible;

final class MockResourceRecordSetApi implements denominator.ResourceRecordSetApi {

  private final MockAllProfileResourceRecordSetApi delegate;

  MockResourceRecordSetApi(Map<String, Collection<ResourceRecordSet<?>>> data, String zoneName) {
    this.delegate = new MockAllProfileResourceRecordSetApi(data, zoneName, alwaysVisible());
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    return delegate.iterator();
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    return delegate.iterateByName(name);
  }

  @Override
  public ResourceRecordSet<?> getByNameAndType(String name, String type) {
    return nextOrNull(delegate.iterateByNameAndType(name, type));
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    Collection<ResourceRecordSet<?>> records = delegate.records();
    synchronized (records) {
      removeByNameAndType(records.iterator(), rrset.name(), rrset.type());
      records.add(rrset);
    }
  }

  private void removeByNameAndType(Iterator<ResourceRecordSet<?>> i, String name, String type) {
    while (i.hasNext()) {
      ResourceRecordSet<?> test = i.next();
      if (test.name().equals(name) && test.type().equals(type) && test.qualifier() == null) {
        i.remove();
      }
    }
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    Collection<ResourceRecordSet<?>> records = delegate.records();
    synchronized (records) {
      removeByNameAndType(records.iterator(), name, type);
    }
  }
}
