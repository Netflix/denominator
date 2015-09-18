package denominator.route53;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.Rule;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import denominator.Credentials.MapCredentials;
import denominator.hook.InstanceMetadataHook;
import denominator.route53.InstanceProfileCredentialsProvider.ReadFirstInstanceProfileCredentialsOrNull;

import static denominator.assertj.MockWebServerAssertions.assertThat;

public class InstanceProfileCredentialsProviderTest {

  @Rule
  public MockWebServer server = new MockWebServer();

  @Test
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

  @Test
  public void whenNoInstanceProfileCredentialsInMetadataServiceReturnNull() throws Exception {
    server.enqueue(new MockResponse().setBody(""));

    assertThat(new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl(
        InstanceMetadataHook.DEFAULT_URI.getPath()).toURI()).get()).isNull();

    assertThat(server.takeRequest()).hasPath("/latest/meta-data/iam/security-credentials/");
  }

  @Test
  public void whenInstanceProfileCredentialsInMetadataServiceReturnJson() throws Exception {
    server.enqueue(new MockResponse().setBody("route53-readonly"));
    server.enqueue(new MockResponse().setBody(securityCredentials));

    assertThat(new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl(
        InstanceMetadataHook.DEFAULT_URI.getPath()).toURI()).get())
        .isEqualTo(securityCredentials);

    assertThat(server.takeRequest()).hasPath("/latest/meta-data/iam/security-credentials/");
    assertThat(server.takeRequest()).hasPath(
        "/latest/meta-data/iam/security-credentials/route53-readonly");
  }

  @Test
  public void whenMultipleInstanceProfileCredentialsInMetadataServiceReturnJsonFromFirst()
      throws Exception {
    server.enqueue(new MockResponse().setBody("route53-readonly\nbooberry"));
    server.enqueue(new MockResponse().setBody(securityCredentials));

    assertThat(new ReadFirstInstanceProfileCredentialsOrNull(server.getUrl(
        InstanceMetadataHook.DEFAULT_URI.getPath()).toURI()).get())
        .isEqualTo(securityCredentials);

    assertThat(server.takeRequest()).hasPath("/latest/meta-data/iam/security-credentials/");
    assertThat(server.takeRequest()).hasPath(
        "/latest/meta-data/iam/security-credentials/route53-readonly");
  }

  @Test
  public void testParseInstanceProfileCredentialsFromJsonWhenNull() {
    assertThat(InstanceProfileCredentialsProvider.parseJson(null)).isEmpty();
  }

  @Test
  public void testParseInstanceProfileCredentialsFromJsonWhenWrongKeys() {
    assertThat(InstanceProfileCredentialsProvider.parseJson("{\"Code\" : \"Failure\"}")).isEmpty();
  }

  @Test
  public void testParseInstanceProfileCredentialsFromJsonWhenAccessAndSecretPresent() {
    assertThat(
        InstanceProfileCredentialsProvider
            .parseJson(
                "{\"AccessKeyId\" : \"AAAAA\",\"SecretAccessKey\" : \"SSSSSSS\"}"))
        .containsEntry("accessKey", "AAAAA")
        .containsEntry("secretKey", "SSSSSSS");
  }

  @Test
  public void testParseInstanceProfileCredentialsFromJsonWhenAccessSecretAndTokenPresent() {
    assertThat(
        InstanceProfileCredentialsProvider
            .parseJson(
                "{\"AccessKeyId\" : \"AAAAA\",\"SecretAccessKey\" : \"SSSSSSS\", \"Token\" : \"TTTTTTT\"}"))
        .containsEntry("accessKey", "AAAAA")
        .containsEntry("secretKey", "SSSSSSS")
        .containsEntry("sessionToken", "TTTTTTT");
  }

  String securityCredentials = "{\n"
                               + "  \"Code\" : \"Success\",\n"
                               + "  \"LastUpdated\" : \"2013-02-26T02:03:57Z\",\n"
                               + "  \"Type\" : \"AWS-HMAC\",\n"
                               + "  \"AccessKeyId\" : \"AAAAA\",\n"
                               + "  \"SecretAccessKey\" : \"SSSSSSS\",\n"
                               + "  \"Token\" : \"TTTTTTT\",\n"
                               + "  \"Expiration\" : \"2013-02-26T08:12:23Z\"\n"
                               + "}";
}
