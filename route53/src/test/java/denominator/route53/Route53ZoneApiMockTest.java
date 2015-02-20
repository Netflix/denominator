package denominator.route53;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Iterator;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static org.testng.Assert.assertFalse;

@Test(singleThreaded = true)
public class Route53ZoneApiMockTest {

  MockRoute53Server server;

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

    ZoneApi api = server.connect().api().zones();
    Iterator<Zone> domains = api.iterator();

    assertThat(domains.next())
        .hasName("denominator.io.")
        .hasId("Z1PA6795UKMFR9");

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone");
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(
        "<ListHostedZonesResponse><HostedZones /></ListHostedZonesResponse>"));

    ZoneApi api = server.connect().api().zones();
    assertFalse(api.iterator().hasNext());

    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/2012-12-12/hostedzone");
  }

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockRoute53Server();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
