package denominator.route53;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.InputStreamReader;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Weighted;
import denominator.model.rdata.AData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.SOAData;
import denominator.route53.Route53.ResourceRecordSetList;
import denominator.route53.Route53.ZoneList;
import denominator.route53.Route53Provider.Route53Decoder;
import feign.Response;

public class Route53DecoderTest {
    Route53Decoder decoder = new Route53Decoder();

    @Test
    public void decodeZoneListWithNext() throws Exception {
        Response response = Response.create(200, "OK", ImmutableListMultimap.<String, String> of(),
                new InputStreamReader(getClass().getResourceAsStream("/hosted_zones.xml")), null);
        ZoneList result = (ZoneList) decoder.decode(null, response, TypeToken.of(ZoneList.class));

        assertEquals(result.zones.size(), 2);
        assertEquals(result.zones.get(0), Zone.create("example.com.", "Z21DW1QVGID6NG"));
        assertEquals(result.zones.get(1), Zone.create("example2.com.", "Z2682N5HXP0BZ4"));
        assertEquals(result.next, "Z333333YYYYYYY");
    }

    @Test
    public void decodeBasicResourceRecordSetListWithNext() throws Exception {
        Response response = Response.create(200, "OK", ImmutableListMultimap.<String, String> of(),
                new InputStreamReader(getClass().getResourceAsStream("/basic_rrsets.xml")), null);
        ResourceRecordSetList result = (ResourceRecordSetList) decoder.decode(null, response,
                TypeToken.of(ResourceRecordSetList.class));

        assertEquals(result.rrsets.size(), 2);
        assertEquals(result.rrsets.get(0), ResourceRecordSet.<SOAData> builder()//
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
        assertEquals(result.rrsets.get(1), ResourceRecordSet.<NSData> builder()//
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
    public void decodeAdvancedResourceRecordSetListWithoutNext() throws Exception {
        Response response = Response.create(200, "OK", ImmutableListMultimap.<String, String> of(),
                new InputStreamReader(getClass().getResourceAsStream("/advanced_rrsets.xml")), null);
        ResourceRecordSetList result = (ResourceRecordSetList) decoder.decode(null, response,
                TypeToken.of(ResourceRecordSetList.class));

        assertEquals(result.rrsets.size(), 2);
        assertEquals(result.rrsets.get(0), ResourceRecordSet.<AData> builder()//
                .name("apple.myzone.com.")//
                .type("A")//
                .qualifier("foobar")
                .ttl(300)//
                .addProfile(Weighted.create(1))
                .add(AData.create("1.2.3.4")).build());

        // alias has no rdata!
        assertEquals(result.rrsets.get(1), ResourceRecordSet.<AData> builder()//
                .name("fooo.myzone.com.")//
                .type("A")//
                .addProfile(ImmutableMap.<String, Object> builder()//
                        .put("type", "alias")//
                        .put("HostedZoneId", "Z3I0BTR7N27QRM")//
                        .put("DNSName", "ipv4-route53recordsetlivetest.adrianc.myzone.com.")//
                        .put("EvaluateTargetHealth", "false").build()).build());
        assertNull(result.next);
    }
}