package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import denominator.ZoneApi;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUser;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUserResponse;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccount;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponsePresent;
import static org.testng.Assert.assertFalse;

@Test(singleThreaded = true)
public class UltraDNSZoneApiMockTest {

  MockUltraDNSServer server;

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getZonesOfAccountResponsePresent));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator().next()).hasName("denominator.io.");

    server.assertRequestHasBody(getAccountsListOfUser);
    server.assertRequestHasBody(getZonesOfAccount);
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));

    ZoneApi api = server.connect().api().zones();

    assertFalse(api.iterator().hasNext());

    server.assertRequestHasBody(getAccountsListOfUser);
    server.assertRequestHasBody(getZonesOfAccount);
  }

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockUltraDNSServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
