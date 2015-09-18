package denominator.cli;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import denominator.DNSApiManager;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetAdd;
import denominator.hook.InstanceMetadataHook;
import denominator.mock.MockProvider;

import static com.squareup.okhttp.mockwebserver.SocketPolicy.SHUTDOWN_INPUT_AT_END;
import static denominator.assertj.MockWebServerAssertions.assertThat;

public class DataFromInstanceMetadataHookTest {

  @Rule
  public final MockWebServer server = new MockWebServer();
  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  DNSApiManager mgr = denominator.Denominator.create(new MockProvider());

  @Test(timeout = 3000)
  public void testInstanceMetadataHookDoesntHangMoreThan3Seconds() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("could not retrieve public-hostname from http://");

    server.enqueue(new MockResponse().setSocketPolicy(SHUTDOWN_INPUT_AT_END));

    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "CNAME";
    command.ec2PublicHostname = true;
    command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

    command.doRun(mgr);
  }

  @Test
  // denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t CNAME --ec2-public-hostname"
  public void testEC2PublicHostnameFlag() throws Exception {
    server.enqueue(new MockResponse().setBody("ec2-50-17-85-234.compute-1.amazonaws.com"));

    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "CNAME";
    command.ec2PublicHostname = true;
    command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

    assertThat(command.doRun(mgr))
        .containsExactly(
            ";; in zone denominator.io. adding to rrset www1.denominator.io. CNAME values: [{cname=ec2-50-17-85-234.compute-1.amazonaws.com}]",
            ";; ok");

    assertThat(server.takeRequest())
        .hasMethod("GET")
        .hasPath("/latest/meta-data/public-hostname");
  }

  @Test
  // denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A --ec2-public-ipv4
  public void testEC2PublicIpv4Flag() throws Exception {
    server.enqueue(new MockResponse().setBody("50.17.85.234"));

    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.ec2PublicIpv4 = true;
    command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

    assertThat(command.doRun(mgr))
        .containsExactly(
            ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=50.17.85.234}]",
            ";; ok");

    assertThat(server.takeRequest())
        .hasMethod("GET")
        .hasPath("/latest/meta-data/public-ipv4");

  }

  @Test(timeout = 3000)
  // denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A --ec2-public-ipv4
  public void testEC2PublicIpv4FlagWhenFailsDoesntHangMoreThan3Seconds() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("could not retrieve public-ipv4 from http://");

    server.enqueue(new MockResponse().setSocketPolicy(SHUTDOWN_INPUT_AT_END));

    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.ec2PublicIpv4 = true;
    command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

    command.doRun(mgr);
  }


  @Test
  // denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t CNAME --ec2-local-hostname
  public void testEC2LocalHostnameFlag() throws Exception {
    server.enqueue(new MockResponse().setBody("domU-12-31-39-02-14-35.compute-1.internal"));

    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "CNAME";
    command.ec2LocalHostname = true;
    command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

    assertThat(command.doRun(mgr))
        .containsExactly(
            ";; in zone denominator.io. adding to rrset www1.denominator.io. CNAME values: [{cname=domU-12-31-39-02-14-35.compute-1.internal}]",
            ";; ok");

    assertThat(server.takeRequest())
        .hasMethod("GET")
        .hasPath("/latest/meta-data/local-hostname");
  }

  @Test
  // denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A --ec2-local-ipv4
  public void testEC2LocalIpv4Flag() throws Exception {
    server.enqueue(new MockResponse().setBody("10.248.27.195"));

    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.ec2LocalIpv4 = true;
    command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

    assertThat(command.doRun(mgr))
        .containsExactly(
            ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=10.248.27.195}]",
            ";; ok");

    assertThat(server.takeRequest())
        .hasMethod("GET")
        .hasPath("/latest/meta-data/local-ipv4");
  }

  @Test(timeout = 3000)
  // denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A --ec2-local-ipv4
  public void testEC2LocalIpv4FlagWhenFailsDoesntHangMoreThan3Seconds() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("could not retrieve local-ipv4 from http://");

    server.enqueue(new MockResponse().setSocketPolicy(SHUTDOWN_INPUT_AT_END));

    ResourceRecordSetAdd command = new ResourceRecordSetAdd();
    command.zoneIdOrName = "denominator.io.";
    command.name = "www1.denominator.io.";
    command.type = "A";
    command.ec2LocalIpv4 = true;
    command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

    command.doRun(mgr);
  }
}

