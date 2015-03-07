package denominator.route53;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

import denominator.ResourceRecordSetApi;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.model.ResourceRecordSets.a;

public class Route53ResourceRecordSetApiMockTest {

  @Rule
  public MockRoute53Server server = new MockRoute53Server();

  @Test
  public void weightedRecordSetsAreFilteredOut() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>Route53Service:us-east-1:PLATFORMSERVICE:i-7f0aec0d:20130313205017</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www1.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>Route53Service:us-east-1:PLATFORMSERVICE:i-fbe41089:20130312203418</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www2.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>"));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    assertThat(api.getByNameAndType("www.denominator.io.", "CNAME")).isNull();

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=CNAME");
  }

  @Test
  public void putFirstRecordCreatesNewRRSet() throws Exception {
    server.enqueue(new MockResponse().setBody(noRecords));
    server.enqueue(new MockResponse().setBody(changeSynced));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset")
        .hasXMLBody(
            "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch><Changes><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>");
  }

  @Test
  public void putSameRecordNoOp() throws Exception {
    server.enqueue(new MockResponse().setBody(oneRecord));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A");
  }

  @Test
  public void putRecreates_ttlChanged() throws Exception {
    server.enqueue(new MockResponse().setBody(oneRecord));
    server.enqueue(new MockResponse().setBody(changeSynced));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    api.put(a("www.denominator.io.", 10000000, Arrays.asList("192.0.2.1")));

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset")
        .hasXMLBody(
            "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>10000000</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>");
  }

  @Test
  public void putRecreates_recordAdded() throws Exception {
    server.enqueue(new MockResponse().setBody(oneRecord));
    server.enqueue(new MockResponse().setBody(changeSynced));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    api.put(a("www.denominator.io.", 3600, Arrays.asList("192.0.2.1", "198.51.100.1")));

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset")
        .hasXMLBody(
            "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>");
  }

  @Test
  public void putOneRecordReplacesRRSet() throws Exception {
    server.enqueue(new MockResponse().setBody(twoRecords));
    server.enqueue(new MockResponse().setBody(changeSynced));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset")
        .hasXMLBody(
            "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>");
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(twoRecords));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    assertThat(api.iterateByName("www.denominator.io."))
        .contains(a("www.denominator.io.", 3600, Arrays.asList("192.0.2.1", "198.51.100.1")));

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.");
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(noRecords));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    assertThat(api.iterateByName("www.denominator.io.")).isEmpty();

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.");
  }

  @Test
  public void getByNameAndTypeWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(twoRecords));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    assertThat(api.getByNameAndType("www.denominator.io.", "A"))
        .isEqualTo(a("www.denominator.io.", 3600, Arrays.asList("192.0.2.1", "198.51.100.1")));

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A");
  }

  @Test
  public void getByNameAndTypeWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(noRecords));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    assertThat(api.getByNameAndType("www.denominator.io.", "A")).isNull();

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A");
  }

  @Test
  public void deleteRRSet() throws Exception {
    server.enqueue(new MockResponse().setBody(twoRecords));
    server.enqueue(new MockResponse().setBody(changeSynced));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    api.deleteByNameAndType("www.denominator.io.", "A");

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A");
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset")
        .hasXMLBody(
            "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>");
  }

  @Test
  public void deleteAbsentRRSDoesNothing() throws Exception {
    server.enqueue(new MockResponse().setBody(oneRecord));

    ResourceRecordSetApi api = server.connect().api().basicRecordSetsInZone("Z1PA6795UKMFR9");
    api.deleteByNameAndType("www1.denominator.io.", "A");

    server.assertRequest()
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www1.denominator.io.&type=A");
  }

  String
      noRecords =
      "<ListResourceRecordSetsResponse><ResourceRecordSets></ResourceRecordSets></ListResourceRecordSetsResponse>";
  String
      changeSynced =
      "<GetChangeResponse><ChangeInfo><Id>/change/C2682N5HXP0BZ4</Id><Status>INSYNC</Status><SubmittedAt>2011-09-10T01:36:41.958Z</SubmittedAt></ChangeInfo></GetChangeResponse>";
  String
      oneRecord =
      "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";
  String
      twoRecords =
      "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";
}
