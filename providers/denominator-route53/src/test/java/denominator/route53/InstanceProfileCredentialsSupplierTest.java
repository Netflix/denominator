package denominator.route53;

import static com.google.mockwebserver.SocketPolicy.DISCONNECT_AT_START;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Credentials;
import denominator.Credentials.MapCredentials;
import denominator.CredentialsConfiguration;
import denominator.route53.InstanceProfileCredentialsSupplier.ReadFirstInstanceProfileCredentialsOrNull;

@Test
public class InstanceProfileCredentialsSupplierTest {
    Credentials sessionCredentials = MapCredentials.from(ImmutableMap.of("accessKey", "AAAAA", "secretKey", "SSSSSSS",
            "sessionToken", "TTTTTTT"));

    public void sessionCredentialsValidForRoute53() {
        CredentialsConfiguration.checkValidForProvider(sessionCredentials, new Route53Provider());
    }

    public void whenInstanceProfileCredentialsInMetadataServiceReturnMapCredentials() throws Exception {
        String securityCredentialsJson = new String(ByteStreams.toByteArray(getClass().getResourceAsStream(
                "/security-credentials.json")));
        MockWebServer server = new MockWebServer();
        try {

            server.enqueue(new MockResponse().setBody("route53-readonly"));
            server.enqueue(new MockResponse().setBody(securityCredentialsJson));
            server.play();

            assertEquals(new InstanceProfileCredentialsSupplier(new ReadFirstInstanceProfileCredentialsOrNull(server
                    .getUrl("/").toString())).get(), sessionCredentials);

            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/ HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/route53-readonly HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test(timeOut = 3000)
    public void whenMetadataServiceIsntRunningWeDontHangMoreThan3Seconds() throws Exception {
        MockWebServer server = new MockWebServer();
        try {
            server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AT_START));
            server.play();

            assertNull(new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl("/").toString()).get());

            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/ HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    public void whenNoInstanceProfileCredentialsInMetadataServiceReturnNull() throws Exception {
        MockWebServer server = new MockWebServer();
        try {

            server.enqueue(new MockResponse().setBody(""));
            server.play();

            assertNull(new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl("/").toString()).get());

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
        try {

            server.enqueue(new MockResponse().setBody("route53-readonly"));
            server.enqueue(new MockResponse().setBody(securityCredentialsJson));
            server.play();

            assertEquals(new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl("/").toString()).get(),
                    securityCredentialsJson);

            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/ HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/route53-readonly HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    public void whenMultipleInstanceProfileCredentialsInMetadataServiceReturnJsonFromFirst() throws Exception {
        String securityCredentialsJson = new String(ByteStreams.toByteArray(getClass().getResourceAsStream(
                "/security-credentials.json")));
        MockWebServer server = new MockWebServer();
        try {

            server.enqueue(new MockResponse().setBody("route53-readonly\nbooberry"));
            server.enqueue(new MockResponse().setBody(securityCredentialsJson));
            server.play();

            assertEquals(new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl("/").toString()).get(),
                    securityCredentialsJson);

            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/ HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /latest/meta-data/iam/security-credentials/route53-readonly HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    public void testParseInstanceProfileCredentialsFromJsonWhenNull() {
        assertEquals(InstanceProfileCredentialsSupplier.parseJson(null), ImmutableMap.of());
    }

    public void testParseInstanceProfileCredentialsFromJsonWhenWrongKeys() {
        assertEquals(InstanceProfileCredentialsSupplier.parseJson("{\"Code\" : \"Failure\"}"), ImmutableMap.of());
    }

    public void testParseInstanceProfileCredentialsFromJsonWhenAccessAndSecretPresent() {
        assertEquals(
                InstanceProfileCredentialsSupplier
                        .parseJson("{\"AccessKeyId\" : \"AAAAA\",\"SecretAccessKey\" : \"SSSSSSS\"}"),
                ImmutableMap.of("accessKey", "AAAAA", "secretKey", "SSSSSSS"));
    }

    public void testParseInstanceProfileCredentialsFromJsonWhenAccessSecretAndTokenPresent() {
        assertEquals(
                InstanceProfileCredentialsSupplier
                        .parseJson("{\"AccessKeyId\" : \"AAAAA\",\"SecretAccessKey\" : \"SSSSSSS\", \"Token\" : \"TTTTTTT\"}"),
                ImmutableMap.of("accessKey", "AAAAA", "secretKey", "SSSSSSS", "sessionToken", "TTTTTTT"));
    }
}
