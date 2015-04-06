package denominator.designate;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.designate.DesignateTest.domainId;
import static denominator.designate.DesignateTest.domainResponse;
import static denominator.designate.DesignateTest.domainsResponse;
import static java.lang.String.format;

public class DesignateZoneApiMockTest {

  @Rule
  public MockDesignateServer server = new MockDesignateServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).containsExactly(
        Zone.create(domainId, "denominator.io.", 3601, "nil@denominator.io")
    );

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1/domains");
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterator()).isEmpty();

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1/domains");
  }

  @Test
  public void iteratorByNameWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.")).containsExactly(
        Zone.create(domainId, "denominator.io.", 3601, "nil@denominator.io")
    );

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1/domains");
  }

  /**
   * Client-side filter is used as there's no server-side command.
   */
  @Test
  public void iteratorByNameWhenIrrelevant() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.com.")).isEmpty();

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1/domains");
  }

  @Test
  public void iteratorByNameWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody("{ \"domains\": [] }"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io.")).isEmpty();

    server.assertAuthRequest();
    server.assertRequest().hasPath("/v1/domains");
  }

  @Test
  public void putWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainResponse));

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create(null, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(domainId);

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/v1/domains")
        .hasBody(
            "{\"name\":\"denominator.io.\",\"ttl\":3601,\"email\":\"nil@denominator.io\"}");
  }

  @Test
  public void putWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setResponseCode(409).setBody(
        "{\"code\": 409, \"type\": \"duplicate_domain\", \"request_id\": \"req-6b86464d-15bb-467f-aa9e-7c90b10555f3\"}"));
    server.enqueue(new MockResponse().setBody(domainsResponse));
    server.enqueue(new MockResponse().setBody(domainResponse));

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create(null, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(domainId);

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/v1/domains")
        .hasBody(
            "{\"name\":\"denominator.io.\",\"ttl\":3601,\"email\":\"nil@denominator.io\"}");
    server.assertRequest().hasPath("/v1/domains");
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/v1/domains/" + domainId)
        .hasBody(format(
            "{\"id\":\"%s\",\"name\":\"denominator.io.\",\"ttl\":3601,\"email\":\"nil@denominator.io\"}",
            domainId));
  }

  @Test
  public void putWhenPresent_withId() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainResponse));

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create(domainId, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(domainId);

    server.assertAuthRequest();
    server.assertRequest()
        .hasMethod("PUT")
        .hasPath("/v1/domains/" + domainId)
        .hasBody(format(
            "{\"id\":\"%s\",\"name\":\"denominator.io.\",\"ttl\":3601,\"email\":\"nil@denominator.io\"}",
            domainId));
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    api.delete(domainId);

    server.assertAuthRequest();
    server.assertRequest().hasMethod("DELETE").hasPath("/v1/domains/" + domainId);
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(
        "{\"code\": 404, \"type\": \"domain_not_found\", \"request_id\": \"req-809e7455-0e56-42ec-8674-a1aa24e18600\"}"));

    ZoneApi api = server.connect().api().zones();
    api.delete(domainId);

    server.assertAuthRequest();
    server.assertRequest().hasMethod("DELETE").hasPath("/v1/domains/" + domainId);
  }
}
