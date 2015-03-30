package denominator.route53;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;

public class Route53ZoneApiMockTest {

  @Rule
  public MockRoute53Server server = new MockRoute53Server();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "<ListHostedZonesResponse>\n"
        + "  <HostedZones>\n"
        + "    <HostedZone>\n"
        + "      <Id>/hostedzone/Z1PA6795UKMFR9</Id>\n"
        + "      <Name>denominator.io.</Name>\n"
        + "      <CallerReference>denomination</CallerReference>\n"
        + "      <Config>\n"
        + "        <Comment>no comment</Comment>\n"
        + "      </Config>\n"
        + "      <ResourceRecordSetCount>17</ResourceRecordSetCount>\n"
        + "    </HostedZone>\n"
        + "  </HostedZones>\n"
        + "</ListHostedZonesResponse>"));
    server.enqueue(new MockResponse().setBody(
        "<?xml version=\"1.0\"?>\n"
        + "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <ResourceRecordSets>\n"
        + "    <ResourceRecordSet>\n"
        + "      <Name>denominator.io.</Name>\n"
        + "      <Type>SOA</Type>\n"
        + "      <TTL>900</TTL>\n"
        + "      <ResourceRecords>\n"
        + "        <ResourceRecord>\n"
        + "          <Value>ns-273.awsdns-34.com. awsdns-hostmaster.amazon.com. 1 7200 900 1209600 86400</Value>\n"
        + "        </ResourceRecord>\n"
        + "      </ResourceRecords>\n"
        + "    </ResourceRecordSet>\n"
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>100</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));

    ZoneApi api = server.connect().api().zones();
    Iterator<Zone> domains = api.iterator();

    assertThat(domains.next())
        .hasName("denominator.io.")
        .hasId("Z1PA6795UKMFR9")
        .hasEmail("awsdns-hostmaster.amazon.com.");

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=denominator.io.&type=SOA");
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "<ListHostedZonesResponse><HostedZones /></ListHostedZonesResponse>"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterator()).isEmpty();

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone");
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "<ListHostedZonesByNameResponse xmlns=\"https://route53.amazonaws.com/doc/2013-04-01/\">\n"
        + "  <HostedZones>\n"
        + "    <HostedZone>\n"
        + "      <Id>/hostedzone/Z2ZEEJCUZCVG56</Id>\n"
        + "      <Name>denominator.io.</Name>\n"
        + "      <CallerReference>Foo</CallerReference>\n"
        + "      <Config>\n"
        + "        <PrivateZone>false</PrivateZone>\n"
        + "      </Config>\n"
        + "      <ResourceRecordSetCount>3</ResourceRecordSetCount>\n"
        + "    </HostedZone>\n"
        + "    <HostedZone>\n"
        + "      <Id>/hostedzone/Z3OQLQGABCU3T</Id>\n"
        + "      <Name>denominator.io.</Name>\n"
        + "      <CallerReference>Bar</CallerReference>\n"
        + "      <Config>\n"
        + "        <PrivateZone>false</PrivateZone>\n"
        + "      </Config>\n"
        + "      <ResourceRecordSetCount>2</ResourceRecordSetCount>\n"
        + "    </HostedZone>\n"
        + "  </HostedZones>\n"
        + "  <DNSName>denominator.io.</DNSName>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>100</MaxItems>\n"
        + "</ListHostedZonesByNameResponse>"));
    server.enqueue(new MockResponse().setBody(
        "<?xml version=\"1.0\"?>\n"
        + "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <ResourceRecordSets>\n"
        + "    <ResourceRecordSet>\n"
        + "      <Name>denominator.io.</Name>\n"
        + "      <Type>SOA</Type>\n"
        + "      <TTL>900</TTL>\n"
        + "      <ResourceRecords>\n"
        + "        <ResourceRecord>\n"
        + "          <Value>ns-273.awsdns-34.com. awsdns-hostmaster.amazon.com. 1 7200 900 1209600 86400</Value>\n"
        + "        </ResourceRecord>\n"
        + "      </ResourceRecords>\n"
        + "    </ResourceRecordSet>\n"
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>100</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));
    server.enqueue(new MockResponse().setBody(
        "<?xml version=\"1.0\"?>\n"
        + "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <ResourceRecordSets>\n"
        + "    <ResourceRecordSet>\n"
        + "      <Name>denominator.io.</Name>\n"
        + "      <Type>SOA</Type>\n"
        + "      <TTL>900</TTL>\n"
        + "      <ResourceRecords>\n"
        + "        <ResourceRecord>\n"
        + "          <Value>ns-273.awsdns-35.com. awsdns-hostmaster.amazon.com. 1 7200 900 1209600 86400</Value>\n"
        + "        </ResourceRecord>\n"
        + "      </ResourceRecords>\n"
        + "    </ResourceRecordSet>\n"
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>100</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io.")).containsExactly(
        Zone.create("denominator.io.", "Z2ZEEJCUZCVG56", "awsdns-hostmaster.amazon.com."),
        Zone.create("denominator.io.", "Z3OQLQGABCU3T", "awsdns-hostmaster.amazon.com.")
    );

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2013-04-01/hostedzonesbyname?dnsname=denominator.io.");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone/Z2ZEEJCUZCVG56/rrset?name=denominator.io.&type=SOA");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone/Z3OQLQGABCU3T/rrset?name=denominator.io.&type=SOA");
  }

  /**
   * Route53 list by name is only about order. We need to ensure we skip irrelevant zones.
   */
  @Test
  public void iterateByNameWhenIrrelevant() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "<ListHostedZonesByNameResponse xmlns=\"https://route53.amazonaws.com/doc/2013-04-01/\">\n"
        + "  <HostedZones>\n"
        + "    <HostedZone>\n"
        + "      <Id>/hostedzone/Z2ZEEJCUZCVG56</Id>\n"
        + "      <Name>denominator.com.</Name>\n"
        + "      <CallerReference>Foo</CallerReference>\n"
        + "      <Config>\n"
        + "        <PrivateZone>false</PrivateZone>\n"
        + "      </Config>\n"
        + "      <ResourceRecordSetCount>3</ResourceRecordSetCount>\n"
        + "    </HostedZone>\n"
        + "  </HostedZones>\n"
        + "  <DNSName>denominator.io.</DNSName>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>100</MaxItems>\n"
        + "</ListHostedZonesByNameResponse>"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io.")).isEmpty();

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2013-04-01/hostedzonesbyname?dnsname=denominator.io.");
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "<ListHostedZonesByNameResponse><HostedZones /></ListHostedZonesByNameResponse>"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io.")).isEmpty();

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2013-04-01/hostedzonesbyname?dnsname=denominator.io.");
  }
}
