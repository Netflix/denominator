package denominator.route53;

import org.xml.sax.helpers.DefaultHandler;

import denominator.model.Zone;
import denominator.route53.Route53.ZoneList;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

/**
 * See <a href= "http://docs.aws.amazon.com/Route53/latest/APIReference/API_ListHostedZones.html"
 * >docs</a>
 */
class ListHostedZonesResponseHandler extends DefaultHandler
    implements ContentHandlerWithResult<ZoneList> {

  private final StringBuilder currentText = new StringBuilder();
  private final ZoneList zones = new ZoneList();
  private String name;
  private String qualifier;
  private String id;

  @Override
  public ZoneList result() {
    return zones;
  }

  @Override
  public void endElement(String uri, String name, String qName) {
    if (qName.equals("Name")) {
      this.name = currentText.toString().trim();
    } else if (qName.equals("Id")) {
      id = currentText.toString().trim().replace("/hostedzone/", "");
    } else if (qName.equals("CallerReference")) {
      qualifier = currentText.toString().trim();
    } else if (qName.equals("HostedZone")) {
      zones.add(Zone.create(this.name, qualifier, id));
      this.name = this.id = null;
    } else if (qName.equals("NextMarker")) {
      zones.next = currentText.toString().trim();
    }
    currentText.setLength(0);
  }

  @Override
  public void characters(char ch[], int start, int length) {
    currentText.append(ch, start, length);
  }
}
