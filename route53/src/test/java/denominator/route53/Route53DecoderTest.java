package denominator.route53;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.InputStreamReader;

import javax.inject.Provider;

import org.testng.annotations.Test;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Weighted;
import denominator.model.rdata.AData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.SOAData;
import denominator.route53.Route53.ResourceRecordSetList;
import denominator.route53.Route53.ZoneList;
import feign.codec.SAXDecoder;

public class Route53DecoderTest {

    @Test
    public void decodeZoneListWithNext() throws Throwable {
        ZoneList result = new SAXDecoder<ZoneList>(new Provider<ListHostedZonesResponseHandler>() {
            public ListHostedZonesResponseHandler get() {
                return new ListHostedZonesResponseHandler();
            }
        }) {
        }.decode(new InputStreamReader(getClass().getResourceAsStream("/hosted_zones.xml")), ZoneList.class);

        assertEquals(result.size(), 2);
        assertEquals(result.get(0), Zone.create("example.com.", "Z21DW1QVGID6NG"));
        assertEquals(result.get(1), Zone.create("example2.com.", "Z2682N5HXP0BZ4"));
        assertEquals(result.next, "Z333333YYYYYYY");
    }

    @Test
    public void decodeBasicResourceRecordSetListWithNext() throws Throwable {
        ResourceRecordSetList result = new SAXDecoder<ResourceRecordSetList>(
                new Provider<ListResourceRecordSetsResponseHandler>() {
                    public ListResourceRecordSetsResponseHandler get() {
                        return new ListResourceRecordSetsResponseHandler();
                    }
                }) {
        }.decode(new InputStreamReader(getClass().getResourceAsStream("/basic_rrsets.xml")),
                ResourceRecordSetList.class);

        assertEquals(result.size(), 2);
        assertEquals(result.get(0), ResourceRecordSet.<SOAData> builder()//
                .name("example.com.")//
                .type("SOA")//
                .ttl(900)//
                .add(SOAData.builder()//
                        .mname("ns-2048.awsdns-64.net.")//
                        .rname("hostmaster.awsdns.com.")//
                        .serial(1)//
                        .refresh(7200)//
                        .retry(900)//
                        .expire(1209600)//
                        .minimum(86400)//
                        .build()).build());
        assertEquals(result.get(1), ResourceRecordSet.<NSData> builder()//
                .name("example.com.")//
                .type("NS")//
                .ttl(172800)//
                .add(NSData.create("ns-2048.awsdns-64.com."))//
                .add(NSData.create("ns-2049.awsdns-65.net."))//
                .add(NSData.create("ns-2050.awsdns-66.org."))//
                .add(NSData.create("ns-2051.awsdns-67.co.uk.")).build());
        assertEquals(result.next.name, "testdoc2.example.com");
        assertEquals(result.next.type, "NS");
        assertNull(result.next.identifier);
    }

    @Test
    public void decodeAdvancedResourceRecordSetListWithoutNext() throws Throwable {
        ResourceRecordSetList result = new SAXDecoder<ResourceRecordSetList>(
                new Provider<ListResourceRecordSetsResponseHandler>() {
                    public ListResourceRecordSetsResponseHandler get() {
                        return new ListResourceRecordSetsResponseHandler();
                    }
                }) {
        }.decode(new InputStreamReader(getClass().getResourceAsStream("/advanced_rrsets.xml")),
                ResourceRecordSetList.class);

        assertEquals(result.size(), 2);
        assertEquals(result.get(0), ResourceRecordSet.<AData> builder()//
                .name("apple.myzone.com.")//
                .type("A")//
                .qualifier("foobar").ttl(300)//
                .weighted(Weighted.create(1)).add(AData.create("1.2.3.4")).build());

        // alias has no rdata!
        assertEquals(result.get(1), ResourceRecordSet.<AData> builder()//
                .name("fooo.myzone.com.")//
                .type("A")//
                .build());
        assertNull(result.next);
    }
}