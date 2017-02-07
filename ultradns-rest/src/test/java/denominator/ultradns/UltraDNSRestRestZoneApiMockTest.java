package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.ultradns.UltraDNSMockResponse.GET_RESOURCE_RECORDS_PRESENT;
import static denominator.ultradns.UltraDNSMockResponse.GET_SOA_RESOURCE_RECORDS;

public class UltraDNSRestRestZoneApiMockTest {

  @Rule
  public final MockUltraDNSRestServer server = new MockUltraDNSRestServer();

  @Test
  public void putWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(UltraDNSMockResponse.GET_ACCOUNTS_LIST_OF_USER_RESPONSE));
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse().setBody(GET_SOA_RESOURCE_RECORDS));
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
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/6/denominator.io.");
  }

  @Test
  public void putWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(UltraDNSMockResponse.GET_ACCOUNTS_LIST_OF_USER_RESPONSE));
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse().setBody(GET_SOA_RESOURCE_RECORDS));
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    Zone zone = Zone.create(null, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());

    server.enqueue(new MockResponse().setResponseCode(400).setBody(UltraDNSMockResponse.getMockErrorResponse("1802", "Zone already exists in the system.")));

    server.assertSessionRequest();
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/accounts");
    server.assertRequest()
            .hasMethod("POST")
            .hasPath("/zones")
            .hasBody("{\"properties\": {\"name\": \"denominator.io.\",\"accountName\": \"npp-rest-test1\",\"type\": \"PRIMARY\"}, " +
                    "\"primaryCreateInfo\": {\"forceImport\": false,\"createType\": \"NEW\"}}");
    server.assertRequest()
            .hasMethod("GET")
            .hasPath("/zones/denominator.io./rrsets/6/denominator.io.");
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
    server.enqueue(new MockResponse().setResponseCode(404).setBody(UltraDNSMockResponse.getMockErrorResponse("1801", "Zone does not exist in the system.")));

    ZoneApi api = server.connect().api().zones();
    api.delete("denominator.io.");

    server.assertSessionRequest();
    server.assertRequest().hasMethod("DELETE").hasPath("/zones/denominator.io.");
  }
}