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
    server.assertRequestHasBody(getAccountsListOfUser);
    server.assertRequestHasBody(getZonesOfAccount);

    MockUltraDNSServer server2 = new MockUltraDNSServer();
    url.set(server2.url());
    server2.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server2.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));

    api.zones().iterator();

    server2.assertRequestHasBody(getAccountsListOfUser);
    server2.assertRequestHasBody(getZonesOfAccount);
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

    server.assertRequestHasBody(getAccountsListOfUser);
    server.assertRequestHasBody(getZonesOfAccount);

    dynamicCredentials.set(ListCredentials.from("bob", "comeon"));

    server.credentials("bob", "comeon");
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));

    api.zones().iterator();

    server.assertRequestHasBody(
        getAccountsListOfUser.replace("joe", "bob").replace("letmein", "comeon"));
    server
        .assertRequestHasBody(getZonesOfAccount.replace("joe", "bob").replace("letmein", "comeon"));
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

    server.assertRequestHasBody(getAccountsListOfUser);
    server.assertRequestHasBody(getZonesOfAccount);

    MockUltraDNSServer server2 = new MockUltraDNSServer();
    url.set(server2.url());
    server2.enqueue(new MockResponse().setBody(
        getAccountsListOfUserResponse.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB")));
    server2.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));

    api.zones().iterator();

    server2.assertRequestHasBody(
        getAccountsListOfUser.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB"));
    server2.assertRequestHasBody(getZonesOfAccount.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB"));

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

    server.assertRequestHasBody(
        getAccountsListOfUser.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB"));
    server.assertRequestHasBody(getZonesOfAccount);

    dynamicCredentials.set(ListCredentials.from("bob", "comeon"));

    server.credentials("bob", "comeon");
    server.enqueue(new MockResponse().setBody(
        getAccountsListOfUserResponse.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB")));
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));

    api.zones().iterator();

    server.assertRequestHasBody(
        getAccountsListOfUser.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB").replace("joe", "bob")
            .replace("letmein", "comeon"));
    server
        .assertRequestHasBody(
            getZonesOfAccount.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB").replace(
                "joe", "bob").replace("letmein", "comeon"));
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
