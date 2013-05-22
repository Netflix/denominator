package denominator.route53;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Credentials;
import denominator.Credentials.MapCredentials;
import denominator.hook.InstanceMetadataHook;
import denominator.route53.InstanceProfileCredentialsProvider.ReadFirstInstanceProfileCredentialsOrNull;

@Test
public class InstanceProfileCredentialsProviderTest {
    Credentials sessionCredentials = MapCredentials.from(ImmutableMap.of("accessKey", "AAAAA", "secretKey", "SSSSSSS",
            "sessionToken", "TTTTTTT"));

    public void whenInstanceProfileCredentialsInMetadataServiceReturnMapCredentials() throws Exception {
        String securityCredentialsJson = new String(ByteStreams.toByteArray(getClass().getResourceAsStream(
                "/security-credentials.json")));
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("route53-readonly"));
        server.enqueue(new MockResponse().setBody(securityCredentialsJson));
        server.play();

        try {
            assertEquals(new InstanceProfileCredentialsProvider(new ReadFirstInstanceProfileCredentialsOrNull(server
                    .getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI())).get(new Route53Provider()),
                    sessionCredentials);
        } finally {
            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/ HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/route53-readonly HTTP/1.1");
            server.shutdown();
        }
    }

    public void whenNoInstanceProfileCredentialsInMetadataServiceReturnNull() throws Exception {
        MockWebServer server = new MockWebServer();
        try {

            server.enqueue(new MockResponse().setBody(""));
            server.play();

            assertNull(new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl(
                    InstanceMetadataHook.DEFAULT_URI.getPath()).toURI()).get());

            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/ HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    public void whenInstanceProfileCredentialsInMetadataServiceReturnJson() throws Exception {
        String securityCredentialsJson = new String(ByteStreams.toByteArray(getClass().getResourceAsStream(
                "/security-credentials.json")));
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("route53-readonly"));
        server.enqueue(new MockResponse().setBody(securityCredentialsJson));
        server.play();

        try {
            assertEquals(
                    new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl(
                            InstanceMetadataHook.DEFAULT_URI.getPath()).toURI()).get(), securityCredentialsJson);
        } finally {
            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/ HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/route53-readonly HTTP/1.1");
            server.shutdown();
        }
    }

    public void whenMultipleInstanceProfileCredentialsInMetadataServiceReturnJsonFromFirst() throws Exception {
        String securityCredentialsJson = new String(ByteStreams.toByteArray(getClass().getResourceAsStream(
                "/security-credentials.json")));
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("route53-readonly\nbooberry"));
        server.enqueue(new MockResponse().setBody(securityCredentialsJson));
        server.play();
        try {
            assertEquals(
                    new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl(
                            InstanceMetadataHook.DEFAULT_URI.getPath()).toURI()).get(), securityCredentialsJson);
        } finally {
            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/ HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/route53-readonly HTTP/1.1");
            server.shutdown();
        }
    }

    public void testParseInstanceProfileCredentialsFromJsonWhenNull() {
        assertEquals(InstanceProfileCredentialsProvider.parseJson(null), ImmutableMap.of());
    }

    public void testParseInstanceProfileCredentialsFromJsonWhenWrongKeys() {
        assertEquals(InstanceProfileCredentialsProvider.parseJson("{\"Code\" : \"Failure\"}"), ImmutableMap.of());
    }

    public void testParseInstanceProfileCredentialsFromJsonWhenAccessAndSecretPresent() {
        assertEquals(
                InstanceProfileCredentialsProvider
                        .parseJson("{\"AccessKeyId\" : \"AAAAA\",\"SecretAccessKey\" : \"SSSSSSS\"}"),
                ImmutableMap.of("accessKey", "AAAAA", "secretKey", "SSSSSSS"));
    }

    public void testParseInstanceProfileCredentialsFromJsonWhenAccessSecretAndTokenPresent() {
        assertEquals(
                InstanceProfileCredentialsProvider
                        .parseJson("{\"AccessKeyId\" : \"AAAAA\",\"SecretAccessKey\" : \"SSSSSSS\", \"Token\" : \"TTTTTTT\"}"),
                ImmutableMap.of("accessKey", "AAAAA", "secretKey", "SSSSSSS", "sessionToken", "TTTTTTT"));
    }
}
