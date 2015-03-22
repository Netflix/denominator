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
import static org.junit.Assert.assertFalse;

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

  @Test
  public void iteratorByNameWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(
        "{\"domains\":[{\"name\":\"denominator.io\",\"id\":1234,\"emailAddress\":\"fake@denominator.io\",\"updated\":\"2015-03-22T18:21:33.000+0000\",\"created\":\"2015-03-22T18:21:33.000+0000\"}],\"totalEntries\":1}"));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io").next())
        .hasName("denominator.io")
        .hasId(String.valueOf(domainId));

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1.0/123123/domains?name=denominator.io");
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
