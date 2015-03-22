package denominator.designate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import denominator.ResourceRecordSetApi;
import denominator.designate.Designate.Record;
import denominator.model.ResourceRecordSet;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.filter;
import static denominator.common.Util.join;
import static denominator.common.Util.nextOrNull;
import static denominator.designate.DesignateFunctions.toRDataMap;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.nameEqualTo;

class DesignateResourceRecordSetApi implements denominator.ResourceRecordSetApi {

  private final Designate api;
  private final String domainId;

  DesignateResourceRecordSetApi(Designate api, String domainId) {
    this.api = api;
    this.domainId = domainId;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    return new GroupByRecordNameAndTypeIterator(api.records(domainId).iterator());
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    // TODO: investigate query by name call in designate
    return filter(iterator(), nameEqualTo(name));
  }

  @Override
  public ResourceRecordSet<?> getByNameAndType(String name, String type) {
    // TODO: investigate query by name and type call in designate
    return nextOrNull(filter(iterator(), nameAndTypeEqualTo(name, type)));
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");
    checkArgument(!rrset.records().isEmpty(), "rrset was empty %s", rrset);

    List<Map<String, Object>>
        recordsLeftToCreate =
        new ArrayList<Map<String, Object>>(rrset.records());

    for (Record record : api.records(domainId)) {
      // TODO: name and type filter
      if (rrset.name().equals(record.name) && rrset.type().equals(record.type)) {
        Map<String, Object> rdata = toRDataMap(record);
        if (recordsLeftToCreate.contains(rdata)) {
          recordsLeftToCreate.remove(rdata);
          if (rrset.ttl() != null) {
            if (rrset.ttl().equals(record.ttl)) {
              continue;
            }
            record.ttl = rrset.ttl();
            api.updateRecord(domainId, record.id, record);
          }
        } else {
          api.deleteRecord(domainId, record.id);
        }
      }
    }

    Record record = new Record();
    record.name = rrset.name();
    record.type = rrset.type();
    record.ttl = rrset.ttl();

    for (Map<String, Object> rdata : recordsLeftToCreate) {
      LinkedHashMap<String, Object> mutable = new LinkedHashMap<String, Object>(rdata);
      if (mutable.containsKey("priority")) { // SRVData
        record.priority = Integer.class.cast(mutable.remove("priority"));
      } else if (mutable.containsKey("preference")) { // MXData
        record.priority = Integer.class.cast(mutable.remove("preference"));
      } else {
        record.priority = null;
      }
      record.data = join(' ', mutable.values().toArray());
      api.createRecord(domainId, record);
    }
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");
    for (Record record : api.records(domainId)) {
      // TODO: name and type filter
      if (name.equals(record.name) && type.equals(record.type)) {
        api.deleteRecord(domainId, record.id);
      }
    }
  }

  static final class Factory implements denominator.ResourceRecordSetApi.Factory {

    private final Designate api;

    @Inject
    Factory(Designate api) {
      this.api = checkNotNull(api, "api");
    }

    @Override
    public ResourceRecordSetApi create(String id) {
      return new DesignateResourceRecordSetApi(api, checkNotNull(id, "id"));
    }
  }
}
