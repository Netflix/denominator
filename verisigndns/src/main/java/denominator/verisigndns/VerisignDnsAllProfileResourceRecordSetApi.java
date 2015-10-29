package denominator.verisigndns;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.equal;
import static denominator.common.Util.nextOrNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import denominator.AllProfileResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.verisigndns.VerisignDnsEncoder.GetRRList;

final class VerisignDnsAllProfileResourceRecordSetApi implements AllProfileResourceRecordSetApi {

  private final VerisignDns api;
  private final String zoneName;

  VerisignDnsAllProfileResourceRecordSetApi(VerisignDns api, String zoneName) {
    this.api = api;
    this.zoneName = zoneName;
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    GetRRList getRRList = new GetRRList(zoneName);

    return new ResourceRecordByNameAndTypeIterator(api, getRRList);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    checkNotNull(name, "name");

    GetRRList getRRList = new GetRRList(zoneName, name);

    return new ResourceRecordByNameAndTypeIterator(api, getRRList);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");

    GetRRList getRRList = new GetRRList(zoneName, name, type);

    return new ResourceRecordByNameAndTypeIterator(api, getRRList);
  }

  @Override
  public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type, String qualifier) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");
    checkNotNull(qualifier, "qualifier");

    GetRRList getRRList = new GetRRList(zoneName, name, type, qualifier);

    return nextOrNull(new ResourceRecordByNameAndTypeIterator(api, getRRList));
  }

  private <E> List<E> notIn(List<? extends E> a, List<? extends E> b) {
    List<E> r = new ArrayList<E>();

    for (E i : a) {
      if (!b.contains(i)) {
        r.add(i);
      }
    }

    return r;
  }

  private <E> List<E> copy(List<? extends E> a) {
    List<E> r = new ArrayList<E>();

    r.addAll(a);

    return r;
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkArgument(rrset != null && !rrset.records().isEmpty(), "rrset was empty");

    Integer ttlToApply = rrset.ttl() != null ? rrset.ttl() : 86400;

    ResourceRecordSet<?> oldRRSet = null;
    if (rrset.qualifier() != null) {
      oldRRSet = getByNameTypeAndQualifier(rrset.name(), rrset.type(), rrset.qualifier());
    } else {
      oldRRSet = nextOrNull(iterateByNameAndType(rrset.name(), rrset.type()));
    }

    List<Map<String, Object>> toAdd;
    List<Map<String, Object>> toDel;
    if (oldRRSet != null) {
      if (equal(oldRRSet.ttl(), ttlToApply)) {
        toDel = notIn(oldRRSet.records(), rrset.records());
        toAdd = notIn(rrset.records(), oldRRSet.records());
      } else {
        toDel = copy(oldRRSet.records());
        toAdd = copy(rrset.records());
      }
    } else {
      toDel = null;
      toAdd = copy(rrset.records());
    }

    if (toAdd.isEmpty() && (toDel == null || toDel.isEmpty())) {
      return;
    }

    rrset = ResourceRecordSet.builder()
        .name(rrset.name())
        .type(rrset.type())
        .ttl(ttlToApply)
        .addAll(toAdd)
        .build();

    ResourceRecordSet<Map<String, Object>> rrsetToBeDeleted = null;
    if (toDel != null && !toDel.isEmpty()) {
      rrsetToBeDeleted = ResourceRecordSet.builder()
        .name(oldRRSet.name())
        .type(oldRRSet.type())
        .ttl(oldRRSet.ttl())
        .addAll(toDel)
        .build();
    }

    api.updateResourceRecords(zoneName, rrset, rrsetToBeDeleted);
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");

    ResourceRecordSet<?> rrSet = nextOrNull(iterateByNameAndType(name, type));
    if (rrSet != null) {
      api.deleteResourceRecords(zoneName, rrSet);
    }
  }

  @Override
  public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
    checkNotNull(name, "name");
    checkNotNull(type, "type");
    checkNotNull(qualifier, "rdata for the record");

    ResourceRecordSet<?> rrSet = getByNameTypeAndQualifier(name, type, qualifier);
    if (rrSet != null) {
      api.deleteResourceRecords(zoneName, rrSet);
    }
  }

  static final class Factory implements denominator.AllProfileResourceRecordSetApi.Factory {

    private final VerisignDns api;

    @Inject
    Factory(VerisignDns api) {
      this.api = api;
    }

    @Override
    public VerisignDnsAllProfileResourceRecordSetApi create(String name) {
      return new VerisignDnsAllProfileResourceRecordSetApi(api, name);
    }
  }

}
