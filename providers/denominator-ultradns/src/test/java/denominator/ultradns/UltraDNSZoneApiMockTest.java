package denominator.ultradns;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUser;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUserResponse;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccount;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponsePresent;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

import java.io.IOException;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Denominator;
import denominator.ZoneApi;
import denominator.model.Zone;

@Test(singleThreaded = true)
public class UltraDNSZoneApiMockTest {

    @Test
    public void iteratorWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setBody(getZonesOfAccountResponsePresent));
        server.play();

        try {
            ZoneApi api = mockApi(server.getPort());
            Zone zone = api.iterator().next();
            assertEquals(zone.name(), "denominator.io.");
            assertNull(zone.id());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(new String(server.takeRequest().getBody()), getAccountsListOfUser);
            assertEquals(new String(server.takeRequest().getBody()), getZonesOfAccount);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iteratorWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));
        server.play();

        try {
            ZoneApi api = mockApi(server.getPort());
            assertFalse(api.iterator().hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(new String(server.takeRequest().getBody()), getAccountsListOfUser);
            assertEquals(new String(server.takeRequest().getBody()), getZonesOfAccount);
        } finally {
            server.shutdown();
        }
    }

    private static ZoneApi mockApi(final int port) {
        return Denominator.create(new UltraDNSProvider() {
            @Override
            public String url() {
                return "http://localhost:" + port + "/";
            }
        }, credentials("joe", "letmein")).api().zones();
    }
}
