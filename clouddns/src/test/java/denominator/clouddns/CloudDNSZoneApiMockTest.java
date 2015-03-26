package denominator.clouddns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.clouddns.RackspaceApisTest.domainResponse;
import static denominator.clouddns.RackspaceApisTest.domainsResponse;

public class CloudDNSZoneApiMockTest {

  @Rule
  public final MockCloudDNSServer server = new MockCloudDNSServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));
    server.enqueue(new MockResponse().setBody(domainResponse));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).containsExactly(
        Zone.builder().name("denominator.io").id("1234").email("admin@denominator.io").ttl(3600)
            .build()
    );

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains");
    server.assertRequest()
        .hasPath("/v1.0/123123/domains/1234?showRecords=false&showSubdomains=false");
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
    server.enqueue(new MockResponse().setBody(domainResponse));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io")).containsExactly(
        Zone.builder().name("denominator.io").id("1234").email("admin@denominator.io").ttl(3600)
            .build()
    );

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains?name=denominator.io");
    server.assertRequest()
        .hasPath("/v1.0/123123/domains/1234?showRecords=false&showSubdomains=false");
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
