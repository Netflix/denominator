package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApi;
import denominator.Denominator;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUser;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUserResponse;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfZoneResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccount;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponseAbsent;

public class UltraDNSProviderDynamicUpdateMockTest {

  @Rule
  public final MockUltraDNSServer server = new MockUltraDNSServer();

  @Test
  public void dynamicEndpointUpdates() throws Exception {
    final AtomicReference<String> url = new AtomicReference<String>(server.url());
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));

    DNSApi api = Denominator.create(new UltraDNSProvider() {
      @Override
      public String url() {
        return url.get();
      }
    }, credentials(server.credentials())).api();

    api.zones().iterator();
    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(getZonesOfAccount);

    MockUltraDNSServer server2 = new MockUltraDNSServer();
    url.set(server2.url());
    server2.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server2.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));

    api.zones().iterator();

    server2.assertSoapBody(getAccountsListOfUser);
    server2.assertSoapBody(getZonesOfAccount);
    server2.shutdown();
  }

  @Test
  public void dynamicCredentialUpdates() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));

    AtomicReference<Credentials>
        dynamicCredentials =
        new AtomicReference<Credentials>(server.credentials());

    DNSApi
        api =
        Denominator.create(server, new OverrideCredentials(dynamicCredentials)).api();

    api.zones().iterator();

    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(getZonesOfAccount);

    dynamicCredentials.set(ListCredentials.from("bob", "comeon"));

    server.credentials("bob", "comeon");
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));

    api.zones().iterator();

    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(getZonesOfAccount);
  }

  @Test
  public void dynamicAccountIdUpdatesOnEndpoint() throws Exception {
    final AtomicReference<String> url = new AtomicReference<String>(server.url());
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));

    DNSApi api = Denominator.create(new UltraDNSProvider() {
      @Override
      public String url() {
        return url.get();
      }
    }, credentials(server.credentials())).api();

    api.zones().iterator();

    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(getZonesOfAccount);

    MockUltraDNSServer server2 = new MockUltraDNSServer();
    url.set(server2.url());
    server2.enqueue(new MockResponse().setBody(
        getAccountsListOfUserResponse.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB")));
    server2.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));

    api.zones().iterator();

    server2.assertSoapBody(getAccountsListOfUser.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB"));
    server2.assertSoapBody(getZonesOfAccount.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB"));

    server2.shutdown();
  }

  @Test
  public void dynamicAccountIdUpdatesOnCredentials() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));

    AtomicReference<Credentials>
        dynamicCredentials =
        new AtomicReference<Credentials>(server.credentials());

    DNSApi
        api =
        Denominator.create(server, new OverrideCredentials(dynamicCredentials)).api();

    api.zones().iterator();

    server.assertSoapBody(getAccountsListOfUser.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB"));
    server.assertSoapBody(getZonesOfAccount);

    dynamicCredentials.set(ListCredentials.from("bob", "comeon"));

    server.credentials("bob", "comeon");
    server.enqueue(new MockResponse().setBody(
        getAccountsListOfUserResponse.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB")));
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));

    api.zones().iterator();

    server.assertSoapBody(getAccountsListOfUser.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB"));
    server.assertSoapBody(getZonesOfAccount.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB"));
  }

  @Module(complete = false, library = true, overrides = true)
  static class OverrideCredentials {

    final AtomicReference<Credentials> dynamicCredentials;

    OverrideCredentials(AtomicReference<Credentials> dynamicCredentials) {
      this.dynamicCredentials = dynamicCredentials;
    }

    @Provides
    public Credentials get() {
      return dynamicCredentials.get();
    }
  }
}
