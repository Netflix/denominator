package denominator.route53;
import static denominator.CredentialsConfiguration.credentials;
import static denominator.route53.Route53Test.invalidClientTokenId;
import static denominator.route53.Route53Test.noHostedZones;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.DNSApiManager;
import denominator.Denominator;

@Test(singleThreaded = true)
public class HostedZonesReadableMockTest {

    @Test
    public void singleRequestOnSuccess() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().setBody(noHostedZones));
        server.play();

        try {
            assertTrue(mockApi(server.getPort()).checkConnection());

            assertEquals(server.getRequestCount(), 1);
            assertEquals(server.takeRequest().getRequestLine(), "GET /2012-12-12/hostedzone HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void singleRequestOnFailure() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();

        server.enqueue(new MockResponse().setResponseCode(403).setBody(invalidClientTokenId));
        server.play();

        try {
            assertFalse(mockApi(server.getPort()).checkConnection());
            
            assertEquals(server.getRequestCount(), 1);
            assertEquals(server.takeRequest().getRequestLine(), "GET /2012-12-12/hostedzone HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    static DNSApiManager mockApi(final int port) {
        return Denominator.create(new Route53Provider() {
            @Override
            public String url() {
                return "http://localhost:" + port;
            }
        }, credentials("accessKey", "secretKey"));
    }
}
