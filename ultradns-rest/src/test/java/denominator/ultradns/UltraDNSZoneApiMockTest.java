package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.ultradns.UltraDNSMockResponse.getAccountsListOfUserResponse;
import static denominator.ultradns.UltraDNSMockResponse.getMockErrorResponse;

public class UltraDNSZoneApiMockTest {

  @Rule
  public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

  @Test
  public void putWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    Zone zone = Zone.create(null, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());

    server.assertSessionRequest();
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/accounts");
    server.assertRequest()
            .hasMethod("POST")
            .hasPath("/zones")
            .hasBody("{\"properties\": {\"name\": \"denominator.io.\",\"accountName\": \"npp-rest-test1\",\"type\": \"PRIMARY\"}, " +
                    "\"primaryCreateInfo\": {\"forceImport\": false,\"createType\": \"NEW\"}}");
  }

  @Test
  public void putWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    Zone zone = Zone.create(null, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());

    server.enqueue(new MockResponse().setResponseCode(400).setBody(getMockErrorResponse("1802", "Zone already exists in the system.")));

    server.assertSessionRequest();
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/accounts");
    server.assertRequest()
            .hasMethod("POST")
            .hasPath("/zones")
            .hasBody("{\"properties\": {\"name\": \"denominator.io.\",\"accountName\": \"npp-rest-test1\",\"type\": \"PRIMARY\"}, " +
                    "\"primaryCreateInfo\": {\"forceImport\": false,\"createType\": \"NEW\"}}");
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    api.delete("denominator.io.");

    server.assertSessionRequest();
    server.assertRequest().hasMethod("DELETE").hasPath("/zones/denominator.io.");
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setResponseCode(404).setBody(getMockErrorResponse("1801", "Zone does not exist in the system.")));

    ZoneApi api = server.connect().api().zones();
    api.delete("denominator.io.");

    server.assertSessionRequest();
    server.assertRequest().hasMethod("DELETE").hasPath("/zones/denominator.io.");
  }
}
