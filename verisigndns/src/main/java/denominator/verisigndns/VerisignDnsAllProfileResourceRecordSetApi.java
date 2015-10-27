package denominator.verisigndns;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static denominator.common.Preconditions.checkNotNull;
import static denominator.common.Util.equal;
import static denominator.common.Util.nextOrNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import denominator.AllProfileResourceRecordSetApi;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
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

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    checkNotNull(rrset, "rrset was null");

    Integer ttlToApply = rrset.ttl() != null ? rrset.ttl() : 86400;

    ResourceRecordSet<?> oldRRSet = null;
    if (rrset.qualifier() != null) {
      oldRRSet = getByNameTypeAndQualifier(rrset.name(), rrset.type(), rrset.qualifier());
    } else {
      oldRRSet = nextOrNull(iterateByNameAndType(rrset.name(), rrset.type()));
    }

    List<Map<String, Object>> newRRData = null;
    List<Map<String, Object>> oldRRData = null;
    if (oldRRSet != null) {
      newRRData = Lists.newArrayList(filter(rrset.records(), not(in(oldRRSet.records()))));
      if (newRRData.isEmpty() && !equal(oldRRSet.ttl(), ttlToApply)) {
        oldRRData = new ArrayList<Map<String, Object>>();
        oldRRData.addAll(oldRRSet.records());
      } else if (newRRData.isEmpty() && equal(oldRRSet.ttl(), ttlToApply)) {
        return;
      } else {
        List<Map<String, Object>> oldRRDataList =
            ImmutableList.copyOf(filter(oldRRSet.records(), in(rrset.records())));

        if (!oldRRDataList.isEmpty()) {
          oldRRData = new ArrayList<Map<String, Object>>();
          oldRRData.addAll(oldRRDataList);
          newRRData.addAll(oldRRDataList);
        }
      }
    } else {
      newRRData = ImmutableList.copyOf(rrset.records());
    }

    Builder<Map<String, Object>> newRRSetBuilder = ResourceRecordSet.builder();
    if (newRRData != null && !newRRData.isEmpty()) {
      rrset =
          newRRSetBuilder.name(rrset.name()).type(rrset.type()).ttl(ttlToApply).addAll(newRRData)
              .build();
    }

    Builder<Map<String, Object>> deleteRRSetBuilder = ResourceRecordSet.builder();
    ResourceRecordSet<Map<String, Object>> rrsetToBeDeleted = null;
    if (oldRRData != null) {
      deleteRRSetBuilder.ttl(oldRRSet.ttl());
      deleteRRSetBuilder.name(oldRRSet.name());
      deleteRRSetBuilder.type(oldRRSet.type());
      deleteRRSetBuilder.addAll(oldRRData);
      rrsetToBeDeleted = deleteRRSetBuilder.build();
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
