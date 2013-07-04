package denominator.route53;

import static denominator.CredentialsConfiguration.credentials;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Denominator;
import denominator.ZoneApi;
import denominator.model.Zone;

@Test(singleThreaded = true)
public class Route53ZoneApiMockTest {

    String hostedZones = "<ListHostedZonesResponse><HostedZones><HostedZone><Id>/hostedzone/Z1PA6795UKMFR9</Id><Name>denominator.io.</Name><CallerReference>denomination</CallerReference><Config><Comment>no comment</Comment></Config><ResourceRecordSetCount>17</ResourceRecordSetCount></HostedZone></HostedZones></ListHostedZonesResponse>";

    @Test
    public void iteratorWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(hostedZones));
        server.play();

        try {
            ZoneApi api = mockApi(server.getUrl(""));
            Zone zone = api.iterator().next();
            assertEquals(zone.name(), "denominator.io.");
            assertEquals(zone.id(), "Z1PA6795UKMFR9");

            assertEquals(server.getRequestCount(), 1);
            assertEquals(server.takeRequest().getRequestLine(), "GET /2012-12-12/hostedzone HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    String noHostedZones = "<ListHostedZonesResponse><HostedZones /></ListHostedZonesResponse>";

    @Test
    public void iteratorWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(noHostedZones));
        server.play();

        try {
            ZoneApi api = mockApi(server.getUrl(""));
            assertFalse(api.iterator().hasNext());

            assertEquals(server.getRequestCount(), 1);
            assertEquals(server.takeRequest().getRequestLine(), "GET /2012-12-12/hostedzone HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    private static ZoneApi mockApi(final URL url) {
        return Denominator.create(new Route53Provider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, credentials("jclouds", "joe", "letmein")).api().zones();
    }
 }
