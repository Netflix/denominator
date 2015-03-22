package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.ZoneApi;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.ultradns.UltraDNSException.ZONE_NOT_FOUND;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUser;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUserResponse;
import static denominator.ultradns.UltraDNSTest.getZoneInfo;
import static denominator.ultradns.UltraDNSTest.getZoneInfoResponseTemplate;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccount;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponsePresent;
import static java.lang.String.format;

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
  public void iteratorByNameWhenPresentAndSameAccount() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(
        new MockResponse().setBody(format(getZoneInfoResponseTemplate, "AAAAAAAAAAAAAAAA")));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.").next()).hasName("denominator.io.");

    server.assertSoapBody(getZoneInfo);
    server.assertSoapBody(getAccountsListOfUser);
  }

  @Test
  public void iteratorByNameWhenPresentAndDifferentAccount() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(
        new MockResponse().setBody(format(getZoneInfoResponseTemplate, "BBBBBBBBBBBBBBBB")));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.")).isEmpty();

    server.assertSoapBody(getZoneInfo);
    server.assertSoapBody(getAccountsListOfUser);
  }

  @Test
  public void iteratorByNameWhenNotFound() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueueError(ZONE_NOT_FOUND, "Zone does not exist in the system.");

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.")).isEmpty();

    server.assertSoapBody(getZoneInfo);
    server.assertSoapBody(getAccountsListOfUser);
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
