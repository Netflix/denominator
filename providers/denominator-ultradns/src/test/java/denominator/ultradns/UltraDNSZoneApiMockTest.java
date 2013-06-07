package denominator.ultradns;

import static denominator.CredentialsConfiguration.credentials;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Denominator;
import denominator.ZoneApi;
import denominator.model.Zone;

@Test(singleThreaded = true)
public class UltraDNSZoneApiMockTest {

    private String getAccountsListOfUser = format(SOAP_TEMPLATE, "<v01:getAccountsListOfUser/>");

    String getAccountsListOfUserResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "    <soap:Body>\n"//
            + "            <ns1:getAccountsListOfUserResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "                    <AccountsList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n"//
            + "                            <ns2:AccountDetailsData accountID=\"AAAAAAAAAAAAAAAA\" accountName=\"denominator\" />\n"//
            + "                    </AccountsList>\n"//
            + "            </ns1:getAccountsListOfUserResponse>\n"//
            + "    </soap:Body>\n"//
            + "</soap:Envelope>";

    private String getZonesOfAccount = format(SOAP_TEMPLATE,
            "<v01:getZonesOfAccount><accountId>AAAAAAAAAAAAAAAA</accountId><zoneType>all</zoneType></v01:getZonesOfAccount>");

    private String getZonesOfAccountResponseHeader = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "    <soap:Body>\n"//
            + "       <ns1:getZonesOfAccountResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "          <ZoneList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";//

    private String getZonesOfAccountResponseFooter = ""//
            + "          </ZoneList>\n"//
            + "       </ns1:getZonesOfAccountResponse>\n"//
            + "    </soap:Body>\n" //
            + "</soap:Envelope>";

    private String zones = new StringBuilder(getZonesOfAccountResponseHeader)
            .append("             <ns2:UltraZone zoneName=\"denominator.io.\" zoneType=\"1\" accountId=\"AAAAAAAAAAAAAAAA\" owner=\"EEEEEEEEEEEEEEE\" zoneId=\"0000000000000001\" dnssecStatus=\"UNSIGNED\"/>\n")
            .append("             <ns2:UltraZone zoneName=\"0.1.2.3.4.5.6.7.ip6.arpa.\" zoneType=\"1\" accountId=\"AAAAAAAAAAAAAAAA\" owner=\"EEEEEEEEEEEEEEEE\" zoneId=\"0000000000000002\" dnssecStatus=\"UNSIGNED\"/>\n")
            .append(getZonesOfAccountResponseFooter).toString();

    private String noZones = new StringBuilder(getZonesOfAccountResponseHeader).append(getZonesOfAccountResponseFooter)
            .toString();

    @Test
    public void iteratorWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setBody(zones));
        server.play();

        try {
            ZoneApi api = mockApi(server.getUrl("/"));
            Zone zone = api.iterator().next();
            assertEquals(zone.name(), "denominator.io.");
            assertFalse(zone.id().isPresent());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(new String(server.takeRequest().getBody()), getAccountsListOfUser);
            assertEquals(new String(server.takeRequest().getBody()), getZonesOfAccount);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iteratorWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
        server.enqueue(new MockResponse().setBody(noZones));
        server.play();

        try {
            ZoneApi api = mockApi(server.getUrl("/"));
            assertFalse(api.iterator().hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(new String(server.takeRequest().getBody()), getAccountsListOfUser);
            assertEquals(new String(server.takeRequest().getBody()), getZonesOfAccount);
        } finally {
            server.shutdown();
        }
    }

    private static ZoneApi mockApi(final URL url) {
        return Denominator.create(new UltraDNSProvider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, credentials("joe", "letmein")).api().zones();
    }

    private static final String SOAP_TEMPLATE = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body>%s</soapenv:Body></soapenv:Envelope>";
}
