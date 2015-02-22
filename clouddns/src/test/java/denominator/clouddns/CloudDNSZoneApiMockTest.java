package denominator.clouddns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Iterator;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.clouddns.RackspaceApisTest.domainId;
import static denominator.clouddns.RackspaceApisTest.domainsResponse;
import static org.testng.Assert.assertFalse;

@Test(singleThreaded = true)
public class CloudDNSZoneApiMockTest {

  MockCloudDNSServer server;

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));

    ZoneApi api = server.connect().api().zones();
    Iterator<Zone> domains = api.iterator();

    assertThat(domains.next())
        .hasName("denominator.io")
        .hasId(String.valueOf(domainId));

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains");
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    ZoneApi api = server.connect().api().zones();
    assertFalse(api.iterator().hasNext());

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains");
  }

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockCloudDNSServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
