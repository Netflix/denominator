package denominator.designate;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.Iterator;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.designate.DesignateTest.domainId;
import static denominator.designate.DesignateTest.domainsResponse;

public class DesignateZoneApiMockTest {

  @Rule
  public MockDesignateServer server = new MockDesignateServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueueAuthResponse();
    server.enqueue(new MockResponse().setBody(domainsResponse));

    ZoneApi api = server.connect().api().zones();
    Iterator<Zone> domains = api.iterator();

    assertThat(domains.next())
        .hasName("denominator.io.")
        .hasId(domainId);

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

    assertThat(api.iterateByName("denominator.io."))
        .contains(Zone.create("denominator.io.", domainId));

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
}
