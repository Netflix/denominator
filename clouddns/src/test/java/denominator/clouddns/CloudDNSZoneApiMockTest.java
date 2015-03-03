package denominator.clouddns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.clouddns.RackspaceApisTest.domainId;
import static denominator.clouddns.RackspaceApisTest.domainsResponse;
import static junit.framework.Assert.assertFalse;

public class CloudDNSZoneApiMockTest {

  @Rule
  public final MockCloudDNSServer server = new MockCloudDNSServer();

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
}
