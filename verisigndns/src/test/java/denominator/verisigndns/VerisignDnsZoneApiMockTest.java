package denominator.verisigndns;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import com.squareup.okhttp.mockwebserver.MockResponse;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.verisigndns.VerisignDnsTest.getZoneListRes;
import static denominator.verisigndns.VerisignDnsTest.getZoneInfoRes;

public class VerisignDnsZoneApiMockTest {

  @Rule
  public final MockVerisignDnsServer server = new MockVerisignDnsServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueue(getZoneListRes);
    server.enqueue(getZoneInfoRes);
    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).containsExactly(
        Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io"));
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("<api1:getZoneList></api1:getZoneList>"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterator()).isEmpty();
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {

    server.enqueue(getZoneInfoRes);
    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io")).containsExactly(
        Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io"));
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody("<ns3:getZoneInfoRes></ns3:getZoneInfoRes>"));

    ZoneApi api = server.connect().api().zones();
    assertThat(api.iterateByName("denominator.io.")).isEmpty();
  }

  @Test
  public void putWhenPresent() throws Exception {
    server.enqueueError("ERROR_OPERATION_FAILURE",
        "Domain already exists. Please verify your domain name.");
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io");
    api.put(zone);
  }

  @Test
  public void putWhenAbsent() throws Exception {
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse());
    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create("denominator.io", "denominator.io", 86400, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server
        .enqueue(new MockResponse()
            .setBody("<ns3:dnsaWSRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
                + "   <ns3:callSuccess>true</ns3:callSuccess>" + "</ns3:dnsaWSRes>"));

    ZoneApi api = server.connect().api().zones();
    api.delete("denominator.io.");
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    api.delete("test.io");
  }
}
