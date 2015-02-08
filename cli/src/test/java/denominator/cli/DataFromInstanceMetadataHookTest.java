package denominator.cli;

import com.google.common.base.Joiner;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.testng.annotations.Test;

import denominator.DNSApiManager;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetAdd;
import denominator.hook.InstanceMetadataHook;
import denominator.mock.MockProvider;

import static com.squareup.okhttp.mockwebserver.SocketPolicy.SHUTDOWN_INPUT_AT_END;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Test
public class DataFromInstanceMetadataHookTest {

    DNSApiManager mgr = denominator.Denominator.create(new MockProvider());

    @Test(timeOut = 3000, expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "could not retrieve public-hostname from http://.*/latest/meta-data/")
    public void testInstanceMetadataHookDoesntHangMoreThan3Seconds() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setSocketPolicy(SHUTDOWN_INPUT_AT_END));
        server.play();
        try {

            ResourceRecordSetAdd command = new ResourceRecordSetAdd();
            command.zoneIdOrName = "denominator.io.";
            command.name = "www1.denominator.io.";
            command.type = "CNAME";
            command.ec2PublicHostname = true;
            command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

            command.doRun(mgr);
            fail("should have erred");

        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "GET /latest/meta-data/public-hostname HTTP/1.1");
            server.shutdown();
        }
    }

    @Test(description = "denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t CNAME --ec2-public-hostname")
    public void testEC2PublicHostnameFlag() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("ec2-50-17-85-234.compute-1.amazonaws.com"));
        server.play();
        try {

            ResourceRecordSetAdd command = new ResourceRecordSetAdd();
            command.zoneIdOrName = "denominator.io.";
            command.name = "www1.denominator.io.";
            command.type = "CNAME";
            command.ec2PublicHostname = true;
            command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

            assertEquals(
                    Joiner.on('\n').join(command.doRun(mgr)),
                    Joiner.on('\n')
                            .join(";; in zone denominator.io. adding to rrset www1.denominator.io. CNAME values: [{cname=ec2-50-17-85-234.compute-1.amazonaws.com}]",
                                  ";; ok"));

        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "GET /latest/meta-data/public-hostname HTTP/1.1");
            server.shutdown();
        }
    }

    @Test(description = "denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A --ec2-public-ipv4")
    public void testEC2PublicIpv4Flag() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("50.17.85.234"));
        server.play();
        try {

            ResourceRecordSetAdd command = new ResourceRecordSetAdd();
            command.zoneIdOrName = "denominator.io.";
            command.name = "www1.denominator.io.";
            command.type = "A";
            command.ec2PublicIpv4 = true;
            command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

            assertEquals(
                    Joiner.on('\n').join(command.doRun(mgr)),
                    Joiner.on('\n')
                            .join(";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=50.17.85.234}]",
                                  ";; ok"));

        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "GET /latest/meta-data/public-ipv4 HTTP/1.1");
            server.shutdown();
        }
    }
    
    @Test(description = "denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A --ec2-public-ipv4", timeOut = 3000, 
            expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "could not retrieve public-ipv4 from http://.*/latest/meta-data/")
    public void testEC2PublicIpv4FlagWhenFailsDoesntHangMoreThan3Seconds() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setSocketPolicy(SHUTDOWN_INPUT_AT_END));
        server.play();
        try {

            ResourceRecordSetAdd command = new ResourceRecordSetAdd();
            command.zoneIdOrName = "denominator.io.";
            command.name = "www1.denominator.io.";
            command.type = "A";
            command.ec2PublicIpv4 = true;
            command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

            command.doRun(mgr);
            fail("should have erred");

        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "GET /latest/meta-data/public-ipv4 HTTP/1.1");
            server.shutdown();
        }
    }



    @Test(description = "denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t CNAME --ec2-local-hostname")
    public void testEC2LocalHostnameFlag() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("domU-12-31-39-02-14-35.compute-1.internal"));
        server.play();
        try {

            ResourceRecordSetAdd command = new ResourceRecordSetAdd();
            command.zoneIdOrName = "denominator.io.";
            command.name = "www1.denominator.io.";
            command.type = "CNAME";
            command.ec2LocalHostname = true;
            command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

            assertEquals(
                    Joiner.on('\n').join(command.doRun(mgr)),
                    Joiner.on('\n')
                            .join(";; in zone denominator.io. adding to rrset www1.denominator.io. CNAME values: [{cname=domU-12-31-39-02-14-35.compute-1.internal}]",
                                  ";; ok"));

        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "GET /latest/meta-data/local-hostname HTTP/1.1");
            server.shutdown();
        }
    }

    @Test(description = "denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A --ec2-local-ipv4")
    public void testEC2LocalIpv4Flag() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("10.248.27.195"));
        server.play();
        try {

            ResourceRecordSetAdd command = new ResourceRecordSetAdd();
            command.zoneIdOrName = "denominator.io.";
            command.name = "www1.denominator.io.";
            command.type = "A";
            command.ec2LocalIpv4 = true;
            command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

            assertEquals(
                    Joiner.on('\n').join(command.doRun(mgr)),
                    Joiner.on('\n')
                            .join(";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=10.248.27.195}]",
                                  ";; ok"));

        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "GET /latest/meta-data/local-ipv4 HTTP/1.1");
            server.shutdown();
        }
    }
    
    @Test(description = "denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A --ec2-local-ipv4", timeOut = 3000, 
            expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "could not retrieve local-ipv4 from http://.*/latest/meta-data/")
    public void testEC2LocalIpv4FlagWhenFailsDoesntHangMoreThan3Seconds() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setSocketPolicy(SHUTDOWN_INPUT_AT_END));
        server.play();
        try {

            ResourceRecordSetAdd command = new ResourceRecordSetAdd();
            command.zoneIdOrName = "denominator.io.";
            command.name = "www1.denominator.io.";
            command.type = "A";
            command.ec2LocalIpv4 = true;
            command.metadataService = server.getUrl(InstanceMetadataHook.DEFAULT_URI.getPath()).toURI();

            command.doRun(mgr);
            fail("should have erred");

        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "GET /latest/meta-data/local-ipv4 HTTP/1.1");
            server.shutdown();
        }
    }
}

