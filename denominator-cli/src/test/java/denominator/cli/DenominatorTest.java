package denominator.cli;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import denominator.DNSApiManager;
import denominator.Provider;
import denominator.cli.Denominator.ListProviders;
import denominator.cli.Denominator.ZoneList;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetAdd;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetList;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetRemove;
import denominator.cli.ResourceRecordSetCommands.ResourceRecordSetReplace;
import denominator.dynect.DynECTProvider;
import denominator.mock.MockProvider;
import denominator.route53.Route53Provider;
import denominator.ultradns.UltraDNSProvider;
@Test
public class DenominatorTest {

    @Test(description = "denominator -p mock providers")
    public void listsAllProvidersWithCredentials() {
        ImmutableList<Provider> providers = ImmutableList.of(new MockProvider(), new DynECTProvider(),
                new Route53Provider(), new UltraDNSProvider());
        assertEquals(ListProviders.providerAndCredentialsTable(providers), Joiner.on('\n').join(
                "provider             credential type  credential arguments",
                "mock                ",
                "dynect               password         customer username password",
                "route53              accessKey        accessKey secretKey",
                "route53              session          accessKey secretKey sessionToken",
                "ultradns             password         username password", ""));
    }

    DNSApiManager mgr = denominator.Denominator.create(new MockProvider());

    @Test(description = "denominator -p mock zone list")
    public void testZoneList() {
        assertEquals(Joiner.on('\n').join(new ZoneList().doRun(mgr)), "denominator.io.");
    }

    @Test(description = "denominator -p mock record -z denominator.io. list")
    public void testResourceRecordSetList() {
        ResourceRecordSetList command = new ResourceRecordSetList();
        command.zoneName = "denominator.io.";
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                "denominator.io.                                   NS     86400 ns1.denominator.io.",
                "denominator.io.                                   SOA    3600  ns1.denominator.io. admin.denominator.io. 1 3600 600 604800 60",
                "www.denominator.io.                               CNAME  3600  www1.denominator.io.",
                "www1.denominator.io.                              A      3600  1.1.1.1",
                "www1.denominator.io.                              A      3600  1.1.1.2",
                "www2.denominator.io.                              A      3600  2.2.2.2"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A -d 1.1.1.1 -d 1.1.1.2")
    public void testResourceRecordSetAdd() {
        ResourceRecordSetAdd command = new ResourceRecordSetAdd();
        command.zoneName = "denominator.io.";
        command.name = "www1.denominator.io.";
        command.type = "A";
        command.values = ImmutableList.of("1.1.1.1", "1.1.1.2");
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=1.1.1.1},{address=1.1.1.2}]",
                ";; ok"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. add -n www1.denominator.io. -t A --ttl 3600 -d 1.1.1.1 -d 1.1.1.2")
    public void testResourceRecordSetAddWithTTL() {
        ResourceRecordSetAdd command = new ResourceRecordSetAdd();
        command.zoneName = "denominator.io.";
        command.name = "www1.denominator.io.";
        command.type = "A";
        command.ttl = 3600;
        command.values = ImmutableList.of("1.1.1.1", "1.1.1.2");
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. adding to rrset www1.denominator.io. A values: [{address=1.1.1.1},{address=1.1.1.2}] applying ttl 3600",
                ";; ok"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. replace -n www1.denominator.io. -t A -d 1.1.1.1 -d 1.1.1.2")
    public void testResourceRecordSetReplace() {
        ResourceRecordSetReplace command = new ResourceRecordSetReplace();
        command.zoneName = "denominator.io.";
        command.name = "www1.denominator.io.";
        command.type = "A";
        command.values = ImmutableList.of("1.1.1.1", "1.1.1.2");
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. replacing rrset www1.denominator.io. A with values: [{address=1.1.1.1},{address=1.1.1.2}]",
                ";; ok"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. replace -n www1.denominator.io. -t A --ttl 3600 -d 1.1.1.1 -d 1.1.1.2")
    public void testResourceRecordSetReplaceWithTTL() {
        ResourceRecordSetReplace command = new ResourceRecordSetReplace();
        command.zoneName = "denominator.io.";
        command.name = "www1.denominator.io.";
        command.type = "A";
        command.ttl = 3600;
        command.values = ImmutableList.of("1.1.1.1", "1.1.1.2");
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. replacing rrset www1.denominator.io. A with values: [{address=1.1.1.1},{address=1.1.1.2}] and ttl 3600",
                ";; ok"));
    }

    @Test(description = "denominator -p mock record -z denominator.io. remove -n www1.denominator.io. -t A -d 1.1.1.1 -d 1.1.1.2")
    public void testResourceRecordSetRemove() {
        ResourceRecordSetRemove command = new ResourceRecordSetRemove();
        command.zoneName = "denominator.io.";
        command.name = "www1.denominator.io.";
        command.type = "A";
        command.values = ImmutableList.of("1.1.1.1", "1.1.1.2");
        assertEquals(Joiner.on('\n').join(command.doRun(mgr)), Joiner.on('\n').join(
                ";; in zone denominator.io. removing from rrset www1.denominator.io. A values: [{address=1.1.1.1},{address=1.1.1.2}]",
                ";; ok"));
    }
}
