package denominator.verisigndns;

import static denominator.common.Util.peekingIterator;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import denominator.common.PeekingIterator;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;
import denominator.verisigndns.VerisignDnsContentHandlers.Page;
import denominator.verisigndns.VerisignDnsContentHandlers.ResourceRecord;
import denominator.verisigndns.VerisignDnsEncoder.GetRRList;

final class ResourceRecordByNameAndTypeIterator implements Iterator<ResourceRecordSet<?>> {

  private final VerisignDns api;
  private final GetRRList getRRList;
  private final String zoneSuffix;
  private PeekingIterator<ResourceRecord> peekingIterator;

  public ResourceRecordByNameAndTypeIterator(VerisignDns api, GetRRList getRRList) {
    this.api = api;
    this.getRRList = getRRList;
    zoneSuffix = "." + getRRList.getZoneName() + ".";
  }

  @Override
  public boolean hasNext() {
    if (peekingIterator == null || !peekingIterator.hasNext()) {
      nextPeekingIterator();
    }
    return peekingIterator.hasNext();
  }

  private void nextPeekingIterator() {
    if (getRRList.nextPage()) {
      Page<ResourceRecord> rrPage = api.getResourceRecords(getRRList.getZoneName(), getRRList);
      getRRList.setTotal(rrPage.getCount());
      peekingIterator = peekingIterator(rrPage.getList().iterator());
    }
  }

  private String relativeName(String name, String root) {
    if (name.endsWith(root)) {
      name = name.substring(0, name.length() - root.length());
    }
    return name;
  }

  @Override
  public ResourceRecordSet<?> next() {
    if (peekingIterator == null) {
      nextPeekingIterator();
    }
    ResourceRecord record = peekingIterator.next();
    if (record == null) {
      return null;
    }

    String owner = relativeName(record.getName(), zoneSuffix);
    String type = record.getType();
    Builder<Map<String, Object>> builder =
        ResourceRecordSet.builder().name(owner).type(type).ttl(record.getTtl());
    builder.add(getRRTypeAndRdata(type, record.getRdata()));

    while (hasNext()) {
      ResourceRecord next = peekingIterator.peek();
      if (fqdnAndTypeEquals(next, record)) {
        peekingIterator.next();
        builder.add(getRRTypeAndRdata(type, next.getRdata()));
      } else {
        break;
      }
    }
    return builder.build();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  private static boolean fqdnAndTypeEquals(ResourceRecord actual, ResourceRecord expected) {
    return actual.getName().equals(expected.getName()) && actual.getType().equals(expected.getType());
  }

  private static final int NAPTR_FIELD_FLAGS = 2;
  private static final int NAPTR_FIELD_SERVICE = 3;

  private static Map<String, Object> getRRTypeAndRdata(String type, String rdata) {

    rdata = rdata.replace("\"", "");
    try {
      if ("AAAA".equals(type)) {
        rdata = rdata.toUpperCase();
      } else if ("NAPTR".equals(type)) {
        List<String> parts = Util.split(' ', rdata);

        if (parts.size() > NAPTR_FIELD_SERVICE) {
          parts.set(NAPTR_FIELD_FLAGS, parts.get(NAPTR_FIELD_FLAGS).toUpperCase());

          String service = parts.get(NAPTR_FIELD_SERVICE);
          List<String> serviceParts = Util.split('+', service);
          serviceParts.set(0, serviceParts.get(0).toUpperCase());
          parts.set(NAPTR_FIELD_SERVICE, Util.join('+', serviceParts.toArray()));

          rdata = Util.join(' ', parts.toArray());
        }
      }
      return Util.toMap(type, rdata);
    } catch (IllegalArgumentException e) {
      Map<String, Object> map = new LinkedHashMap<String, Object>();
      map.put(type, rdata);
      return map;
    }
  }
}
