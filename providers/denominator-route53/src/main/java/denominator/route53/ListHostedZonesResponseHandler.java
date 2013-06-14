package denominator.route53;

import org.xml.sax.helpers.DefaultHandler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import denominator.model.Zone;
import denominator.route53.Route53.ZoneList;

/**
 * @see <a href=
 *      "http://docs.aws.amazon.com/Route53/latest/APIReference/API_ListHostedZones.html"
 *      />
 */
public class ListHostedZonesResponseHandler extends DefaultHandler implements
        feign.codec.SAXDecoder.ContentHandlerWithResult {

    private StringBuilder currentText = new StringBuilder();
    private Builder<Zone> builder = ImmutableList.<Zone> builder();
    private String next;

    @Override
    public ZoneList getResult() {
        try {
            ZoneList list = new ZoneList();
            list.zones = builder.build();
            list.next = next;
            return list;
        } finally {
            builder = ImmutableList.<Zone> builder();
            next = null;
        }
    }

    private String name;
    private String id;

    @Override
    public void endElement(String uri, String name, String qName) {
        if (qName.equals("Name")) {
            this.name = currentText.toString().trim();
        } else if (qName.equals("Id")) {
            id = currentText.toString().trim().replace("/hostedzone/", "");
        } else if (qName.equals("HostedZone")) {
            builder.add(Zone.create(this.name, id));
            this.name = this.id = null;
        } else if (qName.equals("NextMarker")) {
            next = currentText.toString().trim();
        }
        currentText = new StringBuilder();
    }

    @Override
    public void characters(char ch[], int start, int length) {
        currentText.append(ch, start, length);
    }
}
