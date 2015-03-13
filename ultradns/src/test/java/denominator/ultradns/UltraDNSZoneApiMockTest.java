package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.ZoneApi;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUser;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUserResponse;
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

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator().next()).hasName("denominator.io.");

    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(getZonesOfAccount);
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).isEmpty();

    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(getZonesOfAccount);
  }
}
