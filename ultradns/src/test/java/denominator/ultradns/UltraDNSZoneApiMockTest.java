package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.ultradns.UltraDNSException.INVALID_ZONE_NAME;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUser;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUserResponse;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfDNameByType;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfDNameByTypeResponsePresent;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccount;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponsePresent;

public class UltraDNSZoneApiMockTest {

  @Rule
  public final MockUltraDNSServer server = new MockUltraDNSServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getZonesOfAccountResponsePresent));
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponsePresent));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).containsExactly(
        Zone.builder().name("denominator.io.").id("denominator.io.").email("adrianc.netflix.com.")
            .ttl(86400).build()
    );

    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(getZonesOfAccount);
    server.assertSoapBody(getResourceRecordsOfDNameByType);
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponsePresent));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).isEmpty();

    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(getZonesOfAccount);
  }

  @Test
  public void iteratorByName() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponsePresent));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.")).containsExactly(
        Zone.builder().name("denominator.io.").id("denominator.io.").email("adrianc.netflix.com.")
            .ttl(86400).build()
    );

    server.assertSoapBody(getResourceRecordsOfDNameByType);
  }

  @Test
  public void iteratorByNameWhenNotFound() throws Exception {
    server.enqueueError(INVALID_ZONE_NAME, "Invalid zone name.");

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.")).isEmpty();

    server.assertSoapBody(getResourceRecordsOfDNameByType);
  }
}
