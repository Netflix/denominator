package denominator.route53;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import denominator.model.ResourceRecordSet;
import denominator.route53.Route53.ActionOnResourceRecordSet;
import feign.Feign;

import static denominator.model.ResourceRecordSets.a;

public class Route53Test {

  @Rule
  public MockRoute53Server server = new MockRoute53Server();
  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void changeResourceRecordSetsRequestCreateAPending() throws Exception {
    server.enqueue(new MockResponse().setBody(changeResourceRecordSetsResponsePending));

    ActionOnResourceRecordSet
        createA =
        ActionOnResourceRecordSet.create(a("www.denominator.io.", 3600, "192.0.2.1"));

    mockApi().changeResourceRecordSets("Z1PA6795UKMFR9", Arrays.asList(createA));

    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset")
        .hasXMLBody(changeResourceRecordSetsRequestCreateA);
  }

  @Test
  public void changeResourceRecordSetsWhenAliasTarget() throws Exception {
    server.enqueue(new MockResponse().setBody(changeResourceRecordSetsResponsePending));

    ActionOnResourceRecordSet createAlias = ActionOnResourceRecordSet
        .create(ResourceRecordSet
                    .<AliasTarget>builder()
                    .name("www.denominator.io.")
                    .type("A")
                    .add(AliasTarget
                             .create(
                                 "Z3I0BTR7N27QRM",
                                 "ipv4-route53recordsetlivetest.adrianc.myzone.com."))
                    .build());

    mockApi().changeResourceRecordSets("Z1PA6795UKMFR9", Arrays.asList(createAlias));

    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset")
        .hasXMLBody(
            "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
            + "  <ChangeBatch>\n"
            + "    <Changes>\n"
            + "      <Change>\n"
            + "        <Action>CREATE</Action>\n"
            + "        <ResourceRecordSet>\n"
            + "          <Name>www.denominator.io.</Name>\n"
            + "          <Type>A</Type>\n"
            + "          <AliasTarget>\n"
            + "            <HostedZoneId>Z3I0BTR7N27QRM</HostedZoneId>\n"
            + "            <DNSName>ipv4-route53recordsetlivetest.adrianc.myzone.com.</DNSName>\n"
            + "            <EvaluateTargetHealth>false</EvaluateTargetHealth>\n"
            + "          </AliasTarget>\n"
            + "        </ResourceRecordSet>\n"
            + "      </Change>\n"
            + "    </Changes>\n"
            + "  </ChangeBatch>\n"
            + "</ChangeResourceRecordSetsRequest>");
  }

  @Test
  public void changeResourceRecordSetsRequestCreateADuplicate() throws Exception {
    thrown.expect(InvalidChangeBatchException.class);
    thrown.expectMessage("Route53#changeResourceRecordSets(String,List) failed with errors [Tried to create resource record set www.denominator.io. type A, but it already exists]");

    server.enqueue(new MockResponse().setResponseCode(400).setBody(
        "<InvalidChangeBatch xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
        + "  <Messages>\n"
        + "    <Message>Tried to create resource record set www.denominator.io. type A, but it already exists</Message>\n"
        + "  </Messages>\n"
        + "</InvalidChangeBatch>"));

    ActionOnResourceRecordSet
        createA =
        ActionOnResourceRecordSet.create(a("www.denominator.io.", 3600, "192.0.2.1"));

    mockApi().changeResourceRecordSets("Z1PA6795UKMFR9", Arrays.asList(createA));
  }

  Route53 mockApi() {
    return Feign.create(new Route53Target(new Route53Provider() {
      @Override
      public String url() {
        return server.url();
      }
    }, new javax.inject.Provider<Map<String, String>>() {

      @Override
      public Map<String, String> get() {
        return Collections.emptyMap();
      }

    }), new Route53Provider.FeignModule());
  }

  String changeResourceRecordSetsRequestCreateA =
      "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">"
      + "<ChangeBatch><Changes><Change><Action>CREATE</Action>"
      + "<ResourceRecordSet><Name>www.denominator.io.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet>"
      + "</Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";

  String changeResourceRecordSetsResponsePending =
      "<ChangeResourceRecordSetsResponse xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\">\n"
      + "  <ChangeInfo>\n"
      + "    <Id>/change/C2682N5HXP0BZ4</Id>\n"
      + "    <Status>PENDING</Status>\n"
      + "    <SubmittedAt>2010-09-10T01:36:41.958Z</SubmittedAt>\n"
      + "  </ChangeInfo>\n"
      + "</ChangeResourceRecordSetsResponse>";
}
