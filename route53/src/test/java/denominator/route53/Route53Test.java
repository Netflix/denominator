package denominator.route53;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import denominator.model.ResourceRecordSet;
import denominator.route53.Route53.ActionOnResourceRecordSet;
import feign.Feign;

import static denominator.model.ResourceRecordSets.a;
import static org.testng.Assert.assertEquals;

@Test(singleThreaded = true)
public class Route53Test {

  static String hostedZones = ""//
                              + "<ListHostedZonesResponse><HostedZones>"//
                              + "<HostedZone><Id>/hostedzone/Z1PA6795UKMFR9</Id><Name>denominator.io.</Name><CallerReference>denomination</CallerReference><Config><Comment>no comment</Comment></Config><ResourceRecordSetCount>17</ResourceRecordSetCount></HostedZone>"
//
                              + "</HostedZones></ListHostedZonesResponse>";

  static String noHostedZones = ""//
                                + "<ListHostedZonesResponse><HostedZones /></ListHostedZonesResponse>";

  static String invalidClientTokenId = ""//
                                       + "<?xml version=\"1.0\"?>\n"//
                                       + "<ErrorResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">"
//
                                       + "<Error>"//
                                       + "<Type>Sender</Type>"//
                                       + "<Code>InvalidClientTokenId</Code>"//
                                       + "<Message>The security token included in the request is invalid</Message>"
//
                                       + "</Error>"//
                                       + "<RequestId>d3801bc8-f70d-11e2-8a6e-435ba83aa63f</RequestId>"
//
                                       + "</ErrorResponse>";

  static String changeResourceRecordSetsRequestCreateA = ""//
                                                         + "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">"
//
                                                         + "<ChangeBatch><Changes><Change><Action>CREATE</Action>"
//
                                                         + "<ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet>"
//
                                                         + "</Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";

  static String changeResourceRecordSetsResponsePending = ""//
                                                          + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
//
                                                          + "<ChangeResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
//
                                                          + "  <ChangeInfo>\n"//
                                                          + "    <Id>/change/C2682N5HXP0BZ4</Id>\n"
//
                                                          + "    <Status>PENDING</Status>\n"//
                                                          + "    <SubmittedAt>2010-09-10T01:36:41.958Z</SubmittedAt>\n"
//
                                                          + "  </ChangeInfo>\n"//
                                                          + "</ChangeResourceRecordSetsResponse>";
  static String changeResourceRecordSetsRequestCreateAliasTarget = ""//
                                                                   + "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">"
//
                                                                   + "<ChangeBatch><Changes><Change><Action>CREATE</Action>"
//
                                                                   + "<ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><AliasTarget><HostedZoneId>Z3I0BTR7N27QRM</HostedZoneId><DNSName>ipv4-route53recordsetlivetest.adrianc.myzone.com.</DNSName><EvaluateTargetHealth>false</EvaluateTargetHealth></AliasTarget></ResourceRecordSet>"
//
                                                                   + "</Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";
  static String invalidChangeBatchDuplicate = ""//
                                              + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"//
                                              + "<InvalidChangeBatch xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
//
                                              + "  <Messages>\n"//
                                              + "    <Message>Tried to create resource record set www.denominator.io. type A, but it already exists</Message>\n"
//
                                              + "  </Messages>\n"//
                                              + "</InvalidChangeBatch>";
  static javax.inject.Provider<Map<String, String>>
      lazyAuthHeaders =
      new javax.inject.Provider<Map<String, String>>() {

        @Override
        public Map<String, String> get() {
          return ImmutableMap.of();
        }

      };

  static Route53 mockApi(final int port) {
    return Feign.create(new Route53Target(new Route53Provider() {
      @Override
      public String url() {
        return "http://localhost:" + port;
      }
    }, lazyAuthHeaders), new Route53Provider.FeignModule());
  }

  @Test
  public void changeResourceRecordSetsRequestCreateAPending()
      throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(
        new MockResponse().setResponseCode(200).setBody(changeResourceRecordSetsResponsePending));
    server.play();

    try {
      Route53 api = mockApi(server.getPort());

      ImmutableList<ActionOnResourceRecordSet>
          batch =
          ImmutableList.of(ActionOnResourceRecordSet.create(a(
              "www.denominator.io.", 3600, "192.0.2.1")));
      api.changeResourceRecordSets("Z1PA6795UKMFR9", batch);

      RecordedRequest createRRSet = server.takeRequest();
      assertEquals(createRRSet.getRequestLine(),
                   "POST /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
      assertEquals(new String(createRRSet.getBody()), changeResourceRecordSetsRequestCreateA);
    } finally {
      server.shutdown();
    }
  }

  @Test
  public void changeResourceRecordSetsWhenAliasTarget() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(
        new MockResponse().setResponseCode(200).setBody(changeResourceRecordSetsResponsePending));
    server.play();

    try {
      Route53 api = mockApi(server.getPort());

      ImmutableList<ActionOnResourceRecordSet> batch = ImmutableList.of(ActionOnResourceRecordSet
                                                                            .create(
                                                                                ResourceRecordSet
                                                                                    .<AliasTarget>builder()
                                                                                    .name(
                                                                                        "www.denominator.io.")
                                                                                    .type("A")
                                                                                    .add(AliasTarget
                                                                                             .create(
                                                                                                 "Z3I0BTR7N27QRM",
                                                                                                 "ipv4-route53recordsetlivetest.adrianc.myzone.com."))
                                                                                    .build()));
      api.changeResourceRecordSets("Z1PA6795UKMFR9", batch);

      RecordedRequest createRRSet = server.takeRequest();
      assertEquals(createRRSet.getRequestLine(),
                   "POST /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
      assertEquals(new String(createRRSet.getBody()),
                   changeResourceRecordSetsRequestCreateAliasTarget);
    } finally {
      server.shutdown();
    }
  }

  @Test(expectedExceptions = InvalidChangeBatchException.class, expectedExceptionsMessageRegExp = "Route53#changeResourceRecordSets\\(String,List\\) failed with errors \\[Tried to create resource record set www.denominator.io. type A, but it already exists\\]")
  public void changeResourceRecordSetsRequestCreateADuplicate()
      throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
    server.enqueue(new MockResponse().setResponseCode(400).setBody(invalidChangeBatchDuplicate));
    server.play();

    try {
      Route53 api = mockApi(server.getPort());

      ImmutableList<ActionOnResourceRecordSet>
          batch =
          ImmutableList.of(ActionOnResourceRecordSet.create(a(
              "www.denominator.io.", 3600, "192.0.2.1")));
      api.changeResourceRecordSets("Z1PA6795UKMFR9", batch);
    } finally {
      RecordedRequest createRRSet = server.takeRequest();
      assertEquals(createRRSet.getRequestLine(),
                   "POST /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
      assertEquals(new String(createRRSet.getBody()), changeResourceRecordSetsRequestCreateA);
      server.shutdown();
    }
  }
}
