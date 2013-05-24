package denominator.route53;

import static denominator.CredentialsConfiguration.credentials;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import com.google.common.base.Supplier;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApi;
import denominator.Denominator;

@Test(singleThreaded = true)
public class Route53ProviderDynamicUpdateMockTest {
    String hostedZones = "<ListHostedZonesResponse><HostedZones /></ListHostedZonesResponse>";

    @Test
    public void dynamicEndpointUpdates() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(hostedZones));
        server.enqueue(new MockResponse().setBody(hostedZones));
        server.play();

        String initialPath = "/";
        String updatedPath = "/alt/";
        URL mockUrl = server.getUrl(initialPath);
        final AtomicReference<URL> dynamicUrl = new AtomicReference<URL>(mockUrl);

        try {
            DNSApi api = Denominator.create(new Route53Provider() {
                @Override
                public String getUrl() {
                    return dynamicUrl.get().toString();
                }
            }, credentials("accessKey", "secretKey")).getApi();

            assertFalse(api.getZoneApi().list().hasNext());
            dynamicUrl.set(new URL(mockUrl, updatedPath));
            assertFalse(api.getZoneApi().list().hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "GET /2012-02-29/hostedzone HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /alt/2012-02-29/hostedzone HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void dynamicCredentialUpdates() throws IOException, InterruptedException {
        final MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(hostedZones));
        server.enqueue(new MockResponse().setBody(hostedZones));
        server.play();

        try {

            final AtomicReference<Credentials> dynamicCredentials = new AtomicReference<Credentials>(ListCredentials.from("accessKey", "secretKey"));

            DNSApi api = Denominator.create(new Route53Provider() {
                @Override
                public String getUrl() {
                    return server.getUrl("/").toString();
                }
            }, credentials(new Supplier<Credentials>(){
                @Override
                public Credentials get() {
                    return dynamicCredentials.get();
                }
            })).getApi();

            assertFalse(api.getZoneApi().list().hasNext());
            dynamicCredentials.set(ListCredentials.from("accessKey2", "secretKey2"));
            assertFalse(api.getZoneApi().list().hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertTrue(server.takeRequest().getHeader("X-Amzn-Authorization").startsWith("AWS3-HTTPS AWSAccessKeyId=accessKey,Algorithm=HmacSHA256,Signature="));
            assertTrue(server.takeRequest().getHeader("X-Amzn-Authorization").startsWith("AWS3-HTTPS AWSAccessKeyId=accessKey2,Algorithm=HmacSHA256,Signature="));
        } finally {
            server.shutdown();
        }
    }
}
