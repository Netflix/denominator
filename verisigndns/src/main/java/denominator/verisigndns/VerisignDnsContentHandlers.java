package denominator.verisigndns;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import denominator.model.Zone;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

final class VerisignDnsContentHandlers {

  private VerisignDnsContentHandlers() {
  }

  abstract static class ElementHandler extends DefaultHandler {

    private Deque<String> elements = null;
    private String parentEl;

    protected ElementHandler(String parentEl) {
      this.parentEl = parentEl;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {

      if (parentEl.equals(qName)) {
        elements = new ArrayDeque<String>();
      }

      if (elements != null) {
        elements.push(qName);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

      if (elements != null) {
        elements.pop();
      }

      if (parentEl.equals(qName)) {
        elements = null;
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (elements == null) {
        return;
      }

      processElValue(elements.peek(), ch, start, length);
    }

    protected abstract void processElValue(String currentEl, char[] ch, int start, int length);
  }

  static class ZoneHandler extends ElementHandler implements ContentHandlerWithResult<Zone> {
    private String domainName;
    private String email;
    private int ttl;

    ZoneHandler() {
      super("ns3:getZoneInfoRes");
    }

    @Override
    protected void processElValue(String currentEl, char[] ch, int start, int length) {
      if ("ns3:domainName".equals(currentEl)) {
        domainName = val(ch, start, length);
      } else if ("ns3:email".equals(currentEl)) {
        email = val(ch, start, length);
      } else if ("ns3:ttl".equals(currentEl)) {
        ttl = Integer.valueOf(val(ch, start, length));
      }
    }

    @Override
    public Zone result() {
      Zone zone = null;

      if (domainName != null) {
        zone = Zone.create(domainName, domainName, ttl, email);
      }

      return zone;
    }
  }

  static class ZoneListHandler extends ElementHandler implements ContentHandlerWithResult<Page<Zone>> {
    private int count = 0;
    private List<Zone> zones = new ArrayList<Zone>();

    ZoneListHandler() {
      super("ns3:getZoneListRes");
    }

    @Override
    protected void processElValue(String currentEl, char[] ch, int start, int length) {

      if ("ns3:totalCount".equals(currentEl)) {
        String value = val(ch, start, length);
        count = Integer.valueOf(value);
      } else if ("ns3:domainName".equals(currentEl)) {
        String value = val(ch, start, length);
        zones.add(Zone.create(value, value, 86400, "mdnshelp@verisign.com"));
      }
    }

    @Override
    public Page<Zone> result() {
      return new Page<Zone>(zones, count);
    }
  }

  static class RRHandler extends ElementHandler implements ContentHandlerWithResult<Page<ResourceRecord>> {
    private int count = 0;
    private List<ResourceRecord> rrList = new ArrayList<ResourceRecord>();

    RRHandler() {
      super("ns3:getResourceRecordListRes");
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException {
      super.startElement(uri, localName, qName, attributes);
      if ("ns3:resourceRecord".equals(qName)) {
        rrList.add(new ResourceRecord());
      }
    }

    @Override
    protected void processElValue(String currentEl, char[] ch, int start, int length) {
      if (rrList.isEmpty()) {
        return;
      }

      ResourceRecord resourceRecord = rrList.get(rrList.size() - 1);
      String value = val(ch, start, length);
      if ("ns3:totalCount".equals(currentEl)) {
        count = Integer.valueOf(value);
      } else if ("ns3:resourceRecordId".equals(currentEl)) {
        resourceRecord.id = value;
      } else if ("ns3:owner".equals(currentEl)) {
        resourceRecord.name = value;
      } else if ("ns3:type".equals(currentEl)) {
        resourceRecord.type = value;
      } else if ("ns3:rData".equals(currentEl)) {
        resourceRecord.rdata = value;
      } else if ("ns3:ttl".equals(currentEl)) {
        resourceRecord.ttl = Integer.valueOf(value);
      }
    }

    @Override
    public Page<ResourceRecord> result() {
      return new Page<ResourceRecord>(rrList, count);
    }
  }

  static String val(char[] ch, int start, int length) {
    return new String(ch, start, length).trim();
  }

  static class Page<T> {
    private final List<T> list;
    private final int count;

    Page(List<T> list, int count) {
      this.list = list;
      this.count = count;
    }

    List<T> getList() {
      return list;
    }

    int getCount() {
      return count;
    }

    @Override
    public String toString() {
      return String.format("page[count=%d total=%d]", list != null ? list.size() : 0, count);
    }
  }

  static class ResourceRecord {
    private String id;
    private String name;
    private String type;
    private String rdata;
    private Integer ttl;

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public String getRdata() {
      return rdata;
    }

    public Integer getTtl() {
      return ttl;
    }

    @Override
    public String toString() {
      return String.format("rr[id=%s name=%s type=%s rdata=\"%s\" ttl=%d]", id, name, type, rdata, ttl);
    }
  }
}
