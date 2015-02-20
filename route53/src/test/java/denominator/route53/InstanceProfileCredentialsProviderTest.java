package denominator.route53;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import denominator.Credentials.MapCredentials;
import denominator.hook.InstanceMetadataHook;
import denominator.route53.InstanceProfileCredentialsProvider.ReadFirstInstanceProfileCredentialsOrNull;

import static denominator.assertj.MockWebServerAssertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Test
public class InstanceProfileCredentialsProviderTest {

  MockWebServer server;

  String securityCredentials = "{\n"
                               + "  \"Code\" : \"Success\",\n"
                               + "  \"LastUpdated\" : \"2013-02-26T02:03:57Z\",\n"
                               + "  \"Type\" : \"AWS-HMAC\",\n"
                               + "  \"AccessKeyId\" : \"AAAAA\",\n"
                               + "  \"SecretAccessKey\" : \"SSSSSSS\",\n"
                               + "  \"Token\" : \"TTTTTTT\",\n"
                               + "  \"Expiration\" : \"2013-02-26T08:12:23Z\"\n"
                               + "}";

  public void whenInstanceProfileCredentialsInMetadataServiceReturnMapCredentials()
      throws Exception {
    server.enqueue(new MockResponse().setBody("route53-readonly"));
    server.enqueue(new MockResponse().setBody(securityCredentials));

    Map<String, String> sessionCredentials = new LinkedHashMap<String, String>();
    sessionCredentials.put("accessKey", "AAAAA");
    sessionCredentials.put("secretKey", "SSSSSSS");
    sessionCredentials.put("sessionToken", "TTTTTTT");

    assertThat(new InstanceProfileCredentialsProvider(
        new ReadFirstInstanceProfileCredentialsOrNull(server
                                                          .getUrl(
                                                              InstanceMetadataHook.DEFAULT_URI
                                                                  .getPath())
                                                          .toURI())).get(
        new Route53Provider()))
        .isEqualTo(MapCredentials.from(sessionCredentials));

    assertThat(server.takeRequest()).hasPath("/latest/meta-data/iam/security-credentials/");
    assertThat(server.takeRequest())
        .hasPath("/latest/meta-data/iam/security-credentials/route53-readonly");
  }

  public void whenNoInstanceProfileCredentialsInMetadataServiceReturnNull() throws Exception {
    server.enqueue(new MockResponse().setBody(""));

    assertNull(new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl(
        InstanceMetadataHook.DEFAULT_URI.getPath()).toURI()).get());

    assertThat(server.takeRequest()).hasPath("/latest/meta-data/iam/security-credentials/");
  }

  public void whenInstanceProfileCredentialsInMetadataServiceReturnJson() throws Exception {
    server.enqueue(new MockResponse().setBody("route53-readonly"));
    server.enqueue(new MockResponse().setBody(securityCredentials));

    assertEquals(
        new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl(
            InstanceMetadataHook.DEFAULT_URI.getPath()).toURI()).get(), securityCredentials);

    assertThat(server.takeRequest()).hasPath("/latest/meta-data/iam/security-credentials/");
    assertThat(server.takeRequest()).hasPath(
        "/latest/meta-data/iam/security-credentials/route53-readonly");
  }

  public void whenMultipleInstanceProfileCredentialsInMetadataServiceReturnJsonFromFirst()
      throws Exception {
    server.enqueue(new MockResponse().setBody("route53-readonly\nbooberry"));
    server.enqueue(new MockResponse().setBody(securityCredentials));

    assertEquals(
        new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl(
            InstanceMetadataHook.DEFAULT_URI.getPath()).toURI()).get(), securityCredentials);

    assertThat(server.takeRequest()).hasPath("/latest/meta-data/iam/security-credentials/");
    assertThat(server.takeRequest()).hasPath(
        "/latest/meta-data/iam/security-credentials/route53-readonly");
  }

  public void testParseInstanceProfileCredentialsFromJsonWhenNull() {
    assertThat(InstanceProfileCredentialsProvider.parseJson(null)).isEmpty();
  }

  public void testParseInstanceProfileCredentialsFromJsonWhenWrongKeys() {
    assertThat(InstanceProfileCredentialsProvider.parseJson("{\"Code\" : \"Failure\"}")).isEmpty();
  }

  public void testParseInstanceProfileCredentialsFromJsonWhenAccessAndSecretPresent() {
    assertThat(
        InstanceProfileCredentialsProvider
            .parseJson(
                "{\"AccessKeyId\" : \"AAAAA\",\"SecretAccessKey\" : \"SSSSSSS\"}"))
        .containsEntry("accessKey", "AAAAA")
        .containsEntry("secretKey", "SSSSSSS");
  }

  public void testParseInstanceProfileCredentialsFromJsonWhenAccessSecretAndTokenPresent() {
    assertThat(
        InstanceProfileCredentialsProvider
            .parseJson(
                "{\"AccessKeyId\" : \"AAAAA\",\"SecretAccessKey\" : \"SSSSSSS\", \"Token\" : \"TTTTTTT\"}"))
        .containsEntry("accessKey", "AAAAA")
        .containsEntry("secretKey", "SSSSSSS")
        .containsEntry("sessionToken", "TTTTTTT");
  }

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockWebServer();
    server.play();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
