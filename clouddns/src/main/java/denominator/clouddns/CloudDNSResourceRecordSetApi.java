package denominator.clouddns;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import denominator.ResourceRecordSetApi;
import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.clouddns.RackspaceApis.ListWithNext;
import denominator.clouddns.RackspaceApis.Pager;
import denominator.clouddns.RackspaceApis.Record;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;

import static denominator.clouddns.CloudDNSFunctions.awaitComplete;
import static denominator.clouddns.CloudDNSFunctions.toRDataMap;
import static denominator.clouddns.RackspaceApis.emptyOn404;
import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.filter;
import static denominator.common.Util.join;
import static denominator.common.Util.nextOrNull;
import static denominator.model.ResourceRecordSets.nameEqualTo;

class CloudDNSResourceRecordSetApi implements denominator.ResourceRecordSetApi {

  private static final int DEFAULT_TTL = 300;
  private final CloudDNS api;
  private final int domainId;

  CloudDNSResourceRecordSetApi(CloudDNS api, int domainId) {
    this.api = api;
    this.domainId = domainId;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    Pager<Record> recordPager = new Pager<Record>() {
      public ListWithNext<Record> apply(URI nullOrNext) {
        return nullOrNext == null ? api.records(domainId) : api.records(nullOrNext);
      }
    };
    return new GroupByRecordNameAndTypeIterator(lazyIterateRecords(recordPager));
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    checkNotNull(name, "name was null");
    return filter(iterator(), nameEqualTo(name));
  }

  @Override
  public ResourceRecordSet<?> getByNameAndType(final String name, final String type) {
    checkNotNull(name, "name was null");
    checkNotNull(type, "type was null");
    Pager<Record> recordPager = new Pager<Record>() {
      public ListWithNext<Record> apply(URI nullOrNext) {
        return nullOrNext == null ? api.recordsByNameAndType(domainId, name, type)
                                  : api.records(nullOrNext);
      }
    };
    return nextOrNull(new GroupByRecordNameAndTypeIterator(lazyIterateRecords(recordPager)));
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(!rrset.records().isEmpty(), "rrset was empty %s", rrset);

    List<Map<String, Object>>
        recordsLeftToCreate =
        new ArrayList<Map<String, Object>>(rrset.records());

    for (Record record : api.recordsByNameAndType(domainId, rrset.name(), rrset.type())) {
      Map<String, Object> rdata = toRDataMap(record);

      if (recordsLeftToCreate.contains(rdata)) {
        recordsLeftToCreate.remove(rdata);

        if (rrset.ttl() != null) {
          if (Util.equal(rrset.ttl(), record.ttl)) {
            continue;
          }

          awaitComplete(api, api.updateRecord(domainId, record.id, rrset.ttl(), record.data()));
        }
      } else {
        awaitComplete(api, api.deleteRecord(domainId, record.id));
      }
    }

    int ttlToApply = rrset.ttl() != null ? rrset.ttl() : DEFAULT_TTL;

    for (Map<String, Object> rdata : recordsLeftToCreate) {
      Map<String, Object> mutableRData = new LinkedHashMap<String, Object>(rdata);
      Integer priority = getPriority(mutableRData);
      String data = join(' ', mutableRData.values().toArray());

      if (priority == null) {
        awaitComplete(api,
                      api.createRecord(domainId, rrset.name(), rrset.type(), ttlToApply, data));
      } else {
        awaitComplete(api, api.createRecordWithPriority(
            domainId, rrset.name(), rrset.type(), ttlToApply, data, priority));
      }
    }
  }

  /**
   * Has the side effect of removing the priority from the mutableRData.
   *
   * @return null or the priority, if it exists for a MX or SRV record
   */
  private Integer getPriority(Map<String, Object> mutableRData) {
    Integer priority = null;

    if (mutableRData.containsKey("priority")) { // SRVData
      priority = Integer.class.cast(mutableRData.remove("priority"));
    } else if (mutableRData.containsKey("preference")) { // MXData
      priority = Integer.class.cast(mutableRData.remove("preference"));
    }

    return priority;
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");

    for (Record record : api.recordsByNameAndType(domainId, name, type)) {
      awaitComplete(api, api.deleteRecord(domainId, record.id));
    }
  }

  Iterator<Record> lazyIterateRecords(final Pager<Record> recordPager) {
    final ListWithNext<Record> first = emptyOn404(recordPager, null);

    if (first.next == null) {
      return first.iterator();
    }

    return new Iterator<Record>() {
      Iterator<Record> current = first.iterator();
      URI next = first.next;

      @Override
      public boolean hasNext() {
        while (!current.hasNext() && next != null) {
          ListWithNext<Record> nextPage = emptyOn404(recordPager, next);
          current = nextPage.iterator();
          next = nextPage.next;
        }
        return current.hasNext();
      }

      @Override
      public Record next() {
        return current.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  static final class Factory implements denominator.ResourceRecordSetApi.Factory {

    private final CloudDNS api;

    @Inject
    Factory(CloudDNS api) {
      this.api = api;
    }

    @Override
    public ResourceRecordSetApi create(String id) {
      return new CloudDNSResourceRecordSetApi(api, Integer.parseInt(id));
    }
  }
}
