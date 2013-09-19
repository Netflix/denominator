package denominator.route53;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;

import org.testng.annotations.Test;

import dagger.ObjectGraph;
import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Weighted;
import denominator.model.rdata.AData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.SOAData;
import denominator.route53.Route53.ResourceRecordSetList;
import denominator.route53.Route53.ZoneList;
import denominator.route53.Route53Provider.XMLCodec;
import feign.Response;
import feign.codec.Decoder;

public class Route53DecoderTest {

    Decoder decoder = ObjectGraph.create(new XMLCodec()).get(Decoder.class);

    @Test
    public void decodeZoneListWithNext() throws Exception {
        ZoneList result = ZoneList.class.cast(decoder.decode(response("/hosted_zones.xml"), ZoneList.class));

        assertEquals(result.size(), 2);
        assertEquals(result.get(0), Zone.create("example.com.", "Z21DW1QVGID6NG"));
        assertEquals(result.get(1), Zone.create("example2.com.", "Z2682N5HXP0BZ4"));
        assertEquals(result.next, "Z333333YYYYYYY");
    }

    @Test
    public void decodeBasicResourceRecordSetListWithNext() throws Exception {
        ResourceRecordSetList result = ResourceRecordSetList.class.cast(decoder.decode(response("/basic_rrsets.xml"),
                ResourceRecordSetList.class));

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
    public void decodeAdvancedResourceRecordSetListWithoutNext() throws Exception {
        ResourceRecordSetList result = ResourceRecordSetList.class.cast(decoder.decode(response("/advanced_rrsets.xml"),
                ResourceRecordSetList.class));

        assertEquals(result.size(), 2);
        assertEquals(result.get(0), ResourceRecordSet.<AData> builder()//
                .name("apple.myzone.com.")//
                .type("A")//
                .qualifier("foobar").ttl(300)//
                .weighted(Weighted.create(1)).add(AData.create("1.2.3.4")).build());

        assertEquals(result.get(1), ResourceRecordSet.<AliasTarget> builder()//
                .name("fooo.myzone.com.")//
                .type("A")//
                .add(AliasTarget.create("Z3I0BTR7N27QRM", "ipv4-route53recordsetlivetest.adrianc.myzone.com."))//
                .build());
        assertNull(result.next);
    }

    Response response(String resource) throws IOException {
        return Response.create(200, "OK", Collections.<String, Collection<String>> emptyMap(),
                Util.slurp(new InputStreamReader(getClass().getResourceAsStream(resource))));
    }
}
