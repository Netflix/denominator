package denominator.route53;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;

import denominator.model.ResourceRecordSet;
import denominator.model.profile.Weighted;
import denominator.model.rdata.CNAMEData;
import denominator.profile.WeightedResourceRecordSetApi;

import static denominator.assertj.ModelAssertions.assertThat;

public class Route53WeightedResourceRecordSetApiMockTest {

  @Rule
  public MockRoute53Server server = new MockRoute53Server();

  @Test
  public void iterateByNameWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(twoRecords));

    WeightedResourceRecordSetApi api = server.connect().api().weightedRecordSetsInZone("Z1PA6795");
    Iterator<ResourceRecordSet<?>> iterator = api.iterateByName("www.denominator.io.");
    assertThat(iterator.next()).isEqualTo(rrset1);

    assertThat(iterator.next())
        .hasName("www.denominator.io.")
        .hasType("CNAME")
        .hasQualifier("MyService-West")
        .hasTtl(0)
        .hasWeight(5)
        .containsExactlyRecords(CNAMEData.create("www2.denominator.io."));

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795/rrset?name=www.denominator.io.");
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(noRecords));

    WeightedResourceRecordSetApi api = server.connect().api().weightedRecordSetsInZone("Z1PA6795");
    assertThat(api.iterateByName("www.denominator.io.")).isEmpty();

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795/rrset?name=www.denominator.io.");
  }

  @Test
  public void iterateByNameAndTypeWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(twoRecords));

    WeightedResourceRecordSetApi api = server.connect().api().weightedRecordSetsInZone("Z1PA6795");
    assertThat(api.iterateByNameAndType("www.denominator.io.", "CNAME")).contains(rrset1);

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795/rrset?name=www.denominator.io.&type=CNAME");
  }

  @Test
  public void iterateByNameAndTypeWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(noRecords));

    WeightedResourceRecordSetApi api = server.connect().api().weightedRecordSetsInZone("Z1PA6795");
    assertThat(api.iterateByNameAndType("www.denominator.io.", "CNAME")).isEmpty();

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795/rrset?name=www.denominator.io.&type=CNAME");
  }

  @Test
  public void getByNameTypeAndQualifierWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(twoRecords));

    WeightedResourceRecordSetApi api = server.connect().api().weightedRecordSetsInZone("Z1PA6795");
    assertThat(api.getByNameTypeAndQualifier("www.denominator.io.", "CNAME", "MyService-East"))
        .isEqualTo(rrset1);

    server.assertRequest()
        .hasPath(
            "/2012-12-12/hostedzone/Z1PA6795/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-East");
  }

  @Test
  public void getByNameTypeAndQualifierWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(noRecords));

    WeightedResourceRecordSetApi api = server.connect().api().weightedRecordSetsInZone("Z1PA6795");
    assertThat(api.getByNameTypeAndQualifier("www.denominator.io.", "CNAME", "MyService-East"))
        .isNull();

    server.assertRequest()
        .hasPath(
            "/2012-12-12/hostedzone/Z1PA6795/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-East");
  }

  @Test
  public void putRecordSet() throws Exception {
    server.enqueue(new MockResponse().setBody(noRecords));
    server.enqueue(new MockResponse().setBody(changeSynced));

    WeightedResourceRecordSetApi api = server.connect().api().weightedRecordSetsInZone("Z1PA6795");
    api.put(rrset1);

    server.assertRequest()
        .hasPath(
            "/2012-12-12/hostedzone/Z1PA6795/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-East");

    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795/rrset")
        .hasXMLBody(
            "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
            + "  <ChangeBatch>\n"
            + "    <Changes>\n"
            + "      <Change>\n"
            + "        <Action>CREATE</Action>\n"
            + "        <ResourceRecordSet>\n"
            + "          <Name>www.denominator.io.</Name>\n"
            + "          <Type>CNAME</Type>\n"
            + "          <SetIdentifier>MyService-East</SetIdentifier>\n"
            + "          <Weight>1</Weight>\n"
            + "          <TTL>0</TTL>\n"
            + "          <ResourceRecords>\n"
            + "            <ResourceRecord>\n"
            + "              <Value>www1.denominator.io.</Value>\n"
            + "            </ResourceRecord>\n"
            + "          </ResourceRecords>\n"
            + "        </ResourceRecordSet>\n"
            + "      </Change>\n"
            + "    </Changes>\n"
            + "  </ChangeBatch>\n"
            + "</ChangeResourceRecordSetsRequest>");
  }

  @Test
  public void putRecordSetSkipsWhenEqual() throws Exception {
    server.enqueue(new MockResponse().setBody(oneRecord));

    WeightedResourceRecordSetApi api = server.connect().api().weightedRecordSetsInZone("Z1PA6795");
    api.put(rrset1);

    server.assertRequest()
        .hasPath(
            "/2012-12-12/hostedzone/Z1PA6795/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-East");
  }

  @Test
  public void deleteDoesntAffectOtherQualifiers() throws Exception {
    server.enqueue(new MockResponse().setBody(twoRecords));
    server.enqueue(new MockResponse().setBody(changeSynced));

    WeightedResourceRecordSetApi api = server.connect().api().weightedRecordSetsInZone("Z1PA6795");

    api.deleteByNameTypeAndQualifier("www.denominator.io.", "CNAME", "MyService-East");

    server.assertRequest()
        .hasPath(
            "/2012-12-12/hostedzone/Z1PA6795/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-East");

    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795/rrset")
        .hasXMLBody(
            "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
            + "  <ChangeBatch>\n"
            + "    <Changes>\n"
            + "      <Change>\n"
            + "        <Action>DELETE</Action>\n"
            + "        <ResourceRecordSet>\n"
            + "          <Name>www.denominator.io.</Name>\n"
            + "          <Type>CNAME</Type>\n"
            + "          <SetIdentifier>MyService-East</SetIdentifier>\n"
            + "          <Weight>1</Weight>\n"
            + "          <TTL>0</TTL>\n"
            + "          <ResourceRecords>\n"
            + "            <ResourceRecord>\n"
            + "              <Value>www1.denominator.io.</Value>\n"
            + "            </ResourceRecord>\n"
            + "          </ResourceRecords>\n"
            + "        </ResourceRecordSet>\n"
            + "      </Change>\n"
            + "    </Changes>\n"
            + "  </ChangeBatch>\n"
            + "</ChangeResourceRecordSetsRequest>");
  }

  @Test
  public void deleteAbsentRRSDoesNothing() throws Exception {
    server.enqueue(new MockResponse().setBody(oneRecord));

    WeightedResourceRecordSetApi api = server.connect().api().weightedRecordSetsInZone("Z1PA6795");
    api.deleteByNameTypeAndQualifier("www.denominator.io.", "CNAME", "MyService-West");

    server.assertRequest()
        .hasPath(
            "/2012-12-12/hostedzone/Z1PA6795/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-West");
  }

  private String
      noRecords =
      "<ListResourceRecordSetsResponse><ResourceRecordSets></ResourceRecordSets></ListResourceRecordSetsResponse>";
  private String
      oneRecord =
      "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>MyService-East</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www1.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";
  private String
      twoRecords =
      "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>MyService-East</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www1.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>MyService-West</SetIdentifier><Weight>5</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www2.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";

  private String
      changeSynced =
      "<GetChangeResponse><ChangeInfo><Id>/change/C2682N5HXP0BZ4</Id><Status>INSYNC</Status><SubmittedAt>2011-09-10T01:36:41.958Z</SubmittedAt></ChangeInfo></GetChangeResponse>";

  private ResourceRecordSet<CNAMEData> rrset1 = ResourceRecordSet.<CNAMEData>builder()
      .name("www.denominator.io.")
      .type("CNAME")
      .qualifier("MyService-East")
      .ttl(0)
      .weighted(Weighted.create(1))
      .add(CNAMEData.create("www1.denominator.io.")).build();
}
