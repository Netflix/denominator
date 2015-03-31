package denominator.clouddns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.clouddns.RackspaceApisTest.domainsResponse;
import static denominator.clouddns.RackspaceApisTest.soaResponse;

public class CloudDNSZoneApiMockTest {

  @Rule
  public final MockCloudDNSServer server = new MockCloudDNSServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));
    server.enqueue(new MockResponse().setBody(soaResponse));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).containsExactly(
        Zone.create("1234", "denominator.io", 3600, "admin@denominator.io")
    );

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains");
    server.assertRequest()
        .hasPath("/v1.0/123123/domains/1234/records?name=denominator.io&type=SOA");
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterator()).isEmpty();

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains");
  }

  @Test
  public void iteratorByNameWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));
    server.enqueue(new MockResponse().setBody(soaResponse));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io")).containsExactly(
        Zone.create("1234", "denominator.io", 3600, "admin@denominator.io")
    );

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains?name=denominator.io");
    server.assertRequest()
        .hasPath("/v1.0/123123/domains/1234/records?name=denominator.io&type=SOA");
  }

  @Test
  public void iteratorByNameWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{\"domains\":[],\"totalEntries\":0}"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io")).isEmpty();

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains?name=denominator.io");
  }
}
