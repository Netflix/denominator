package denominator.route53;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

import org.testng.annotations.Test;

import java.io.IOException;

import denominator.Denominator;
import denominator.ResourceRecordSetApi;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.model.ResourceRecordSets.a;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

@Test(singleThreaded = true)
public class Route53ResourceRecordSetApiMockTest {

  String
      weightedRecords =
      "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>Route53Service:us-east-1:PLATFORMSERVICE:i-7f0aec0d:20130313205017</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www1.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>Route53Service:us-east-1:PLATFORMSERVICE:i-fbe41089:20130312203418</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www2.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";
  String
      noRecords =
      "<ListResourceRecordSetsResponse><ResourceRecordSets></ResourceRecordSets></ListResourceRecordSetsResponse>";
  String
      createARecordSet =
      "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch><Changes><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";
  String
      changeSynced =
      "<GetChangeResponse><ChangeInfo><Id>/change/C2682N5HXP0BZ4</Id><Status>INSYNC</Status><SubmittedAt>2011-09-10T01:36:41.958Z</SubmittedAt></ChangeInfo></GetChangeResponse>";
  String
      oneRecord =
      "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";
  String
      replaceWith2ElementRecordSet =
      "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>10000000</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";
  String
      twoRecords =
      "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";
  String
      replaceWith1ElementRecordSet =
      "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";
  String
      delete2ElementRecordSet =
      "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";

  private static ResourceRecordSetApi mockApi(final int port) {
    return Denominator.create(new Route53Provider() {
      @Override
      public String url() {
        return "http://localhost:" + port;
      }
    }, credentials("accessKey", "secretKey")).api().basicRecordSetsInZone("Z1PA6795UKMFR9");
  }

  @Test
  public void weightedRecordSetsAreFilteredOut() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(weightedRecords));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertNull(api.getByNameAndType("www.denominator.io.", "CNAME"));

      assertEquals(server.getRequestCount(), 1);

      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=CNAME HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putFirstRecordCreatesNewRRSet() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
    server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

      assertEquals(server.getRequestCount(), 2);

      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A HTTP/1.1");

      RecordedRequest createRRSet = server.takeRequest();
      assertEquals(createRRSet.getRequestLine(),
                   "POST /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
      assertEquals(createRRSet.getHeader("Content-Type"), "application/xml");
      assertEquals(new String(createRRSet.getBody()), createARecordSet);
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putSameRecordNoOp() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

      assertEquals(server.getRequestCount(), 1);

      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putRecreatesWhenPresent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
    server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(a("www.denominator.io.", 10000000, ImmutableSet.of("192.0.2.1", "198.51.100.1")));

      assertEquals(server.getRequestCount(), 2);

      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A HTTP/1.1");

      RecordedRequest createRRSet = server.takeRequest();
      assertEquals(createRRSet.getRequestLine(),
                   "POST /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
      assertEquals(new String(createRRSet.getBody()), replaceWith2ElementRecordSet);
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void putOneRecordReplacesRRSet() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
    server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

      assertEquals(server.getRequestCount(), 2);

      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A HTTP/1.1");

      RecordedRequest createRRSet = server.takeRequest();
      assertEquals(createRRSet.getRequestLine(),
                   "POST /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
      assertEquals(new String(createRRSet.getBody()), replaceWith1ElementRecordSet);
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void iterateByNameWhenPresent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertEquals(api.iterateByName("www.denominator.io.").next(),
                   a("www.denominator.io.", 3600, ImmutableList.of("192.0.2.1", "198.51.100.1")));

      assertEquals(server.getRequestCount(), 1);

      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io. HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void iterateByNameWhenAbsent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertFalse(api.iterateByName("www.denominator.io.").hasNext());

      assertEquals(server.getRequestCount(), 1);

      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io. HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void getByNameAndTypeWhenPresent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertEquals(api.getByNameAndType("www.denominator.io.", "A"),
                   a("www.denominator.io.", 3600, ImmutableList.of("192.0.2.1", "198.51.100.1")));

      assertEquals(server.getRequestCount(), 1);

      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void getByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      assertNull(api.getByNameAndType("www.denominator.io.", "A"));

      assertEquals(server.getRequestCount(), 1);

      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void deleteRRSet() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
    server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.deleteByNameAndType("www.denominator.io.", "A");

      assertEquals(server.getRequestCount(), 2);

      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=A HTTP/1.1");

      RecordedRequest createRRSet = server.takeRequest();
      assertEquals(createRRSet.getRequestLine(),
                   "POST /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
      assertEquals(new String(createRRSet.getBody()), delete2ElementRecordSet);
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void deleteAbsentRRSDoesNothing() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
    server.play();

    try {
      ResourceRecordSetApi api = mockApi(server.getPort());
      api.deleteByNameAndType("www1.denominator.io.", "A");

      assertEquals(server.getRequestCount(), 1);

      assertEquals(server.takeRequest().getRequestLine(),
                   "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www1.denominator.io.&type=A HTTP/1.1");
    } finally {
      server.shutdown();
    }
  }
}
