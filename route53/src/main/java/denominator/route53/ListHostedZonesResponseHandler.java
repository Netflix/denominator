package denominator.route53;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import denominator.route53.Route53.HostedZone;
import denominator.route53.Route53.HostedZoneList;
import feign.sax.SAXDecoder.ContentHandlerWithResult;

/**
 * See <a href= "http://docs.aws.amazon.com/Route53/latest/APIReference/API_ListHostedZones.html"
 * >docs</a>
 */
class ListHostedZonesResponseHandler extends DefaultHandler
    implements ContentHandlerWithResult<HostedZoneList> {

  private final StringBuilder currentText = new StringBuilder();
  private final HostedZoneList zones = new HostedZoneList();
  private HostedZone zone = new HostedZone();

  @Override
  public HostedZoneList result() {
    return zones;
  }

  @Override
  public void endElement(String uri, String name, String qName) {
    if (qName.equals("Name")) {
      zone.name = currentText.toString().trim();
    } else if (qName.equals("Id")) {
      zone.id = currentText.toString().trim().replace("/hostedzone/", "");
    } else if (qName.equals("HostedZone")) {
      zones.add(zone);
      zone = new HostedZone();
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
