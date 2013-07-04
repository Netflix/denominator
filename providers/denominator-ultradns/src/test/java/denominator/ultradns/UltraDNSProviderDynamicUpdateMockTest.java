package denominator.ultradns;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUser;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUserResponse;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfZoneResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccount;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponsePresent;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApi;
import denominator.Denominator;

@Test
public class UltraDNSProviderDynamicUpdateMockTest {

    @Test
    public void dynamicAccountIdUpdatesOnEndpoint() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setBody(getZonesOfAccountResponsePresent));
        server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB")));
        server.enqueue(new MockResponse().setBody(getZonesOfAccountResponsePresent));
        server.play();
        
        try {
            String initialPath = "/";
            String updatedPath = "/alt/";
            URL mockUrl = server.getUrl(initialPath);
            final AtomicReference<URL> dynamicUrl = new AtomicReference<URL>(mockUrl);

            DNSApi api = Denominator.create(new UltraDNSProvider() {
                @Override
                public String url() {
                    return dynamicUrl.get().toString();
                }
            }, credentials("joe", "letmein")).api();

            api.zones().iterator().next();
            dynamicUrl.set(new URL(mockUrl, updatedPath));
            api.zones().iterator().next();

            assertEquals(server.getRequestCount(), 4);
            assertEquals(new String(server.takeRequest().getBody()), getAccountsListOfUser);
            assertEquals(new String(server.takeRequest().getBody()), getZonesOfAccount);
            assertEquals(new String(server.takeRequest().getBody()), getAccountsListOfUser);
            assertEquals(new String(server.takeRequest().getBody()), getZonesOfAccount.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void dynamicAccountIdUpdatesOnCredentials() throws IOException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setBody(getZonesOfAccountResponsePresent));
        server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse.replace("AAAAAAAAAAAAAAAA", "BBBBBBBBBBBBBBBB")));
        server.enqueue(new MockResponse().setBody(getZonesOfAccountResponsePresent));
        server.play();
        
        try {
            final AtomicReference<Credentials> dynamicCredentials = new AtomicReference<Credentials>(
                    ListCredentials.from("joe", "letmein"));

            @Module(complete = false, library = true, overrides = true)
            class OverrideCredentials {
                @Provides
                public Credentials get() {
                    return dynamicCredentials.get();
                }
            }

            DNSApi api = Denominator.create(new UltraDNSProvider() {
                @Override
                public String url() {
                    return server.getUrl("/").toString();
                }
            }, new OverrideCredentials()).api();

            api.zones().iterator().next();
            dynamicCredentials.set(ListCredentials.from("bob", "comeon"));
            api.zones().iterator().next();

            assertEquals(server.getRequestCount(), 4);
            assertTrue(new String(server.takeRequest().getBody()).indexOf("letmein") != -1);
            assertTrue(new String(server.takeRequest().getBody()).indexOf("AAAAAAAAAAAAAAAA") != -1);
            assertTrue(new String(server.takeRequest().getBody()).indexOf("comeon") != -1);
            assertTrue(new String(server.takeRequest().getBody()).indexOf("BBBBBBBBBBBBBBBB") != -1);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void dynamicEndpointUpdates() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getResourceRecordsOfZoneResponseAbsent));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getResourceRecordsOfZoneResponseAbsent));
        server.play();

        try {
            String initialPath = "/";
            String updatedPath = "/alt/";
            URL mockUrl = server.getUrl(initialPath);
            final AtomicReference<URL> dynamicUrl = new AtomicReference<URL>(mockUrl);

            DNSApi api = Denominator.create(new UltraDNSProvider() {
                @Override
                public String url() {
                    return dynamicUrl.get().toString();
                }
            }, credentials("joe", "letmein")).api();

            assertNull(api.basicRecordSetsInZone("denominator.io.").getByNameAndType("www.denominator.io.", "A"));
            dynamicUrl.set(new URL(mockUrl, updatedPath));
            assertNull(api.basicRecordSetsInZone("denominator.io.").getByNameAndType("www.denominator.io.", "A"));

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST " + initialPath + " HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "POST " + updatedPath + " HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void dynamicCredentialUpdates() throws IOException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getResourceRecordsOfZoneResponseAbsent));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(getResourceRecordsOfZoneResponseAbsent));
        server.play();

        try {
            final AtomicReference<Credentials> dynamicCredentials = new AtomicReference<Credentials>(
                    ListCredentials.from("joe", "letmein"));

            @Module(complete = false, library = true, overrides = true)
            class OverrideCredentials {
                @Provides
                public Credentials get() {
                    return dynamicCredentials.get();
                }
            }

            DNSApi api = Denominator.create(new UltraDNSProvider() {
                @Override
                public String url() {
                    return server.getUrl("/").toString();
                }
            }, new OverrideCredentials()).api();

            assertNull(api.basicRecordSetsInZone("denominator.io.").getByNameAndType("www.denominator.io.", "A"));
            dynamicCredentials.set(ListCredentials.from("bob", "comeon"));
            assertNull(api.basicRecordSetsInZone("denominator.io.").getByNameAndType("www.denominator.io.", "A"));

            assertEquals(server.getRequestCount(), 2);
            assertTrue(new String(server.takeRequest().getBody()).indexOf("letmein") != -1);
            assertTrue(new String(server.takeRequest().getBody()).indexOf("comeon") != -1);
        } finally {
            server.shutdown();
        }
    }
}
