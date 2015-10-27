package denominator.verisigndns;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;

final class VerisignDnsEncoder implements Encoder {

  private static final String NS_API_1 = "api1";
  private static final String NS_API_2 = "api2";

  @SuppressWarnings("unchecked")
  @Override
  public void encode(Object object, Type bodyType, RequestTemplate template) throws EncodeException {

    Map<String, ?> params = Map.class.cast(object);

    Node node = null;
    if (params.containsKey("rrSet")) {
      node = encodeRRSet(params);
    } else if (params.containsKey("createZone")) {
      node = encodeCreateZone(params);
    } else if (params.containsKey("updateSoa")) {
      node = encodeUpdateSoa(params);
    } else if (params.containsKey("getZone")) {
      node = encodeGetZone(params);
    } else if (params.containsKey("getZoneList")) {
      node = encodeGetZoneList(params);
    } else if (params.containsKey("deleteZone")) {
      node = encodeDeleteZone(params);
    } else if (params.containsKey("getRRList")) {
      node = encodeGetRRList(params);
    } else if (params.containsKey("deleteRRSet")) {
      node = encodeDeleteRRSet(params);
    } else {
      throw new EncodeException("Unsupported param key");
    }

    template.body(node.toXml());
  }

  private String normalize(String email) {
    email = email.replace("@", ".");
    if (!email.endsWith(".")) {
      email += ".";
    }
    return email;
  }

  private Node encodeCreateZone(Map<String, ?> params) {

    Zone zone = Zone.class.cast(params.get("createZone"));
    if (zone == null) {
      return null;
    }

    TagNode zoneNode = new TagNode(NS_API_1, "createZone");
    zoneNode.add(NS_API_1, "domainName", zone.name());
    zoneNode.add(NS_API_1, "type", "DNS Hosting");
    return zoneNode;
  }

  private Node encodeUpdateSoa(Map<String, ?> params) {

    Zone zone = Zone.class.cast(params.get("updateSoa"));
    if (zone == null) {
      return null;
    }

    TagNode soaNode = new TagNode(NS_API_1, "zoneSOAInfo");
    soaNode.add(NS_API_1, "email", normalize(zone.email()));
    soaNode.add(NS_API_1, "retry", 7200);
    soaNode.add(NS_API_1, "ttl", zone.ttl());
    soaNode.add(NS_API_1, "refresh", 86400);
    soaNode.add(NS_API_1, "expire", 1209600);

    TagNode zoneNode = new TagNode(NS_API_1, "updateSOA");
    zoneNode.add(NS_API_1, "domainName", zone.name());
    zoneNode.add(soaNode);
    return zoneNode;
  }

  private Node encodeGetZone(Map<String, ?> params) {

    String zoneName = String.class.cast(params.get("getZone"));
    if (zoneName == null) {
      return null;
    }

    TagNode zoneNode = new TagNode(NS_API_1, "getZoneInfo");
    zoneNode.add(NS_API_1, "domainName", zoneName);
    return zoneNode;
  }

  private Node encodeGetZoneList(Map<String, ?> params) {

    Paging paging = Paging.class.cast(params.get("getZoneList"));
    if (paging == null) {
      return null;
    }

    TagNode zoneListNode = new TagNode(NS_API_1, "getZoneList");
    zoneListNode.add(toPagingNode(paging));
    return zoneListNode;
  }

  private Node encodeDeleteZone(Map<String, ?> params) {

    String zoneName = (String) params.get("deleteZone");
    if (zoneName == null) {
      return null;
    }

    return new TagNode(NS_API_1, "deleteZone").add(NS_API_1, "domainName", zoneName);
  }

  private Node encodeDeleteRRSet(Map<String, ?> params) {

    ResourceRecordSet<?> oldRRSet = ResourceRecordSet.class.cast(params.get("deleteRRSet"));
    if (oldRRSet == null) {
      return null;
    }

    String zoneName = String.class.cast(params.get("zone"));

    TagNode bulkUpdateZoneNode = new TagNode(NS_API_2, "bulkUpdateSingleZone");
    bulkUpdateZoneNode.add(NS_API_2, "domainName", zoneName);
    bulkUpdateZoneNode.add(toRRNode(NS_API_2, "deleteResourceRecords", oldRRSet, false));
    return bulkUpdateZoneNode;
  }

  private Node toPagingNode(Paging paging) {
    TagNode pagingNode = null;

    if (paging != null) {
      pagingNode = new TagNode(NS_API_1, "listPagingInfo");

      pagingNode.add(NS_API_1, "pageNumber", paging.pageNumber);
      pagingNode.add(NS_API_1, "pageSize", paging.pageSize);
    }

    return pagingNode;
  }

  private Node encodeGetRRList(Map<String, ?> params) {

    GetRRList getRRList = GetRRList.class.cast(params.get("getRRList"));
    if (getRRList == null) {
      return null;
    }

    String zoneName = String.class.cast(params.get("zone"));

    TagNode getRRListNode = new TagNode(NS_API_1, "getResourceRecordList");
    getRRListNode.add(NS_API_1, "domainName", zoneName);
    getRRListNode.add(NS_API_1, "owner", getRRList.ownerName);
    getRRListNode.add(NS_API_1, "resourceRecordType", getRRList.type);
    getRRListNode.add(toPagingNode(getRRList.paging));

    return getRRListNode;
  }

  private Node encodeRRSet(Map<String, ?> params) {

    ResourceRecordSet<?> rrSet = ResourceRecordSet.class.cast(params.get("rrSet"));
    if (rrSet == null) {
      return null;
    }

    String zoneName = String.class.cast(params.get("zone"));
    ResourceRecordSet<?> oldRRSet = ResourceRecordSet.class.cast(params.get("oldRRSet"));

    TagNode bulkUpdateZoneNode = new TagNode(NS_API_2, "bulkUpdateSingleZone");
    bulkUpdateZoneNode.add(NS_API_2, "domainName", zoneName);
    bulkUpdateZoneNode.add(toRRNode(NS_API_2, "createResourceRecords", rrSet, true));
    if (oldRRSet != null) {
      bulkUpdateZoneNode.add(toRRNode(NS_API_2, "deleteResourceRecords", oldRRSet, false));
    }

    return bulkUpdateZoneNode;
  }

  private Node toRRNode(String ns, String tag, ResourceRecordSet<?> rrSet, boolean includeTtl) {

    String name = rrSet.name();
    String type = rrSet.type();
    Integer ttl = rrSet.ttl();
    TagNode rrsNode = new TagNode(ns, tag);

    for (Map<String, Object> record : rrSet.records()) {
      TagNode rrNode = new TagNode(ns, "resourceRecord");
      rrNode.add(ns, "owner", name);
      rrNode.add(ns, "type", type);
      rrNode.add(ns, "rData", Util.flatten(record));
      if (includeTtl && ttl != null) {
        rrNode.add(ns, "ttl", ttl.intValue());
      }
      rrsNode.add(rrNode);
    }
    return rrsNode;
  }

  static class GetRRList {
    private String zoneName;
    private String ownerName;
    private String type;
    private String viewName;
    private Paging paging;

    public GetRRList(String zoneName) {
      this.zoneName = zoneName;
    }

    public GetRRList(String zoneName, String ownerName) {
      this.zoneName = zoneName;
      this.ownerName = ownerName;
    }

    public GetRRList(String zoneName, String ownerName, String type) {
      this.zoneName = zoneName;
      this.ownerName = ownerName;
      this.type = type;
    }

    public GetRRList(String zoneName, String ownerName, String type, String viewName) {
      this.zoneName = zoneName;
      this.ownerName = ownerName;
      this.type = type;
      this.viewName = viewName;
    }

    public String getZoneName() {
      return zoneName;
    }

    public boolean nextPage() {
      if (paging == null) {
        paging = new Paging(1);
        return true;
      } else {
        return paging.nextPage();
      }
    }

    public void setTotal(int total) {
      paging.setTotal(total);
    }

    @Override
    public String toString() {
      return String.format("getrrlist[zone=%s owner=%s type=%s view=%s paging=%s]",
          zoneName, ownerName, type, viewName, paging);
    }
  }

  static class Paging {
    private int pageNumber;
    private int pageSize;
    private int total;

    Paging(int pageNumber) {
      this.pageNumber = pageNumber;
      this.pageSize = 100;
    }

    Paging(int pageNumber, int pageSize) {
      this.pageNumber = pageNumber;
      this.pageSize = pageSize;
    }

    void setTotal(int total) {
      this.total = total;
    }

    int getPages() {
      return (total / pageSize) + ((total % pageSize) == 0 ? 0 : 1);
    }

    boolean nextPage() {
      return ++pageNumber < total;
    }

    @Override
    public String toString() {
      return String.format("paging[page=%d size=%d total=%d pages=%d]", pageNumber, pageSize, total, getPages());
    }
  }

  interface Node {
    String toXml();
  }

  class TextNode implements Node {

    private final String value;

    TextNode(String value) {
      this.value = value;
    }

    @Override
    public String toXml() {
      return value;
    }
  }

  class TagNode implements Node {

    private final String ns;
    private final String tag;
    private final List<Node> children;

    TagNode(String ns, String tag) {
      this.ns = ns;
      this.tag = tag;
      this.children = new ArrayList<Node>();
    }

    String getTag() {
      return tag;
    }

    List<Node> getChildren() {
      return children;
    }

    TagNode add(Node node) {
      if (node != null) {
        children.add(node);
      }
      return this;
    }

    TagNode add(String cns, String ctag, String value) {
      if (value != null) {
        children.add(new TagNode(cns, ctag).add(new TextNode(value)));
      }
      return this;
    }

    TagNode add(String cns, String ctag, int value) {
      return add(cns, ctag, Integer.toString(value));
    }

    @Override
    public String toXml() {

      StringBuilder sb = new StringBuilder();
      sb.append("<").append(ns).append(":").append(tag).append(">");
      for (Node child : children) {
        sb.append(child.toXml());
      }
      sb.append("</").append(ns).append(":").append(tag).append(">");

      return sb.toString();
    }
  }
}
