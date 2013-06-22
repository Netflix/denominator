package denominator.ultradns;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfZoneResponseAbsent;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.DNSApi;
import denominator.Denominator;

@Test
public class UltraDNSProviderDynamicUpdateMockTest {

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

            assertEquals(api.basicRecordSetsInZone("denominator.io.").getByNameAndType("www.denominator.io.", "A"),
                    Optional.absent());
            dynamicUrl.set(new URL(mockUrl, updatedPath));
            assertEquals(api.basicRecordSetsInZone("denominator.io.").getByNameAndType("www.denominator.io.", "A"),
                    Optional.absent());

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

            DNSApi api = Denominator.create(new UltraDNSProvider() {
                @Override
                public String url() {
                    return server.getUrl("/").toString();
                }
            }, credentials(new Supplier<Credentials>() {
                @Override
                public Credentials get() {
                    return dynamicCredentials.get();
                }
            })).api();

            assertEquals(api.basicRecordSetsInZone("denominator.io.").getByNameAndType("www.denominator.io.", "A"),
                    Optional.absent());
            dynamicCredentials.set(ListCredentials.from("bob", "comeon"));
            assertEquals(api.basicRecordSetsInZone("denominator.io.").getByNameAndType("www.denominator.io.", "A"),
                    Optional.absent());

            assertEquals(server.getRequestCount(), 2);
            assertTrue(new String(server.takeRequest().getBody()).indexOf("letmein") != -1);
            assertTrue(new String(server.takeRequest().getBody()).indexOf("comeon") != -1);
        } finally {
            server.shutdown();
        }
    }
}
