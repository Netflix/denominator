package denominator.route53;

import org.xml.sax.helpers.DefaultHandler;

import denominator.route53.Route53.NameAndCount;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

/**
 * See <a href= "http://docs.aws.amazon.com/Route53/latest/APIReference/API_GetHostedZone.html"
 * >docs</a>
 */
class GetHostedZoneResponseHandler extends DefaultHandler
    implements ContentHandlerWithResult<NameAndCount> {

  private final StringBuilder currentText = new StringBuilder();
  private NameAndCount zone = new NameAndCount();

  @Override
  public NameAndCount result() {
    return zone;
  }

  @Override
  public void endElement(String uri, String name, String qName) {
    if (qName.equals("Name")) {
      zone.name = currentText.toString().trim();
    } else if (qName.equals("ResourceRecordSetCount")) {
      zone.resourceRecordSetCount = Integer.parseInt(currentText.toString().trim());
    }
    currentText.setLength(0);
  }

  @Override
  public void characters(char ch[], int start, int length) {
    currentText.append(ch, start, length);
  }
}
