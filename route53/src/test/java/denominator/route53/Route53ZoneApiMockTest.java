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
        + soaRRSet
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>100</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));

    ZoneApi api = server.connect().api().zones();
    Iterator<Zone> domains = api.iterator();

    assertThat(domains).containsExactly(
        Zone.create("Z1PA6795UKMFR9", "denominator.io.", 3601, "test@denominator.io")
    );

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
        + soaRRSet
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>100</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));
    server.enqueue(new MockResponse().setBody(
        "<?xml version=\"1.0\"?>\n"
        + "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <ResourceRecordSets>\n"
        + soaRRSet
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>100</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io.")).containsExactly(
        Zone.create("Z2ZEEJCUZCVG56", "denominator.io.", 3601, "test@denominator.io"),
        Zone.create("Z3OQLQGABCU3T", "denominator.io.", 3601, "test@denominator.io")
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
        + "      <Name>denominator.io.</Name>\n"
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
    assertThat(api.iterateByName("denominator.com.")).isEmpty();

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2013-04-01/hostedzonesbyname?dnsname=denominator.com.");
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

  @Test
  public void putWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "<CreateHostedZoneResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <HostedZone>\n"
        + "    <Id>/hostedzone/Z1PA6795UKMFR9</Id>\n"
        + "    <Name>denominator.io.</Name>\n"
        + "    <CallerReference>a228ebcc-0c93-4627-8fff-1b899f5de2a4</CallerReference>\n"
        + "    <Config/>\n"
        + "    <ResourceRecordSetCount>2</ResourceRecordSetCount>\n"
        + "  </HostedZone>\n"
        + "  <ChangeInfo>\n"
        + "    <Id>/change/C1DMRYCM7MK76K</Id>\n"
        + "    <Status>PENDING</Status>\n"
        + "    <SubmittedAt>2015-04-04T02:50:41.602Z</SubmittedAt>\n"
        + "  </ChangeInfo>\n"
        + "  <DelegationSet>\n"
        + "    <NameServers>\n"
        + "      <NameServer>ns-534.awsdns-02.net</NameServer>\n"
        + "      <NameServer>ns-448.awsdns-56.com</NameServer>\n"
        + "      <NameServer>ns-1296.awsdns-34.org</NameServer>\n"
        + "      <NameServer>ns-1725.awsdns-23.co.uk</NameServer>\n"
        + "    </NameServers>\n"
        + "  </DelegationSet>\n"
        + "</CreateHostedZoneResponse>"
    ));
    server.enqueue(new MockResponse().setBody(
        "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <ResourceRecordSets>\n"
        + initialSOA
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>100</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));
    server.enqueue(changingRRSets);

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create(null, "denominator.io.", 3601, "test@denominator.io");
    assertThat(api.put(zone)).isEqualTo("Z1PA6795UKMFR9");

    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=denominator.io.&type=SOA");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset")
        .hasXMLBody(
            "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
            + "  <ChangeBatch>\n"
            + "    <Changes>\n"
            + "      <Change>\n"
            + "        <Action>DELETE</Action>\n"
            + initialSOA
            + "      </Change>\n"
            + "      <Change>\n"
            + "        <Action>CREATE</Action>\n"
            + soaRRSet
            + "      </Change>\n"
            + "    </Changes>\n"
            + "  </ChangeBatch>\n"
            + "</ChangeResourceRecordSetsRequest>");
  }

  @Test
  public void putWhenPresent_changingSOA() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <ResourceRecordSets>\n"
        + initialSOA
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>100</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));
    server.enqueue(changingRRSets);

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create("Z1PA6795UKMFR9", "denominator.io.", 3601, "test@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.id());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=denominator.io.&type=SOA");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset")
        .hasXMLBody(
            "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
            + "  <ChangeBatch>\n"
            + "    <Changes>\n"
            + "      <Change>\n"
            + "        <Action>DELETE</Action>\n"
            + initialSOA
            + "      </Change>\n"
            + "      <Change>\n"
            + "        <Action>CREATE</Action>\n"
            + soaRRSet
            + "      </Change>\n"
            + "    </Changes>\n"
            + "  </ChangeBatch>\n"
            + "</ChangeResourceRecordSetsRequest>");
  }

  @Test
  public void putWhenPresent_noop() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <ResourceRecordSets>\n"
        + soaRRSet
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>100</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create("Z1PA6795UKMFR9", "denominator.io.", 3601, "test@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.id());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=denominator.io.&type=SOA");
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server.enqueue(oneZone);
    server.enqueue(new MockResponse().setBody(
        "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <ResourceRecordSets>\n"
        + nsRRSet
        + soaRRSet
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>2</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));
    server.enqueue(deletingZone);

    ZoneApi api = server.connect().api().zones();
    api.delete("Z1PA6795UKMFR9");

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset");
    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9");
  }

  @Test
  public void deleteWhenPresent_extraRRSet() throws Exception {
    // Only rrset we expect to delete manually
    String aRecord = "    <ResourceRecordSet>\n"
                     + "      <Name>ns-google.denominator.io.</Name>\n"
                     + "      <Type>A</Type>\n"
                     + "      <TTL>300</TTL>\n"
                     + "      <ResourceRecords>\n"
                     + "        <ResourceRecord>\n"
                     + "          <Value>8.8.8.8</Value>\n"
                     + "        </ResourceRecord>\n"
                     + "      </ResourceRecords>\n"
                     + "    </ResourceRecordSet>\n";
    server.enqueue(oneZone);
    server.enqueue(new MockResponse().setBody(
        "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <ResourceRecordSets>\n"
        + nsRRSet
        + soaRRSet
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>true</IsTruncated>\n"
        + "  <NextRecordName>ns-google.denominator.io.</NextRecordName>\n"
        + "  <NextRecordType>A</NextRecordType>\n"
        + "  <MaxItems>2</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));
    server.enqueue(new MockResponse().setBody(
        "<ListResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <ResourceRecordSets>\n"
        + aRecord
        + "  </ResourceRecordSets>\n"
        + "  <IsTruncated>false</IsTruncated>\n"
        + "  <MaxItems>2</MaxItems>\n"
        + "</ListResourceRecordSetsResponse>"
    ));
    server.enqueue(changingRRSets);
    server.enqueue(deletingZone);

    ZoneApi api = server.connect().api().zones();
    api.delete("Z1PA6795UKMFR9");

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath(
            "/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=ns-google.denominator.io.&type=A");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset")
        .hasXMLBody(
            "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
            + "  <ChangeBatch>\n"
            + "    <Changes>\n"
            + "      <Change>\n"
            + "        <Action>DELETE</Action>\n"
            + aRecord
            + "      </Change>\n"
            + "    </Changes>\n"
            + "  </ChangeBatch>\n"
            + "</ChangeResourceRecordSetsRequest>");
    server.assertRequest()
        .hasMethod("DELETE")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9");
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(404).setBody(
        "<ErrorResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><Error><Type>Sender</Type><Code>NoSuchHostedZone</Code><Message>The specified hosted zone does not exist.</Message></Error><RequestId>d1862286-da13-11e4-a87a-f78bcee90724</RequestId></ErrorResponse>"));

    ZoneApi api = server.connect().api().zones();
    api.delete("Z1PA6795UKMFR9");

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9");
  }

  private String soaRRSet = "    <ResourceRecordSet>\n"
                            + "      <Name>denominator.io.</Name>\n"
                            + "      <Type>SOA</Type>\n"
                            + "      <TTL>3601</TTL>\n"
                            + "      <ResourceRecords>\n"
                            + "        <ResourceRecord>\n"
                            + "          <Value>ns-1612.awsdns-27.net. test@denominator.io 2 7200 3601 1209600 86400</Value>\n"
                            + "        </ResourceRecord>\n"
                            + "      </ResourceRecords>\n"
                            + "    </ResourceRecordSet>\n";
  private String initialSOA = // Initially SOA has a TTL of 900, an amazon rname and serial number 1
      soaRRSet.replaceFirst("3601", "900").replace("test@denominator.io 2",
                                                   "awsdns-hostmaster.amazon.com. 1");
  private String nsRRSet = "    <ResourceRecordSet>\n"
                           + "      <Name>denominator.io.</Name>\n"
                           + "      <Type>NS</Type>\n"
                           + "      <TTL>172800</TTL>\n"
                           + "      <ResourceRecords>\n"
                           + "        <ResourceRecord>\n"
                           + "          <Value>ns-1612.awsdns-09.co.uk.</Value>\n"
                           + "        </ResourceRecord>\n"
                           + "        <ResourceRecord>\n"
                           + "          <Value>ns-230.awsdns-28.com.</Value>\n"
                           + "        </ResourceRecord>\n"
                           + "        <ResourceRecord>\n"
                           + "          <Value>ns-993.awsdns-60.net.</Value>\n"
                           + "        </ResourceRecord>\n"
                           + "        <ResourceRecord>\n"
                           + "          <Value>ns-1398.awsdns-46.org.</Value>\n"
                           + "        </ResourceRecord>\n"
                           + "      </ResourceRecords>\n"
                           + "    </ResourceRecordSet>\n";
  private MockResponse oneZone = new MockResponse().setBody(
      "<ListHostedZonesResponse>\n"
      + "  <HostedZones>\n"
      + "    <HostedZone>\n"
      + "      <Id>/hostedzone/Z1PA6795UKMFR9</Id>\n"
      + "      <Name>denominator.io.</Name>\n"
      + "      <CallerReference>denomination</CallerReference>\n"
      + "      <Config>\n"
      + "        <Comment>no comment</Comment>\n"
      + "      </Config>\n"
      + "      <ResourceRecordSetCount>3</ResourceRecordSetCount>\n"
      + "    </HostedZone>\n"
      + "  </HostedZones>\n"
      + "</ListHostedZonesResponse>");
  private MockResponse changingRRSets = new MockResponse().setBody(
      "<ChangeResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeInfo><Id>/change/CWWCRAXTKEB72d</Id><Status>PENDING</Status><SubmittedAt>2015-04-03T14:41:54.371Z</SubmittedAt></ChangeInfo></ChangeResourceRecordSetsResponse>"
  );
  private MockResponse deletingZone = new MockResponse().setBody(
      "<DeleteHostedZoneResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeInfo><Id>/change/C1QB5QU6VYXUHE</Id><Status>PENDING</Status><SubmittedAt>2015-04-03T14:41:54.512Z</SubmittedAt></ChangeInfo></DeleteHostedZoneResponse>"
  );
}
