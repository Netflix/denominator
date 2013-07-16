package denominator.cli.groups;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.kohsuke.args4j.CmdLineException;
import org.testng.annotations.Test;

import denominator.cli.Denominator;

@Test
public class ResourceRecordSetGroupTest {

    @Test(description = "denominator -p mock rrset -z denominator.io get")
    public void rrsets() throws IOException, CmdLineException {
        StringWriter writer = new StringWriter();
        Denominator.execute(writer, "-p", "mock", "rrset", "-z", "denominator.io.", "get");

        assertEquals(writer.toString(), ""//
                + "name: denominator.io.\n"//
                + "type: NS\n"//
                + "ttl: 86400\n"//
                + "rdata: [ns1.denominator.io.]\n"//
                + "---\n"//
                + "name: denominator.io.\n"//
                + "type: SOA\n"//
                + "ttl: 3600\n"//
                + "rdata: [ns1.denominator.io. admin.denominator.io. 1 3600 600 604800 60]\n"//
                + "---\n"//
                + "name: www.denominator.io.\n"//
                + "type: CNAME\n"//
                + "ttl: 3600\n"//
                + "rdata: [www1.denominator.io.]\n"//
                + "---\n"//
                + "name: www.geo.denominator.io.\n"//
                + "type: CNAME\n"//
                + "qualifier: alazona\n"//
                + "ttl: 300\n"//
                + "profiles:\n"//
                + "- type: geo\n"//
                + "  regions:\n"//
                + "    United States (US): [Alaska, Arizona]\n"//
                + "rdata: [a.denominator.io.]\n"//
                + "---\n"//
                + "name: www.geo.denominator.io.\n"//
                + "type: CNAME\n"//
                + "qualifier: antarctica\n"//
                + "ttl: 0\n"//
                + "profiles:\n"//
                + "- type: geo\n"//
                + "  regions:\n"//
                + "    Antarctica: [Bouvet Island, French Southern Territories, Antarctica]\n"//
                + "rdata: [c.denominator.io.]\n"//
                + "---\n"//
                + "name: www.geo.denominator.io.\n"//
                + "type: CNAME\n"//
                + "qualifier: columbador\n"//
                + "ttl: 86400\n"//
                + "profiles:\n"//
                + "- type: geo\n"//
                + "  regions:\n"//
                + "    South America: [Colombia, Ecuador]\n"//
                + "rdata: [b.denominator.io.]\n"//
                + "---\n"//
                + "name: www.weighted.denominator.io.\n"//
                + "type: CNAME\n"//
                + "qualifier: EU-West\n"//
                + "ttl: 0\n"//
                + "profiles:\n"//
                + "- {type: weighted, weight: 1}\n"//
                + "rdata: [c.denominator.io.]\n"//
                + "---\n"//
                + "name: www.weighted.denominator.io.\n"//
                + "type: CNAME\n"//
                + "qualifier: US-East\n"//
                + "ttl: 0\n"//
                + "profiles:\n"//
                + "- {type: weighted, weight: 1}\n"//
                + "rdata: [b.denominator.io.]\n"//
                + "---\n"//
                + "name: www.weighted.denominator.io.\n"//
                + "type: CNAME\n"//
                + "qualifier: US-West\n"//
                + "ttl: 0\n"//
                + "profiles:\n"//
                + "- {type: weighted, weight: 1}\n"//
                + "rdata: [a.denominator.io.]\n"//
                + "---\n"//
                + "name: www1.denominator.io.\n"//
                + "type: A\n"//
                + "ttl: 3600\n"//
                + "rdata: [192.0.2.1, 192.0.2.2]\n"//
                + "---\n"//
                + "name: www2.denominator.io.\n"//
                + "type: A\n"//
                + "ttl: 3600\n"//
                + "rdata: [198.51.100.1]\n"//
                + "---\n"//
                + "name: www2.geo.denominator.io.\n"//
                + "type: A\n"//
                + "qualifier: alazona\n"//
                + "ttl: 300\n"//
                + "profiles:\n"//
                + "- type: geo\n"//
                + "  regions:\n"//
                + "    United States (US): [Alaska, Arizona]\n"//
                + "rdata: [192.0.2.1]\n"//
                + "---\n"//
                + "name: www2.weighted.denominator.io.\n"//
                + "type: A\n"//
                + "qualifier: US-West\n"//
                + "ttl: 0\n"//
                + "profiles:\n"//
                + "- {type: weighted, weight: 0}\n"//
                + "rdata: [192.0.2.1]\n"//
                + "");
    }

    @Test(description = "denominator -p mock rrset -z denominator.io get -n www.weighted.denominator.io.")
    public void rrsetsByName() throws IOException, CmdLineException {
        StringWriter writer = new StringWriter();
        Denominator.execute(writer, "-p", "mock", "rrset", "-z", "denominator.io.", "get", "-n",
                "www.weighted.denominator.io.");

        assertEquals(writer.toString(), ""//
                + "name: www.weighted.denominator.io.\n"//
                + "type: CNAME\n"//
                + "qualifier: EU-West\n"//
                + "ttl: 0\n"//
                + "profiles:\n"//
                + "- {type: weighted, weight: 1}\n"//
                + "rdata: [c.denominator.io.]\n"//
                + "---\n"//
                + "name: www.weighted.denominator.io.\n"//
                + "type: CNAME\n"//
                + "qualifier: US-East\n"//
                + "ttl: 0\n"//
                + "profiles:\n"//
                + "- {type: weighted, weight: 1}\n"//
                + "rdata: [b.denominator.io.]\n"//
                + "---\n"//
                + "name: www.weighted.denominator.io.\n"//
                + "type: CNAME\n"//
                + "qualifier: US-West\n"//
                + "ttl: 0\n"//
                + "profiles:\n"//
                + "- {type: weighted, weight: 1}\n"//
                + "rdata: [a.denominator.io.]\n"//
                + "");
    }

    @Test(description = "denominator -p mock rrset -z denominator.io get -n denominator.io. -t NS")
    public void rrsetsByNameAndType() throws IOException, CmdLineException {
        StringWriter writer = new StringWriter();
        Denominator.execute(writer, "-p", "mock", "rrset", "-z", "denominator.io.", "get", "-n", "denominator.io.",
                "-t", "NS");

        assertEquals(writer.toString(), ""//
                + "name: denominator.io.\n"//
                + "type: NS\n"//
                + "ttl: 86400\n"//
                + "rdata: [ns1.denominator.io.]\n"//
                + "");
    }

    @Test(description = "denominator -p mock rrset -z denominator.io get -n www.weighted.denominator.io. -t CNAME -q EU-West")
    public void rrsetsByNameTypeAndQualifier() throws IOException, CmdLineException {
        StringWriter writer = new StringWriter();
        Denominator.execute(writer, "-p", "mock", "rrset", "-z", "denominator.io.", "get", "-n",
                "www.weighted.denominator.io.", "-t", "CNAME", "-q", "EU-West");

        assertEquals(writer.toString(), ""//
                + "name: www.weighted.denominator.io.\n"//
                + "type: CNAME\n"//
                + "qualifier: EU-West\n"//
                + "ttl: 0\n"//
                + "profiles:\n"//
                + "- {type: weighted, weight: 1}\n"//
                + "rdata: [c.denominator.io.]\n"//
                + "");
    }
}
