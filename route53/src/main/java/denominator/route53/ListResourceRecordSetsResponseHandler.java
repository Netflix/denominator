package denominator.route53;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import denominator.route53.Route53.ResourceRecordSetList;
import denominator.route53.Route53.ResourceRecordSetList.NextRecord;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

/**
 * See <a href= "http://docs.aws.amazon.com/Route53/latest/APIReference/API_ListResourceRecordSets.html"
 * />
 */
class ListResourceRecordSetsResponseHandler extends DefaultHandler implements
                                                                   ContentHandlerWithResult<ResourceRecordSetList> {

  private final StringBuilder currentText = new StringBuilder();
  private final ResourceRecordSetList rrsets = new ResourceRecordSetList();
  private ResourceRecordSetHandler resourceRecordSetHandler = new ResourceRecordSetHandler();
  private NextRecord next = null;
  private boolean inResourceRecordSets;

  @Override
  public ResourceRecordSetList result() {
    rrsets.next = next;
    return rrsets;
  }

  @Override
  public void startElement(String url, String name, String qName, Attributes attributes) {
    if ("ResourceRecordSets".equals(qName)) {
      inResourceRecordSets = true;
    }
  }

  @Override
  public void endElement(String uri, String name, String qName) {
    if (inResourceRecordSets) {
      if ("ResourceRecordSets".equals(qName)) {
        inResourceRecordSets = false;
      } else if (qName.equals("ResourceRecordSet")) {
        rrsets.add(resourceRecordSetHandler.result());
        resourceRecordSetHandler = new ResourceRecordSetHandler();
      } else {
        resourceRecordSetHandler.endElement(uri, name, qName);
      }
    } else if (qName.equals("NextRecordName")) {
      next = new NextRecord(currentText.toString().trim());
    } else if (qName.equals("NextRecordType")) {
      next.type = currentText.toString().trim();
    } else if (qName.equals("NextRecordIdentifier")) {
      next.identifier = currentText.toString().trim();
    }
    currentText.setLength(0);
  }

  @Override
  public void characters(char ch[], int start, int length) {
    if (inResourceRecordSets) {
      resourceRecordSetHandler.characters(ch, start, length);
    } else {
      currentText.append(ch, start, length);
    }
  }
}
