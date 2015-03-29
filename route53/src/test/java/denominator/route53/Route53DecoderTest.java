package denominator.route53;

import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import denominator.model.rdata.AData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.SOAData;
import denominator.route53.Route53.HostedZoneList;
import denominator.route53.Route53.ResourceRecordSetList;
import feign.Response;
import feign.codec.Decoder;

import static denominator.assertj.ModelAssertions.assertThat;
import static feign.Util.UTF_8;
import static org.assertj.core.api.Assertions.tuple;

public class Route53DecoderTest {

  Decoder decoder = new Route53Provider.FeignModule().decoder();

  @Test
  public void decodeZoneListWithNext() throws Exception {
    HostedZoneList result = (HostedZoneList) decoder.decode(response(
        "<ListHostedZonesResponse xmlns=\"https://route53.amazonaws.com/doc/2012-02-29/\">\n"
        + "  <HostedZones>\n"
        + "    <HostedZone>\n"
        + "      <Id>/hostedzone/Z21DW1QVGID6NG</Id>\n"
        + "      <Name>example.com.</Name>\n"
        + "      <CallerReference>a_unique_reference</CallerReference>\n"
        + "      <Config>\n"
        + "        <Comment>Migrate an existing domain to Route 53</Comment>\n"
        + "      </Config>\n"
        + "      <ResourceRecordSetCount>17</ResourceRecordSetCount>\n"
        + "    </HostedZone>\n"
        + "    <HostedZone>\n"
        + "      <Id>/hostedzone/Z2682N5HXP0BZ4</Id>\n"
        + "      <Name>example2.com.</Name>\n"
        + "      <CallerReference>a_unique_reference2</CallerReference>\n"
        + "      <Config>\n"
        + "        <Comment>This is my 2nd hosted zone.</Comment>\n"
        + "      </Config>\n"
        + "      <ResourceRecordSetCount>117</ResourceRecordSetCount>\n"
        + "    </HostedZone>\n"
        + "  </HostedZones>\n"
        + "  <Marker>Z222222VVVVVVV</Marker>\n"
        + "  <IsTruncated>true</IsTruncated>\n"
        + "  <NextMarker>Z333333YYYYYYY</NextMarker>\n"
        + "  <MaxItems>10</MaxItems>\n"
        + "</ListHostedZonesResponse>"), HostedZoneList.class);

    assertThat(result).extracting("name", "id").containsExactly(
        tuple("example.com.", "Z21DW1QVGID6NG"),
        tuple("example2.com.", "Z2682N5HXP0BZ4")
    );

    assertThat(result.next).isEqualTo("Z333333YYYYYYY");
  }

  @Test
  public void decodeBasicResourceRecordSetListWithNext() throws Exception {
    ResourceRecordSetList result = (ResourceRecordSetList) decoder.decode(response(
                                                                              "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-02-29/\">\n"
                                                                              + "  <ResourceRecordSets>\n"
                                                                              + "    <ResourceRecordSet>\n"
                                                                              + "      <Name>example.com.</Name>\n"
                                                                              + "      <Type>SOA</Type>\n"
                                                                              + "      <TTL>900</TTL>\n"
                                                                              + "      <ResourceRecords>\n"
                                                                              + "        <ResourceRecord>\n"
                                                                              + "          <Value>ns-2048.awsdns-64.net. hostmaster.awsdns.com. 1 7200 900 1209600 86400</Value>\n"
                                                                              + "        </ResourceRecord>\n"
                                                                              + "      </ResourceRecords>\n"
                                                                              + "    </ResourceRecordSet>\n"
                                                                              + "    <ResourceRecordSet>\n"
                                                                              + "      <Name>example.com.</Name>\n"
                                                                              + "      <Type>NS</Type>\n"
                                                                              + "      <TTL>172800</TTL>\n"
                                                                              + "      <ResourceRecords>\n"
                                                                              + "        <ResourceRecord>\n"
                                                                              + "          <Value>ns-2048.awsdns-64.com.</Value>\n"
                                                                              + "        </ResourceRecord>\n"
                                                                              + "        <ResourceRecord>\n"
                                                                              + "          <Value>ns-2049.awsdns-65.net.</Value>\n"
                                                                              + "        </ResourceRecord>\n"
                                                                              + "        <ResourceRecord>\n"
                                                                              + "          <Value>ns-2050.awsdns-66.org.</Value>\n"
                                                                              + "        </ResourceRecord>\n"
                                                                              + "        <ResourceRecord>\n"
                                                                              + "          <Value>ns-2051.awsdns-67.co.uk.</Value>\n"
                                                                              + "        </ResourceRecord>\n"
                                                                              + "      </ResourceRecords>\n"
                                                                              + "    </ResourceRecordSet>\n"
                                                                              + "  </ResourceRecordSets>\n"
                                                                              + "  <IsTruncated>true</IsTruncated>\n"
                                                                              + "  <MaxItems>3</MaxItems>\n"
                                                                              + "  <NextRecordName>testdoc2.example.com</NextRecordName>\n"
                                                                              + "  <NextRecordType>NS</NextRecordType>\n"
                                                                              + "</ListResourceRecordSetsResponse>"),
                                                                          ResourceRecordSetList.class);

    assertThat(result.get(0))
        .hasName("example.com.")
        .hasType("SOA")
        .hasTtl(900)
        .containsExactlyRecords(SOAData.builder()
                                 .mname("ns-2048.awsdns-64.net.")
                                 .rname("hostmaster.awsdns.com.")
                                 .serial(1)
                                 .refresh(7200)
                                 .retry(900)
                                 .expire(1209600)
                                 .minimum(86400)
                                 .build());

    assertThat(result.get(1))
        .hasName("example.com.")
        .hasType("NS")
        .hasTtl(172800)
        .containsExactlyRecords(NSData.create("ns-2048.awsdns-64.com."),
                             NSData.create("ns-2049.awsdns-65.net."),
                             NSData.create("ns-2050.awsdns-66.org."),
                             NSData.create("ns-2051.awsdns-67.co.uk."));

    assertThat(result.next.name).isEqualTo("testdoc2.example.com");
    assertThat(result.next.type).isEqualTo("NS");
    assertThat(result.next.identifier).isNull();
  }

  @Test
  public void decodeAdvancedResourceRecordSetListWithoutNext() throws Exception {
    ResourceRecordSetList
        result =
        (ResourceRecordSetList) decoder.decode(response(
                                                   "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
                                                   + "  <ResourceRecordSets>\n"
                                                   + "    <ResourceRecordSet>\n"
                                                   + "      <Name>apple.myzone.com.</Name>\n"
                                                   + "      <Type>A</Type>\n"
                                                   + "      <SetIdentifier>foobar</SetIdentifier>\n"
                                                   + "      <Weight>1</Weight>\n"
                                                   + "      <TTL>300</TTL>\n"
                                                   + "      <ResourceRecords>\n"
                                                   + "        <ResourceRecord>\n"
                                                   + "          <Value>1.2.3.4</Value>\n"
                                                   + "        </ResourceRecord>\n"
                                                   + "      </ResourceRecords>\n"
                                                   + "    </ResourceRecordSet>\n"
                                                   + "    <ResourceRecordSet>\n"
                                                   + "      <Name>fooo.myzone.com.</Name>\n"
                                                   + "      <Type>A</Type>\n"
                                                   + "      <AliasTarget>\n"
                                                   + "        <HostedZoneId>Z3I0BTR7N27QRM</HostedZoneId>\n"
                                                   + "        <DNSName>ipv4-route53recordsetlivetest.adrianc.myzone.com.</DNSName>\n"
                                                   + "        <EvaluateTargetHealth>false</EvaluateTargetHealth>\n"
                                                   + "      </AliasTarget>\n"
                                                   + "    </ResourceRecordSet>\n"
                                                   + "  </ResourceRecordSets>\n"
                                                   + "  <IsTruncated>false</IsTruncated>\n"
                                                   + "  <MaxItems>100</MaxItems>\n"
                                                   + "</ListResourceRecordSetsResponse>"),
                                               ResourceRecordSetList.class);

    assertThat(result.get(0))
        .hasName("apple.myzone.com.")
        .hasType("A")
        .hasQualifier("foobar")
        .hasTtl(300)
        .hasWeight(1)
        .containsExactlyRecords(AData.create("1.2.3.4"));

    assertThat(result.get(1))
        .hasName("fooo.myzone.com.")
        .hasType("A")
        .containsExactlyRecords(AliasTarget.create("Z3I0BTR7N27QRM",
                                                "ipv4-route53recordsetlivetest.adrianc.myzone.com."));
    assertThat(result.next).isNull();
  }

  private Response response(String xml) throws IOException {
    return Response
        .create(200, "OK", Collections.<String, Collection<String>>emptyMap(), xml, UTF_8);
  }
}
