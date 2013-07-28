package denominator.ultradns;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.ultradns.UltraDNSTest.getNeustarNetworkStatus;
import static denominator.ultradns.UltraDNSTest.getNeustarNetworkStatusResponse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.DNSApiManager;
import denominator.Denominator;

@Test(singleThreaded = true)
public class NetworkStatusReadableMockTest {

    @Test
    public void singleRequestOnSuccess() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().setBody(getNeustarNetworkStatusResponse));
        server.play();

        try {
            assertTrue(mockApi(server.getUrl("")).checkConnection());

            assertEquals(server.getRequestCount(), 1);
            assertEquals(new String(server.takeRequest().getBody()), getNeustarNetworkStatus);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void singleRequestOnFailure() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().setResponseCode(500));
        server.play();

        try {
            assertFalse(mockApi(server.getUrl("")).checkConnection());

            assertEquals(server.getRequestCount(), 1);
            assertEquals(new String(server.takeRequest().getBody()), getNeustarNetworkStatus);
        } finally {
            server.shutdown();
        }
    }

    static DNSApiManager mockApi(final URL url) {
        return Denominator.create(new UltraDNSProvider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, credentials("joe", "letmein"));
    }
}
