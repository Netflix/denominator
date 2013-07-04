package denominator.hook;

import static com.google.mockwebserver.SocketPolicy.DISCONNECT_AT_START;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.net.URI;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

@Test
public class InstanceMetadataHookTest {

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "metadataService must end with '/'; http://localhost/foo provided")
    public void overriddenUrlEndsInSlash() throws Exception {
        InstanceMetadataHook.get(URI.create("http://localhost/foo"), "public-hostname");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "path must end with '/'; iam/security-credentials provided")
    public void listPathEndsInSlash() throws Exception {
        InstanceMetadataHook.list("iam/security-credentials");
    }

    @Test(timeOut = 3000)
    public void whenMetadataServiceIsntRunningWeDontHangMoreThan3Seconds() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AT_START));
        server.play();
        try {
            assertNull(InstanceMetadataHook.get(server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI(),
                    "public-hostname"));

        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "GET /latest/meta-data/public-hostname HTTP/1.1");
            server.shutdown();
        }
    }

    public void getWhenMetadataPresent() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("ec2-50-17-85-234.compute-1.amazonaws.com"));
        server.play();
        try {
            assertEquals(
                    InstanceMetadataHook.get(server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI(),
                            "public-hostname"), "ec2-50-17-85-234.compute-1.amazonaws.com");

        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "GET /latest/meta-data/public-hostname HTTP/1.1");
            server.shutdown();
        }
    }

    public void listSplitsOnNewline() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("route53-readonly\nbooberry"));
        server.play();
        try {
            assertEquals(InstanceMetadataHook.list(server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI(),
                    "iam/security-credentials/"), ImmutableList.of("route53-readonly", "booberry"));

        } finally {
            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/ HTTP/1.1");
            server.shutdown();
        }
    }
}
