package denominator.hook;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URI;

import static com.squareup.okhttp.mockwebserver.SocketPolicy.SHUTDOWN_INPUT_AT_END;
import static denominator.assertj.MockWebServerAssertions.assertThat;
import static org.junit.Assert.assertNull;


public class InstanceMetadataHookTest {

  @Rule
  public final MockWebServer server = new MockWebServer();
  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void overriddenUrlEndsInSlash() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("metadataService must end with '/'; http://localhost/foo provided");

    InstanceMetadataHook.get(URI.create("http://localhost/foo"), "public-hostname");
  }

  @Test
  public void listPathEndsInSlash() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("path must end with '/'; iam/security-credentials provided");

    InstanceMetadataHook.list("iam/security-credentials");
  }

  @Test
  public void whenMetadataServiceIsntRunningWeDontHangMoreThan3Seconds() throws Exception {
    server.enqueue(new MockResponse().setSocketPolicy(SHUTDOWN_INPUT_AT_END));

    assertNull(InstanceMetadataHook
                   .get(server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI(),
                        "public-hostname"));

    assertThat(server.takeRequest())
        .hasMethod("GET")
        .hasPath("/latest/meta-data/public-hostname");
  }

  @Test
  public void getWhenMetadataPresent() throws Exception {
    server.enqueue(new MockResponse().setBody("ec2-50-17-85-234.compute-1.amazonaws.com"));

    assertThat(
        InstanceMetadataHook
            .get(server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI(),
                 "public-hostname")).isEqualTo(
        "ec2-50-17-85-234.compute-1.amazonaws.com");

    assertThat(server.takeRequest())
        .hasMethod("GET")
        .hasPath("/latest/meta-data/public-hostname");
  }

  @Test
  public void listSplitsOnNewline() throws Exception {
    server.enqueue(new MockResponse().setBody("route53-readonly\nbooberry"));

    Assertions.assertThat(InstanceMetadataHook
                              .list(
                                  server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI(),
                                  "iam/security-credentials/"))
        .containsExactly("route53-readonly", "booberry");

    assertThat(server.takeRequest())
        .hasMethod("GET")
        .hasPath("/latest/meta-data/iam/security-credentials/");
  }
}
