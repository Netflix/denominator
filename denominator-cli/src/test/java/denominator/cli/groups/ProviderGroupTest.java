package denominator.cli.groups;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.kohsuke.args4j.CmdLineException;
import org.testng.annotations.Test;

import denominator.cli.Denominator;

@Test
public class ProviderGroupTest {

    @Test(description = "denominator providers")
    public void providers() throws IOException, CmdLineException {
        StringWriter writer = new StringWriter();

        Denominator.execute(writer, "provider", "get");

        assertEquals(writer.toString(), ""//
                + "{name: mock, url: 'mem:mock', duplicateZones: false}\n"//
                + "---\n"//
                + "name: route53\n"//
                + "url: https://route53.amazonaws.com\n"//
                + "duplicateZones: true\n"//
                + "credentialTypes:\n"//
                + "  accessKey: [accessKey, secretKey]\n"//
                + "  session: [accessKey, secretKey, sessionToken]\n"//
                + "---\n"//
                + "name: ultradns\n"//
                + "url: https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01\n"//
                + "duplicateZones: false\n"//
                + "credentialTypes:\n"//
                + "  password: [username, password]\n"//
                + "---\n"//
                + "name: dynect\n"//
                + "url: https://api2.dynect.net/REST\n"//
                + "duplicateZones: false\n"//
                + "credentialTypes:\n"//
                + "  password: [customer, username, password]\n"//
                + "---\n"//
                + "name: clouddns\n"//
                + "url: https://identity.api.rackspacecloud.com/v2.0\n"//
                + "duplicateZones: true\n"//
                + "credentialTypes:\n"//
                + "  password: [username, password]\n"//
                + "  apiKey: [username, apiKey]\n"//
                + "---\n"//
                + "name: designate\n"//
                + "url: http://localhost:5000/v2.0\n"//
                + "duplicateZones: true\n"//
                + "credentialTypes:\n"//
                + "  password: [tenantId, username, password]\n"//
                + "");
    }
}
