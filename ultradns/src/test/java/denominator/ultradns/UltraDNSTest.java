package denominator.ultradns;

import static denominator.ultradns.UltraDNSException.DIRECTIONALPOOL_NOT_FOUND;
import static denominator.ultradns.UltraDNSException.DIRECTIONALPOOL_RECORD_NOT_FOUND;
import static denominator.ultradns.UltraDNSException.GROUP_NOT_FOUND;
import static denominator.ultradns.UltraDNSException.POOL_ALREADY_EXISTS;
import static denominator.ultradns.UltraDNSException.POOL_NOT_FOUND;
import static denominator.ultradns.UltraDNSException.POOL_RECORD_ALREADY_EXISTS;
import static denominator.ultradns.UltraDNSException.RESOURCE_RECORD_ALREADY_EXISTS;
import static denominator.ultradns.UltraDNSException.RESOURCE_RECORD_NOT_FOUND;
import static denominator.ultradns.UltraDNSException.SYSTEM_ERROR;
import static java.lang.String.format;
import static java.util.Locale.US;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.model.Zone;
import denominator.ultradns.UltraDNS.DirectionalGroup;
import denominator.ultradns.UltraDNS.DirectionalRecord;
import denominator.ultradns.UltraDNS.NameAndType;
import denominator.ultradns.UltraDNS.NetworkStatus;
import denominator.ultradns.UltraDNS.Record;
import feign.Feign;
import feign.codec.DecodeException;

@Test(singleThreaded = true)
public class UltraDNSTest {
    /**
     * param 1, 2: user/pass, 3 is the xml element
     */
    static String SOAP_TEMPLATE = format(denominator.ultradns.UltraDNSTarget.SOAP_TEMPLATE, "joe", "letmein", "%s");

    /**
     * param 1 is the code, 2 is the description
     */
    static String FAULT_TEMPLATE = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "    <soap:Body>\n"//
            + "            <soap:Fault>\n"//
            + "                    <faultcode>soap:Server</faultcode>\n"//
            + "                    <faultstring>Fault occurred while processing.</faultstring>\n"//
            + "                    <detail>\n"//
            + "                            <ns1:UltraWSException xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "                                    <errorCode xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:int\">%s</errorCode>\n"//
            + "                                    <errorDescription xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">%s</errorDescription>\n"//
            + "                            </ns1:UltraWSException>\n"//
            + "                    </detail>\n"//
            + "            </soap:Fault>\n"//
            + "    </soap:Body>\n"//
            + "</soap:Envelope>";

    static String getNeustarNetworkStatus = format(SOAP_TEMPLATE, "<v01:getNeustarNetworkStatus/>");

    static String getNeustarNetworkStatusResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:getNeustarNetworkStatusResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <NeustarNetworkStatus xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Good</NeustarNetworkStatus>\n"//
            + "    </ns1:getNeustarNetworkStatusResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void networkGood() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getNeustarNetworkStatusResponse));
        server.play();

        try {
            assertEquals(mockApi(server.getPort()).getNeustarNetworkStatus(), NetworkStatus.GOOD);

            assertEquals(new String(server.takeRequest().getBody()), getNeustarNetworkStatus);
        } finally {
            server.shutdown();
        }
    }

    static String getNeustarNetworkStatusFailedResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:getNeustarNetworkStatusResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <NeustarNetworkStatus xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Failed</NeustarNetworkStatus>\n"//
            + "    </ns1:getNeustarNetworkStatusResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void networkFailed() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getNeustarNetworkStatusFailedResponse));
        server.play();

        try {
            assertEquals(mockApi(server.getPort()).getNeustarNetworkStatus(), NetworkStatus.FAILED);

            assertEquals(new String(server.takeRequest().getBody()), getNeustarNetworkStatus);
        } finally {
            server.shutdown();
        }
    }

    static String systemError = format(FAULT_TEMPLATE, SYSTEM_ERROR, "System Error");

    @Test
    public void retryOnSystemError() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(systemError));
        server.enqueue(new MockResponse().setBody(getNeustarNetworkStatusResponse));
        server.play();

        try {
            assertEquals(mockApi(server.getPort()).getNeustarNetworkStatus(), NetworkStatus.GOOD);

            assertEquals(new String(server.takeRequest().getBody()), getNeustarNetworkStatus);
            assertEquals(new String(server.takeRequest().getBody()), getNeustarNetworkStatus);
        } finally {
            server.shutdown();
        }
    }

    static String invalidUser = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "    <soap:Body>\n"//
            + "            <soap:Fault>\n"//
            + "                    <faultcode>soap:Client</faultcode>\n"//
            + "                    <faultstring>Invalid User</faultstring>\n"//
            + "            </soap:Fault>\n"//
            + "    </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.getNeustarNetworkStatus\\(\\) failed: Invalid User")
    public void noRetryOnInvalidUser() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(invalidUser));
        server.play();

        try {
            mockApi(server.getPort()).getNeustarNetworkStatus();

            assertEquals(new String(server.takeRequest().getBody()), getNeustarNetworkStatus);
        } finally {
            server.shutdown();
        }
    }

    @Test(expectedExceptions = DecodeException.class, expectedExceptionsMessageRegExp = "Content is not allowed in prolog.")
    public void networkStatusCantParse() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{\"foo\": \"bar\"}"));
        server.play();

        try {
            mockApi(server.getPort()).getNeustarNetworkStatus();
        } finally {
            assertEquals(new String(server.takeRequest().getBody()), getNeustarNetworkStatus);
            server.shutdown();
        }
    }

    static String getAccountsListOfUser = format(SOAP_TEMPLATE, "<v01:getAccountsListOfUser/>");

    static String getAccountsListOfUserResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:getAccountsListOfUserResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <AccountsList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n"//
            + "        <ns2:AccountDetailsData accountID=\"AAAAAAAAAAAAAAAA\" accountName=\"denominator\" />\n"//
            + "      </AccountsList>\n"//
            + "    </ns1:getAccountsListOfUserResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void accountId() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
        server.play();

        try {
            assertEquals(mockApi(server.getPort()).getAccountsListOfUser(), "AAAAAAAAAAAAAAAA");

            assertEquals(new String(server.takeRequest().getBody()), getAccountsListOfUser);
        } finally {
            server.shutdown();
        }
    }

    static String getZonesOfAccount = format(SOAP_TEMPLATE,
            "<v01:getZonesOfAccount><accountId>AAAAAAAAAAAAAAAA</accountId><zoneType>all</zoneType></v01:getZonesOfAccount>");

    static String getZonesOfAccountResponseHeader = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:getZonesOfAccountResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <ZoneList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";//

    static String getZonesOfAccountResponseFooter = ""//
            + "      </ZoneList>\n"//
            + "    </ns1:getZonesOfAccountResponse>\n"//
            + "  </soap:Body>\n" //
            + "</soap:Envelope>";

    static String getZonesOfAccountResponsePresent = ""//
            + getZonesOfAccountResponseHeader //
            + "        <ns2:UltraZone zoneName=\"denominator.io.\" zoneType=\"1\" accountId=\"AAAAAAAAAAAAAAAA\" owner=\"EEEEEEEEEEEEEEE\" zoneId=\"0000000000000001\" dnssecStatus=\"UNSIGNED\"/>\n"//
            + "        <ns2:UltraZone zoneName=\"0.1.2.3.4.5.6.7.ip6.arpa.\" zoneType=\"1\" accountId=\"AAAAAAAAAAAAAAAA\" owner=\"EEEEEEEEEEEEEEEE\" zoneId=\"0000000000000002\" dnssecStatus=\"UNSIGNED\"/>\n"//
            + getZonesOfAccountResponseFooter;

    @Test
    public void zonesOfAccountPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getZonesOfAccountResponsePresent));
        server.play();

        try {
            assertEquals(mockApi(server.getPort()).getZonesOfAccount("AAAAAAAAAAAAAAAA"),
                    ImmutableList.of(Zone.create("denominator.io."), Zone.create("0.1.2.3.4.5.6.7.ip6.arpa.")));

            assertEquals(new String(server.takeRequest().getBody()), getZonesOfAccount);
        } finally {
            server.shutdown();
        }
    }

    static String getZonesOfAccountResponseAbsent = getZonesOfAccountResponseHeader + getZonesOfAccountResponseFooter;

    @Test
    public void zonesOfAccountAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));
        server.play();

        try {
            assertTrue(mockApi(server.getPort()).getZonesOfAccount("AAAAAAAAAAAAAAAA").isEmpty());

            assertEquals(new String(server.takeRequest().getBody()), getZonesOfAccount);
        } finally {
            server.shutdown();
        }
    }

    static String getResourceRecordsOfZone = format(SOAP_TEMPLATE,
            "<v01:getResourceRecordsOfZone><zoneName>denominator.io.</zoneName><rrType>0</rrType></v01:getResourceRecordsOfZone>");

    static String getResourceRecordsOfZoneResponseHeader = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:getResourceRecordsOfZoneResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <ResourceRecordList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";//

    static String getResourceRecordsOfZoneResponseFooter = ""//
            + "      </ResourceRecordList>\n"//
            + "    </ns1:getResourceRecordsOfZoneResponse>\n"//
            + "  </soap:Body>\n" //
            + "</soap:Envelope>";

    private String getResourceRecordsOfZoneResponsePresent = ""//
            + getResourceRecordsOfZoneResponseHeader //
            + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"1\" DName=\"www.denominator.io.\" TTL=\"3600\" Guid=\"04023A2507B6468F\" ZoneId=\"0000000000000001\" LName=\"www.denominator.io.\" Created=\"2010-10-02T16:57:16.000Z\" Modified=\"2011-09-27T23:49:21.000Z\">\n"//
            + "          <ns2:InfoValues Info1Value=\"1.2.3.4\"/>\n"//
            + "        </ns2:ResourceRecord>\n"//
            + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"2\" DName=\"denominator.io.\" TTL=\"86400\" Guid=\"0B0338C2023F7969\" ZoneId=\"0000000000000001\" LName=\"denominator.io.\" Created=\"2009-10-12T12:02:23.000Z\" Modified=\"2009-10-12T12:02:23.000Z\">\n"//
            + "          <ns2:InfoValues Info1Value=\"pdns2.ultradns.net.\"/>\n"//
            + "        </ns2:ResourceRecord>\n"//
            + getResourceRecordsOfZoneResponseFooter;

    @Test
    public void recordsInZonePresent() throws IOException, InterruptedException, ParseException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponsePresent));
        server.play();

        try {
            List<Record> records = mockApi(server.getPort()).getResourceRecordsOfZone("denominator.io.");

            assertEquals(records.size(), 2);

            // sorted
            assertEquals(records.get(1).id, "04023A2507B6468F");
            assertEquals(records.get(1).created, iso8601SimpleDateFormat.parse("2010-10-02T16:57:16.000Z"));
            assertEquals(records.get(1).name, "www.denominator.io.");
            assertEquals(records.get(1).typeCode, 1);
            assertEquals(records.get(1).ttl, 3600);
            assertEquals(records.get(1).rdata.get(0), "1.2.3.4");

            assertEquals(records.get(0).id, "0B0338C2023F7969");
            assertEquals(records.get(0).created, iso8601SimpleDateFormat.parse("2009-10-12T12:02:23.000Z"));
            assertEquals(records.get(0).name, "denominator.io.");
            assertEquals(records.get(0).typeCode, 2);
            assertEquals(records.get(0).ttl, 86400);
            assertEquals(records.get(0).rdata.get(0), "pdns2.ultradns.net.");

            assertEquals(new String(server.takeRequest().getBody()), getResourceRecordsOfZone);
        } finally {
            server.shutdown();
        }
    }

    static String getResourceRecordsOfZoneResponseAbsent = getResourceRecordsOfZoneResponseHeader
            + getResourceRecordsOfZoneResponseFooter;

    @Test
    public void recordsInZoneAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));
        server.play();

        try {
            assertTrue(mockApi(server.getPort()).getResourceRecordsOfZone("denominator.io.").isEmpty());

            assertEquals(new String(server.takeRequest().getBody()), getResourceRecordsOfZone);
        } finally {
            server.shutdown();
        }
    }

    static String getResourceRecordsOfDNameByType = format(
            SOAP_TEMPLATE,
            "<v01:getResourceRecordsOfDNameByType><zoneName>denominator.io.</zoneName><hostName>denominator.io.</hostName><rrType>6</rrType></v01:getResourceRecordsOfDNameByType>");

    static String getResourceRecordsOfDNameByTypeResponseHeader = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:getResourceRecordsOfDNameByTypeResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <ResourceRecordList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";//

    static String getResourceRecordsOfDNameByTypeResponseFooter = ""//
            + "      </ResourceRecordList>\n"//
            + "    </ns1:getResourceRecordsOfDNameByTypeResponse>\n"//
            + "  </soap:Body>\n" //
            + "</soap:Envelope>";

    static String getResourceRecordsOfDNameByTypeResponsePresent = ""//
            + getResourceRecordsOfDNameByTypeResponseHeader//
            + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"6\" DName=\"denominator.io.\" TTL=\"86400\" Guid=\"04053D8E57C7A22F\" ZoneId=\"03053D8E57C7A22A\" LName=\"denominator.io.\" Created=\"2013-02-22T08:22:48.000Z\" Modified=\"2013-02-22T08:22:49.000Z\">\n"//
            + "          <ns2:InfoValues Info1Value=\"pdns75.ultradns.com.\" Info2Value=\"adrianc.netflix.com.\" Info3Value=\"2013022200\" Info4Value=\"86400\" Info5Value=\"86400\" Info6Value=\"86400\" Info7Value=\"86400\" />\n"//
            + "        </ns2:ResourceRecord>\n"//
            + getResourceRecordsOfDNameByTypeResponseFooter;

    static String getResourceRecordsOfDNameByTypeResponseAbsent = getResourceRecordsOfDNameByTypeResponseHeader
            + getResourceRecordsOfDNameByTypeResponseFooter;

    @Test
    public void recordsInZoneByNameAndTypePresent() throws IOException, InterruptedException, ParseException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponsePresent));
        server.play();

        try {
            List<Record> records = mockApi(server.getPort()).getResourceRecordsOfDNameByType("denominator.io.",
                    "denominator.io.", 6);
            assertEquals(records.size(), 1);
            checkSOARecord(records.get(0));

            assertEquals(new String(server.takeRequest().getBody()), format(getResourceRecordsOfDNameByType));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void recordsInZoneByNameAndTypeAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponseAbsent));
        server.play();

        try {
            assertEquals(
                    mockApi(server.getPort()).getResourceRecordsOfDNameByType("denominator.io.", "denominator.io.", 6),
                    ImmutableList.of());

            assertEquals(new String(server.takeRequest().getBody()), format(getResourceRecordsOfDNameByType));
        } finally {
            server.shutdown();
        }
    }

    static String createResourceRecord = format(
            SOAP_TEMPLATE,
            "<v01:createResourceRecord><transactionID /><resourceRecord ZoneName=\"denominator.io.\" Type=\"15\" DName=\"mail.denominator.io.\" TTL=\"1800\"><InfoValues Info1Value=\"10\" Info2Value=\"maileast.denominator.io.\" /></resourceRecord></v01:createResourceRecord>");

    static String createResourceRecordResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:createResourceRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <guid xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">04063D9D54C6A01F</guid>\n"//
            + "    </ns1:createResourceRecordResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void createRecordInZone() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(createResourceRecordResponse));
        server.play();

        try {
            Record record = new Record();
            record.name = "mail.denominator.io.";
            record.typeCode = 15;
            record.ttl = 1800;
            record.rdata.add("10");
            record.rdata.add("maileast.denominator.io.");
            mockApi(server.getPort()).createResourceRecord(record, "denominator.io.");

            assertEquals(new String(server.takeRequest().getBody()), createResourceRecord);
        } finally {
            server.shutdown();
        }
    }

    static String updateResourceRecord = format(
            SOAP_TEMPLATE,
            "<v01:updateResourceRecord><transactionID /><resourceRecord Guid=\"ABCDEF\" ZoneName=\"denominator.io.\" Type=\"1\" DName=\"www.denominator.io.\" TTL=\"3600\"><InfoValues Info1Value=\"1.1.1.1\" /></resourceRecord></v01:updateResourceRecord>");

    static String updateResourceRecordResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:updateResourceRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"//
            + "    </ns1:updateResourceRecordResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void updateRecordInZone() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(updateResourceRecordResponse));
        server.play();

        try {
            Record record = new Record();
            record.id = "ABCDEF";
            record.name = "www.denominator.io.";
            record.typeCode = 1;
            record.ttl = 3600;
            record.rdata.add("1.1.1.1");
            mockApi(server.getPort()).updateResourceRecord(record, "denominator.io.");

            assertEquals(new String(server.takeRequest().getBody()), updateResourceRecord);
        } finally {
            server.shutdown();
        }
    }

    static String rrAlreadyExists = format(FAULT_TEMPLATE, RESOURCE_RECORD_ALREADY_EXISTS,
            "Resource Record of type 1 with these attributes already exists in the system.");

    /**
     * Granted, this doesn't make sense, but testing found update to return this
     * error.
     */
    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.updateResourceRecord\\(Record,String\\) failed with error 2111: Resource Record of type 1 with these attributes already exists in the system.")
    public void updateRecordInZoneAlreadyExists() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(rrAlreadyExists));
        server.play();

        try {
            Record record = new Record();
            record.id = "ABCDEF";
            record.name = "www.denominator.io.";
            record.typeCode = 1;
            record.ttl = 3600;
            record.rdata.add("1.1.1.1");
            mockApi(server.getPort()).updateResourceRecord(record, "denominator.io.");
        } finally {
            server.shutdown();
        }
    }

    static String deleteResourceRecord = format(SOAP_TEMPLATE,
            "<v01:deleteResourceRecord><transactionID /><guid>ABCDEF</guid></v01:deleteResourceRecord>");

    static String deleteResourceRecordResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:deleteResourceRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"//
            + "    </ns1:deleteResourceRecordResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void deleteRecord() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(deleteResourceRecordResponse));
        server.play();

        try {
            mockApi(server.getPort()).deleteResourceRecord("ABCDEF");

            assertEquals(new String(server.takeRequest().getBody()), deleteResourceRecord);
        } finally {
            server.shutdown();
        }
    }

    static String getLoadBalancingPoolsByZone = format(
            SOAP_TEMPLATE,
            "<v01:getLoadBalancingPoolsByZone><zoneName>denominator.io.</zoneName><lbPoolType>RR</lbPoolType></v01:getLoadBalancingPoolsByZone>");

    static String getLoadBalancingPoolsByZoneResponseHeader = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:getLoadBalancingPoolsByZoneResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <LBPoolList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";//

    static String getLoadBalancingPoolsByZoneResponseFooter = ""//
            + "      </LBPoolList>\n"//
            + "    </ns1:getLoadBalancingPoolsByZoneResponse>\n"//
            + "  </soap:Body>\n" //
            + "</soap:Envelope>";

    private String getLoadBalancingPoolsByZoneResponsePresent = ""//
            + getLoadBalancingPoolsByZoneResponseHeader //
            + "      <ns2:LBPoolData zoneid=\"0000000000000001\">\n"//
            + "        <ns2:PoolData description=\"uswest1\" PoolId=\"000000000000002\" PoolType=\"RD\" PoolRecordType=\"A\" PoolDName=\"app-uswest1.denominator.io.\" ResponseMethod=\"RR\" />\n"//
            + "      </ns2:LBPoolData>\n"//
            + "      <ns2:LBPoolData zoneid=\"0000000000000001\">\n"//
            + "        <ns2:PoolData description=\"uswest2\" PoolId=\"000000000000003\" PoolType=\"RD\" PoolRecordType=\"A\" PoolDName=\"app-uswest2.denominator.io.\" ResponseMethod=\"RR\" />\n"//
            + "      </ns2:LBPoolData>\n"//
            + getLoadBalancingPoolsByZoneResponseFooter;

    @Test
    public void rrPoolNameTypeToIdInZonePresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getLoadBalancingPoolsByZoneResponsePresent));
        server.play();

        try {
            Map<NameAndType, String> pools = mockApi(server.getPort()).getLoadBalancingPoolsByZone("denominator.io.");
            assertEquals(pools.size(), 2);

            NameAndType one = new NameAndType();
            one.name = "app-uswest1.denominator.io.";
            one.type = "A";
            assertEquals(pools.get(one), "000000000000002", pools.toString());
            NameAndType two = new NameAndType();
            two.name = "app-uswest2.denominator.io.";
            two.type = "A";
            assertEquals(pools.get(two), "000000000000003", pools.toString());

            assertEquals(new String(server.takeRequest().getBody()), getLoadBalancingPoolsByZone);
        } finally {
            server.shutdown();
        }
    }

    static String getLoadBalancingPoolsByZoneResponseAbsent = ""//
            + getLoadBalancingPoolsByZoneResponseHeader//
            + getLoadBalancingPoolsByZoneResponseFooter;

    @Test
    public void rrPoolNameTypeToIdInZoneAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getLoadBalancingPoolsByZoneResponseAbsent));
        server.play();

        try {
            assertTrue(mockApi(server.getPort()).getLoadBalancingPoolsByZone("denominator.io.").isEmpty());

            assertEquals(new String(server.takeRequest().getBody()), getLoadBalancingPoolsByZone);
        } finally {
            server.shutdown();
        }
    }

    static String getRRPoolRecords = format(SOAP_TEMPLATE,
            "<v01:getRRPoolRecords><lbPoolId>000000000000002</lbPoolId></v01:getRRPoolRecords>");

    static String getRRPoolRecordsResponseHeader = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:getRRPoolRecordsResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <ResourceRecordList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";//

    static String getRRPoolRecordsResponseFooter = ""//
            + "      </ResourceRecordList>\n"//
            + "    </ns1:getRRPoolRecordsResponse>\n"//
            + "  </soap:Body>\n" //
            + "</soap:Envelope>";

    private String getRRPoolRecordsResponsePresent = ""//
            + getRRPoolRecordsResponseHeader //
            + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"1\" DName=\"www.denominator.io.\" TTL=\"3600\" Guid=\"04023A2507B6468F\" ZoneId=\"0000000000000001\" LName=\"www.denominator.io.\" Created=\"2010-10-02T16:57:16.000Z\" Modified=\"2011-09-27T23:49:21.000Z\">\n"//
            + "          <ns2:InfoValues Info1Value=\"1.2.3.4\"/>\n"//
            + "        </ns2:ResourceRecord>\n"//
            + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"2\" DName=\"denominator.io.\" TTL=\"86400\" Guid=\"0B0338C2023F7969\" ZoneId=\"0000000000000001\" LName=\"denominator.io.\" Created=\"2009-10-12T12:02:23.000Z\" Modified=\"2009-10-12T12:02:23.000Z\">\n"//
            + "          <ns2:InfoValues Info1Value=\"pdns2.ultradns.net.\"/>\n"//
            + "        </ns2:ResourceRecord>\n"//
            + getRRPoolRecordsResponseFooter;

    @Test
    public void recordsInRRPoolPresent() throws IOException, InterruptedException, ParseException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getRRPoolRecordsResponsePresent));
        server.play();

        try {
            List<Record> records = mockApi(server.getPort()).getRRPoolRecords("000000000000002");

            assertEquals(records.size(), 2);

            // sorted
            assertEquals(records.get(1).id, "04023A2507B6468F");
            assertEquals(records.get(1).created, iso8601SimpleDateFormat.parse("2010-10-02T16:57:16.000Z"));
            assertEquals(records.get(1).name, "www.denominator.io.");
            assertEquals(records.get(1).typeCode, 1);
            assertEquals(records.get(1).ttl, 3600);
            assertEquals(records.get(1).rdata.get(0), "1.2.3.4");

            assertEquals(records.get(0).id, "0B0338C2023F7969");
            assertEquals(records.get(0).created, iso8601SimpleDateFormat.parse("2009-10-12T12:02:23.000Z"));
            assertEquals(records.get(0).name, "denominator.io.");
            assertEquals(records.get(0).typeCode, 2);
            assertEquals(records.get(0).ttl, 86400);
            assertEquals(records.get(0).rdata.get(0), "pdns2.ultradns.net.");

            assertEquals(new String(server.takeRequest().getBody()), getRRPoolRecords);
        } finally {
            server.shutdown();
        }
    }

    static String getRRPoolRecordsResponseAbsent = getRRPoolRecordsResponseHeader + getRRPoolRecordsResponseFooter;

    @Test
    public void recordsInRRPoolAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getRRPoolRecordsResponseAbsent));
        server.play();

        try {
            assertTrue(mockApi(server.getPort()).getRRPoolRecords("000000000000002").isEmpty());

            assertEquals(new String(server.takeRequest().getBody()), getRRPoolRecords);
        } finally {
            server.shutdown();
        }
    }

    static String addRRLBPool = format(
            SOAP_TEMPLATE,
            "<v01:addRRLBPool><transactionID /><zoneName>denominator.io.</zoneName><hostName>www.denominator.io.</hostName><description>1</description><poolRecordType>1</poolRecordType><rrGUID /></v01:addRRLBPool>");

    static String addRRLBPoolResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:addRRLBPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <RRPoolID>AAAAAAAAAAAAAAAA</RRPoolID>\n"//
            + "    </ns1:addRRLBPoolResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void createRRPoolInZoneForNameAndType() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(addRRLBPoolResponse));
        server.play();

        try {
            assertEquals(mockApi(server.getPort()).addRRLBPool("denominator.io.", "www.denominator.io.", 1),
                    "AAAAAAAAAAAAAAAA");

            assertEquals(new String(server.takeRequest().getBody()), addRRLBPool);
        } finally {
            server.shutdown();
        }
    }

    static String poolAlreadyExists = format(FAULT_TEMPLATE, POOL_ALREADY_EXISTS,
            "Pool already created for this host name : www.denominator.io.");

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#addRRLBPool\\(String,String,int\\) failed with error 2912: Pool already created for this host name : www.denominator.io.")
    public void createRRPoolInZoneForNameAndTypeWhenAlreadyExists() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(poolAlreadyExists));
        server.play();

        try {
            mockApi(server.getPort()).addRRLBPool("denominator.io.", "www.denominator.io.", 1);
        } finally {
            server.shutdown();
        }
    }

    static String addRecordToRRPool = format(
            SOAP_TEMPLATE,
            "<v01:addRecordToRRPool><transactionID /><roundRobinRecord lbPoolID=\"AAAAAAAAAAAAAAAA\" info1Value=\"www1.denominator.io.\" ZoneName=\"denominator.io.\" Type=\"1\" TTL=\"300\"/></v01:addRecordToRRPool>");

    static String addRecordToRRPoolResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:addRecordToRRPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <guid>012345</guid>\n"//
            + "    </ns1:addRecordToRRPoolResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void addRecordIntoRRPoolInZone() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(addRecordToRRPoolResponse));
        server.play();

        try {
            mockApi(server.getPort()).addRecordToRRPool(1, 300, "www1.denominator.io.", "AAAAAAAAAAAAAAAA",
                    "denominator.io.");

            assertEquals(new String(server.takeRequest().getBody()), addRecordToRRPool);
        } finally {
            server.shutdown();
        }
    }

    static String deleteLBPool = format(
            SOAP_TEMPLATE,
            "<v01:deleteLBPool><transactionID /><lbPoolID>AAAAAAAAAAAAAAAA</lbPoolID><DeleteAll>Yes</DeleteAll><retainRecordId /></v01:deleteLBPool>");

    static String deleteLBPoolResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:deleteLBPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"//
            + "    </ns1:deleteLBPoolResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void deleteRRPool() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(deleteLBPoolResponse));
        server.play();

        try {
            mockApi(server.getPort()).deleteLBPool("AAAAAAAAAAAAAAAA");

            assertEquals(new String(server.takeRequest().getBody()), deleteLBPool);
        } finally {
            server.shutdown();
        }
    }

    static String poolNotFound = format(FAULT_TEMPLATE, POOL_NOT_FOUND, "Pool does not exist in the system");

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#deleteLBPool\\(String\\) failed with error 2911: Pool does not exist in the system")
    public void deleteRRPoolWhenPoolNotFound() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(poolNotFound));
        server.play();

        try {
            mockApi(server.getPort()).deleteLBPool("AAAAAAAAAAAAAAAA");
        } finally {
            server.shutdown();
        }
    }

    static String recordNotFound = format(FAULT_TEMPLATE, RESOURCE_RECORD_NOT_FOUND,
            "No resource record with GUID found in the system AAAAAAAAAAAAAAAA");

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#deleteLBPool\\(String\\) failed with error 2103: No resource record with GUID found in the system AAAAAAAAAAAAAAAA")
    public void deleteRRPoolWhenRecordNotFound() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(recordNotFound));
        server.play();

        try {
            mockApi(server.getPort()).deleteLBPool("AAAAAAAAAAAAAAAA");
        } finally {
            server.shutdown();
        }
    }

    static String getDirectionalPoolsOfZone = format(SOAP_TEMPLATE,
            "<v01:getDirectionalPoolsOfZone><zoneName>denominator.io.</zoneName></v01:getDirectionalPoolsOfZone>");

    static String getDirectionalPoolsOfZoneResponseHeader = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:getDirectionalPoolsOfZoneResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <DirectionalPoolList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";//

    static String getDirectionalPoolsOfZoneResponseFooter = ""//
            + "      </DirectionalPoolList>\n"//
            + "    </ns1:getDirectionalPoolsOfZoneResponse>\n"//
            + "  </soap:Body>\n" //
            + "</soap:Envelope>";

    static String getDirectionalPoolsOfZoneResponsePresent = ""//
            + getDirectionalPoolsOfZoneResponseHeader //
            + "      <ns2:DirectionalPoolData dirpoolid=\"D000000000000001\" Zoneid=\"Z000000000000001\" Pooldname=\"www.denominator.io.\" DirPoolType=\"GEOLOCATION\" Description=\"test with ips and cnames\" />\n"//
            + "      <ns2:DirectionalPoolData dirpoolid=\"D000000000000002\" Zoneid=\"Z000000000000001\" Pooldname=\"www2.denominator.io.\" DirPoolType=\"SOURCEIP\" Description=\"should filter out as not geo\" />\n"//
            + getDirectionalPoolsOfZoneResponseFooter;

    @Test
    public void dirPoolNameToIdsInZonePresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalPoolsOfZoneResponsePresent));
        server.play();

        try {
            Map<String, String> pools = mockApi(server.getPort()).getDirectionalPoolsOfZone("denominator.io.");
            assertEquals(pools.size(), 1);

            assertEquals(pools.get("www.denominator.io."), "D000000000000001");
            assertFalse(pools.containsKey("www2.denominator.io."));

            assertEquals(new String(server.takeRequest().getBody()), getDirectionalPoolsOfZone);
        } finally {
            server.shutdown();
        }
    }

    static String getDirectionalPoolsOfZoneResponseAbsent = ""//
            + getDirectionalPoolsOfZoneResponseHeader//
            + getDirectionalPoolsOfZoneResponseFooter;

    @Test
    public void dirPoolNameToIdsInZoneAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalPoolsOfZoneResponseAbsent));
        server.play();

        try {
            assertTrue(mockApi(server.getPort()).getDirectionalPoolsOfZone("denominator.io.").isEmpty());

            assertEquals(new String(server.takeRequest().getBody()), getDirectionalPoolsOfZone);
        } finally {
            server.shutdown();
        }
    }

    static String getDirectionalDNSRecordsForHostTemplate = format(
            SOAP_TEMPLATE,
            "<v01:getDirectionalDNSRecordsForHost><zoneName>%s</zoneName><hostName>%s</hostName><poolRecordType>%s</poolRecordType></v01:getDirectionalDNSRecordsForHost>");

    static String getDirectionalDNSRecordsForHost = format(getDirectionalDNSRecordsForHostTemplate, "denominator.io.",
            "www.denominator.io.", 0);

    static String getDirectionalDNSRecordsForHostIPV4 = format(getDirectionalDNSRecordsForHostTemplate,
            "denominator.io.", "www.denominator.io.", 1);

    static String getDirectionalDNSRecordsForHostIPV6 = format(getDirectionalDNSRecordsForHostTemplate,
            "denominator.io.", "www.denominator.io.", 28);

    static String getDirectionalDNSRecordsForHostResponseHeader = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:getDirectionalDNSRecordsForHostResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n";

    static String getDirectionalDNSRecordsForHostResponseFooter = ""//
            + "    </ns1:getDirectionalDNSRecordsForHostResponse>\n"//
            + "  </soap:Body>\n" //
            + "</soap:Envelope>";

    static String getDirectionalDNSRecordsForHostResponsePresent = ""//
            + getDirectionalDNSRecordsForHostResponseHeader//
            + "    <DirectionalDNSRecordDetailList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" ZoneName=\"denominator.io.\" DName=\"www.denominator.io.\">\n"//
            + "      <ns2:DirectionalDNSRecordDetail GeolocationGroupName=\"Europe\" GeolocationGroupId=\"C000000000000001\" TerritoriesCount=\"54\" DirPoolRecordId=\"A000000000000001\">\n"//
            + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"300\" noResponseRecord=\"false\">\n"//
            + "          <ns2:InfoValues Info1Value=\"www-000000001.eu-west-1.elb.amazonaws.com.\" />\n"//
            + "        </ns2:DirectionalDNSRecord>\n"//
            + "      </ns2:DirectionalDNSRecordDetail>\n"//
            + "      <ns2:DirectionalDNSRecordDetail GeolocationGroupName=\"US\" GeolocationGroupId=\"C000000000000002\" TerritoriesCount=\"3\" DirPoolRecordId=\"A000000000000002\">\n"//
            + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"300\" noResponseRecord=\"false\">\n"//
            + "          <ns2:InfoValues Info1Value=\"www-000000001.us-east-1.elb.amazonaws.com.\" />\n"//
            + "        </ns2:DirectionalDNSRecord>\n"//
            + "      </ns2:DirectionalDNSRecordDetail>\n"//
            + "      <ns2:DirectionalDNSRecordDetail GeolocationGroupName=\"Everywhere Else\" GeolocationGroupId=\"C000000000000003\" TerritoriesCount=\"323\" DirPoolRecordId=\"A000000000000003\">\n"//
            + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"60\" noResponseRecord=\"false\">\n"//
            + "          <ns2:InfoValues Info1Value=\"www-000000002.us-east-1.elb.amazonaws.com.\" />\n"//
            + "        </ns2:DirectionalDNSRecord>\n"//
            + "      </ns2:DirectionalDNSRecordDetail>\n"//
            + "    </DirectionalDNSRecordDetailList>\n"//
            + getDirectionalDNSRecordsForHostResponseFooter;

    static String getDirectionalDNSRecordsForHostResponseAbsent = getDirectionalDNSRecordsForHostResponseHeader
            + getDirectionalDNSRecordsForHostResponseFooter;

    @Test
    public void directionalRecordsByNameAndTypePresent() throws IOException, InterruptedException, ParseException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponsePresent));
        server.play();

        try {
            List<DirectionalRecord> records = mockApi(server.getPort()).getDirectionalDNSRecordsForHost(
                    "denominator.io.", "www.denominator.io.", 0);

            assertEquals(records.size(), 3);

            assertEquals(records.get(0).geoGroupId, "C000000000000001");
            assertEquals(records.get(0).geoGroupName, "Europe");
            assertFalse(records.get(0).noResponseRecord);
            assertEquals(records.get(0).id, "A000000000000001");
            assertEquals(records.get(0).name, "www.denominator.io.");
            assertEquals(records.get(0).type, "CNAME");
            assertEquals(records.get(0).ttl, 300);
            assertEquals(records.get(0).rdata.get(0), "www-000000001.eu-west-1.elb.amazonaws.com.");

            // sorted
            assertEquals(records.get(2).geoGroupId, "C000000000000002");
            assertEquals(records.get(2).geoGroupName, "US");
            assertFalse(records.get(2).noResponseRecord);
            assertEquals(records.get(2).id, "A000000000000002");
            assertEquals(records.get(2).name, "www.denominator.io.");
            assertEquals(records.get(2).type, "CNAME");
            assertEquals(records.get(2).ttl, 300);
            assertEquals(records.get(2).rdata.get(0), "www-000000001.us-east-1.elb.amazonaws.com.");

            assertEquals(records.get(1).geoGroupId, "C000000000000003");
            assertEquals(records.get(1).geoGroupName, "Everywhere Else");
            assertFalse(records.get(1).noResponseRecord);
            assertEquals(records.get(1).id, "A000000000000003");
            assertEquals(records.get(1).name, "www.denominator.io.");
            assertEquals(records.get(1).type, "CNAME");
            assertEquals(records.get(1).ttl, 60);
            assertEquals(records.get(1).rdata.get(0), "www-000000002.us-east-1.elb.amazonaws.com.");

            assertEquals(new String(server.takeRequest().getBody()), format(getDirectionalDNSRecordsForHost));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void directionalRecordsByNameAndTypeAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponseAbsent));
        server.play();

        try {
            assertTrue(mockApi(server.getPort()).getDirectionalDNSRecordsForHost("denominator.io.",
                    "www.denominator.io.", 0).isEmpty());

            assertEquals(new String(server.takeRequest().getBody()), format(getDirectionalDNSRecordsForHost));
        } finally {
            server.shutdown();
        }
    }

    static String dirPoolNotFound = format(FAULT_TEMPLATE, DIRECTIONALPOOL_NOT_FOUND,
            "No Pool or Multiple pools of same type exists for the PoolName : www.denominator.io.");

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.getDirectionalDNSRecordsForHost\\(String,String,int\\) failed with error 2142: No Pool or Multiple pools of same type exists for the PoolName : www.denominator.io.")
    public void directionalRecordsByNameAndTypeWhenPoolNotFound() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(dirPoolNotFound));
        server.play();

        try {
            mockApi(server.getPort()).getDirectionalDNSRecordsForHost("denominator.io.", "www.denominator.io.", 0);
        } finally {
            server.shutdown();
        }
    }

    private String getDirectionalDNSRecordsForHostResponseFiltersOutSourceIP = ""//
            + getDirectionalDNSRecordsForHostResponseHeader//
            + "    <DirectionalDNSRecordDetailList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" ZoneName=\"denominator.io.\" DName=\"www.denominator.io.\">\n"//
            + "      <ns2:DirectionalDNSRecordDetail SourceIPGroupName=\"172.16.1.0/24\" SourceIPGroupId=\"C000000000000001\" TerritoriesCount=\"54\" DirPoolRecordId=\"A000000000000001\">\n"//
            + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"300\" noResponseRecord=\"false\">\n"//
            + "          <ns2:InfoValues Info1Value=\"www-000000001.eu-west-1.elb.amazonaws.com.\" />\n"//
            + "        </ns2:DirectionalDNSRecord>\n"//
            + "      </ns2:DirectionalDNSRecordDetail>\n"//
            + "      <ns2:DirectionalDNSRecordDetail GeolocationGroupName=\"US\" GeolocationGroupId=\"C000000000000002\" TerritoriesCount=\"3\" DirPoolRecordId=\"A000000000000002\">\n"//
            + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"300\" noResponseRecord=\"false\">\n"//
            + "          <ns2:InfoValues Info1Value=\"www-000000001.us-east-1.elb.amazonaws.com.\" />\n"//
            + "        </ns2:DirectionalDNSRecord>\n"//
            + "      </ns2:DirectionalDNSRecordDetail>\n"//
            + "    </DirectionalDNSRecordDetailList>\n"//
            + getDirectionalDNSRecordsForHostResponseFooter;

    @Test
    public void directionalRecordsByNameAndTypeFiltersOutSourceIP() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponseFiltersOutSourceIP));
        server.play();

        try {
            List<DirectionalRecord> records = mockApi(server.getPort()).getDirectionalDNSRecordsForHost(
                    "denominator.io.", "www.denominator.io.", 0);

            assertEquals(records.size(), 1);

            assertEquals(records.get(0).geoGroupId, "C000000000000002");
            assertEquals(records.get(0).geoGroupName, "US");
            assertFalse(records.get(0).noResponseRecord);
            assertEquals(records.get(0).id, "A000000000000002");
            assertEquals(records.get(0).name, "www.denominator.io.");
            assertEquals(records.get(0).type, "CNAME");
            assertEquals(records.get(0).ttl, 300);
            assertEquals(records.get(0).rdata.get(0), "www-000000001.us-east-1.elb.amazonaws.com.");

            assertEquals(new String(server.takeRequest().getBody()), format(getDirectionalDNSRecordsForHost));
        } finally {
            server.shutdown();
        }
    }

    static String getDirectionalDNSRecordsForGroupTemplate = format(
            SOAP_TEMPLATE,
            "<v01:getDirectionalDNSRecordsForGroup><groupName>%s</groupName><hostName>%s</hostName><zoneName>%s</zoneName><poolRecordType>%s</poolRecordType></v01:getDirectionalDNSRecordsForGroup>");

    static String getDirectionalDNSRecordsForGroup = format(getDirectionalDNSRecordsForGroupTemplate, "Europe",
            "www.denominator.io.", "denominator.io.", 1);

    static String getDirectionalDNSRecordsForGroupEuropeIPV6 = format(getDirectionalDNSRecordsForGroupTemplate,
            "Europe", "www.denominator.io.", "denominator.io.", 28);

    static String getDirectionalDNSRecordsForGroupResponseHeader = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:getDirectionalDNSRecordsForGroupResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n";

    static String getDirectionalDNSRecordsForGroupResponseFooter = ""//
            + "    </ns1:getDirectionalDNSRecordsForGroupResponse>\n"//
            + "  </soap:Body>\n" //
            + "</soap:Envelope>";

    static String getDirectionalDNSRecordsForGroupResponsePresent = ""//
            + getDirectionalDNSRecordsForGroupResponseHeader//
            + "    <DirectionalDNSRecordDetailList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" ZoneName=\"denominator.io.\" DName=\"www.denominator.io.\">\n"//
            + "      <ns2:DirectionalDNSRecordDetail GeolocationGroupName=\"Europe\" GeolocationGroupId=\"C000000000000001\" TerritoriesCount=\"54\" DirPoolRecordId=\"A000000000000001\">\n"//
            + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"300\" noResponseRecord=\"false\">\n"//
            + "          <ns2:InfoValues Info1Value=\"www-000000001.eu-west-1.elb.amazonaws.com.\" />\n"//
            + "        </ns2:DirectionalDNSRecord>\n"//
            + "      </ns2:DirectionalDNSRecordDetail>\n"//
            + "    </DirectionalDNSRecordDetailList>\n"//
            + getDirectionalDNSRecordsForGroupResponseFooter;

    static String getDirectionalDNSRecordsForGroupResponseAbsent = getDirectionalDNSRecordsForGroupResponseHeader
            + getDirectionalDNSRecordsForGroupResponseFooter;

    @Test
    public void directionalRecordsInZoneAndGroupByNameAndTypePresent() throws IOException, InterruptedException,
            ParseException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponsePresent));
        server.play();

        try {
            List<DirectionalRecord> records = mockApi(server.getPort()).getDirectionalDNSRecordsForGroup(
                    "denominator.io.", "Europe", "www.denominator.io.", 1);

            assertEquals(records.size(), 1);

            assertEquals(records.get(0).geoGroupId, "C000000000000001");
            assertEquals(records.get(0).geoGroupName, "Europe");
            assertFalse(records.get(0).noResponseRecord);
            assertEquals(records.get(0).id, "A000000000000001");
            assertEquals(records.get(0).name, "www.denominator.io.");
            assertEquals(records.get(0).type, "CNAME");
            assertEquals(records.get(0).ttl, 300);
            assertEquals(records.get(0).rdata.get(0), "www-000000001.eu-west-1.elb.amazonaws.com.");

            assertEquals(new String(server.takeRequest().getBody()), format(getDirectionalDNSRecordsForGroup));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void directionalRecordsInZoneAndGroupByNameAndTypeAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponseAbsent));
        server.play();

        try {
            assertTrue(mockApi(server.getPort()).getDirectionalDNSRecordsForGroup("denominator.io.", "Europe",
                    "www.denominator.io.", 1).isEmpty());

            assertEquals(new String(server.takeRequest().getBody()), format(getDirectionalDNSRecordsForGroup));
        } finally {
            server.shutdown();
        }
    }

    static String groupNotFound = format(FAULT_TEMPLATE, GROUP_NOT_FOUND, "Group does not exist.");

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#getDirectionalDNSRecordsForGroup\\(String,String,String,int\\) failed with error 4003: Group does not exist.")
    public void directionalRecordsInZoneAndGroupByNameAndTypeWhenGroupNotFound() throws IOException,
            InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(groupNotFound));
        server.play();

        try {
            mockApi(server.getPort()).getDirectionalDNSRecordsForGroup("denominator.io.", "Europe",
                    "www.denominator.io.", 1);
        } finally {
            server.shutdown();
        }
    }

    static String addDirectionalPool = format(
            SOAP_TEMPLATE,
            "<v01:addDirectionalPool><transactionID /><AddDirectionalPoolData dirPoolType=\"GEOLOCATION\" poolRecordType=\"A\" zoneName=\"denominator.io.\" hostName=\"www.denominator.io.\" description=\"A\"/></v01:addDirectionalPool>");

    static String addDirectionalPoolResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:addDirectionalPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <DirPoolID>AAAAAAAAAAAAAAAA</DirPoolID>\n"//
            + "    </ns1:addDirectionalPoolResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void createDirectionalPoolInZoneForNameAndType() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(addDirectionalPoolResponse));
        server.play();

        try {
            assertEquals(mockApi(server.getPort()).addDirectionalPool("denominator.io.", "www.denominator.io.", "A"),
                    "AAAAAAAAAAAAAAAA");

            assertEquals(new String(server.takeRequest().getBody()), addDirectionalPool);
        } finally {
            server.shutdown();
        }
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#addDirectionalPool\\(String,String,String\\) failed with error 2912: Pool already created for this host name : www.denominator.io.")
    public void createDirectionalPoolInZoneForNameAndTypeWhenAlreadyExists() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(poolAlreadyExists));
        server.play();

        try {
            mockApi(server.getPort()).addDirectionalPool("denominator.io.", "www.denominator.io.", "A");
        } finally {
            server.shutdown();
        }
    }

    static String deleteDirectionalPool = format(
            SOAP_TEMPLATE,
            "<v01:deleteDirectionalPool><transactionID /><dirPoolID>AAAAAAAAAAAAAAAA</dirPoolID><retainRecordID /></v01:deleteDirectionalPool>");

    static String deleteDirectionalPoolResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:deleteDirectionalPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"//
            + "    </ns1:deleteDirectionalPoolResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void deleteDirectionalPool() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(deleteDirectionalPoolResponse));
        server.play();

        try {
            mockApi(server.getPort()).deleteDirectionalPool("AAAAAAAAAAAAAAAA");

            assertEquals(new String(server.takeRequest().getBody()), deleteDirectionalPool);
        } finally {
            server.shutdown();
        }
    }

    static String deleteDirectionalPoolRecord = format(
            SOAP_TEMPLATE,
            "<v01:deleteDirectionalPoolRecord><transactionID /><dirPoolRecordId>00000000000</dirPoolRecordId></v01:deleteDirectionalPoolRecord>");

    static String deleteDirectionalPoolRecordResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:deleteDirectionalPoolRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"//
            + "    </ns1:deleteDirectionalPoolRecordResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void deleteDirectionalRecord() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(deleteDirectionalPoolRecordResponse));
        server.play();

        try {
            mockApi(server.getPort()).deleteDirectionalPoolRecord("00000000000");

            assertEquals(new String(server.takeRequest().getBody()), deleteDirectionalPoolRecord);
        } finally {
            server.shutdown();
        }
    }

    static String dirRecordNotFound = format(FAULT_TEMPLATE, DIRECTIONALPOOL_RECORD_NOT_FOUND,
            "Directional Pool Record does not exist in the system");

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#deleteDirectionalPoolRecord\\(String\\) failed with error 2705: Directional Pool Record does not exist in the system")
    public void deleteDirectionalRecordNotFound() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(dirRecordNotFound));
        server.play();

        try {
            mockApi(server.getPort()).deleteDirectionalPoolRecord("00000000000");
        } finally {
            server.shutdown();
        }
    }

    static String addDirectionalPoolRecord = format(
            SOAP_TEMPLATE,
            ""//
                    + "<v01:addDirectionalPoolRecord><transactionID />"//
                    + "<AddDirectionalRecordData directionalPoolId=\"AAAAAAAAAAAAAAAA\">"//
                    + "<DirectionalRecordConfiguration recordType=\"MX\" TTL=\"1800\" >"//
                    + "<InfoValues Info1Value=\"10\" Info2Value=\"maileast.denominator.io.\" />" //
                    + "</DirectionalRecordConfiguration>"//
                    + "<GeolocationGroupData><GroupData groupingType=\"DEFINE_NEW_GROUP\" />" //
                    + "<GeolocationGroupDetails groupName=\"Mexas\">" //
                    + "<GeolocationGroupDefinitionData regionName=\"United States (US)\" territoryNames=\"Maryland;Texas\" />" //
                    + "</GeolocationGroupDetails></GeolocationGroupData>"//
                    + "<forceOverlapTransfer>true</forceOverlapTransfer></AddDirectionalRecordData></v01:addDirectionalPoolRecord>");

    static String addDirectionalPoolRecordResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:addDirectionalPoolRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <DirectionalPoolRecordID xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">06063DC355058294</DirectionalPoolRecordID>\n"//
            + "    </ns1:addDirectionalPoolRecordResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void createDirectionalRecordAndGroupInPool() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(addDirectionalPoolRecordResponse));
        server.play();

        try {
            DirectionalRecord record = new DirectionalRecord();
            record.name = "mail.denominator.io.";
            record.type = "MX";
            record.ttl = 1800;
            record.rdata.add("10");
            record.rdata.add("maileast.denominator.io.");

            DirectionalGroup group = new DirectionalGroup();
            group.name = "Mexas";
            group.regionToTerritories = ImmutableMultimap.<String, String> builder()//
                    .putAll("United States (US)", "Maryland", "Texas")//
                    .build().asMap();

            assertEquals(mockApi(server.getPort()).addDirectionalPoolRecord(record, group, "AAAAAAAAAAAAAAAA"),
                    "06063DC355058294");

            assertEquals(new String(server.takeRequest().getBody()), addDirectionalPoolRecord);
        } finally {
            server.shutdown();
        }
    }

    static String poolRecordAlreadyExists = format(FAULT_TEMPLATE, POOL_RECORD_ALREADY_EXISTS,
            "Resource Record already exists.");

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#addDirectionalPoolRecord\\(DirectionalRecord,DirectionalGroup,String\\) failed with error 4009: Resource Record already exists.")
    public void createDirectionalRecordAndGroupInPoolWhenExists() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(poolRecordAlreadyExists));
        server.play();

        try {
            DirectionalRecord record = new DirectionalRecord();
            record.name = "mail.denominator.io.";
            record.type = "MX";
            record.ttl = 1800;
            record.rdata.add("10");
            record.rdata.add("maileast.denominator.io.");

            DirectionalGroup group = new DirectionalGroup();
            group.name = "Mexas";
            group.regionToTerritories = ImmutableMultimap.<String, String> builder()//
                    .putAll("United States (US)", "Maryland", "Texas")//
                    .build().asMap();

            assertEquals(mockApi(server.getPort()).addDirectionalPoolRecord(record, group, "AAAAAAAAAAAAAAAA"),
                    "06063DC355058294");

            assertEquals(new String(server.takeRequest().getBody()), addDirectionalPoolRecord);
        } finally {
            server.shutdown();
        }
    }

    static String updateDirectionalPoolRecordTemplate = format(
            SOAP_TEMPLATE,
            "<v01:updateDirectionalPoolRecord><transactionID /><UpdateDirectionalRecordData directionalPoolRecordId=\"%s\"><DirectionalRecordConfiguration TTL=\"%s\" ><InfoValues Info1Value=\"%s\" /></DirectionalRecordConfiguration>%s</UpdateDirectionalRecordData></v01:updateDirectionalPoolRecord>");

    static String updateDirectionalPoolRecordRegions = format(
            updateDirectionalPoolRecordTemplate,
            "A000000000000001",
            300,
            "www-000000001.eu-west-1.elb.amazonaws.com.",
            "<GeolocationGroupDetails groupName=\"Europe\"><GeolocationGroupDefinitionData regionName=\"Europe\" territoryNames=\"Aland Islands\" /></GeolocationGroupDetails><forceOverlapTransfer>true</forceOverlapTransfer>");

    static String updateDirectionalPoolRecordResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "  <soap:Body>\n"//
            + "    <ns1:updateDirectionalPoolRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"//
            + "    </ns1:updateDirectionalPoolRecordResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void updateDirectionalRecordAndGroup() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(updateDirectionalPoolRecordResponse));
        server.play();

        try {
            DirectionalRecord record = new DirectionalRecord();
            record.id = "A000000000000001";
            record.name = "www.denominator.io.";
            record.type = "CNAME";
            record.ttl = 300;
            record.rdata.add("www-000000001.eu-west-1.elb.amazonaws.com.");

            DirectionalGroup group = new DirectionalGroup();
            group.name = "Europe";
            group.regionToTerritories = ImmutableMultimap.<String, String> builder()//
                    .putAll("Europe", "Aland Islands")//
                    .build().asMap();

            mockApi(server.getPort()).updateDirectionalPoolRecord(record, group);

            assertEquals(new String(server.takeRequest().getBody()), updateDirectionalPoolRecordRegions);
        } finally {
            server.shutdown();
        }
    }

    static String existsWithSameAttributes = format(FAULT_TEMPLATE, RESOURCE_RECORD_ALREADY_EXISTS,
            "Resource Record of type CNAME with these attributes already exists in the system.");

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#updateDirectionalPoolRecord\\(DirectionalRecord,DirectionalGroup\\) failed with error 2111: Resource Record of type CNAME with these attributes already exists in the system.")
    public void updateDirectionalRecordAndGroupWhenExistsWithSameAttributes() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(500).setBody(existsWithSameAttributes));
        server.play();

        try {
            DirectionalRecord record = new DirectionalRecord();
            record.id = "ABCDEF";
            record.name = "mail.denominator.io.";
            record.type = "MX";
            record.ttl = 1800;
            record.rdata.add("10");
            record.rdata.add("maileast.denominator.io.");

            DirectionalGroup group = new DirectionalGroup();
            group.name = "Mexas";
            group.regionToTerritories = ImmutableMultimap.<String, String> builder()//
                    .putAll("United States (US)", "Maryland", "Texas")//
                    .build().asMap();

            mockApi(server.getPort()).updateDirectionalPoolRecord(record, group);
        } finally {
            server.shutdown();
        }
    }

    static String getDirectionalDNSGroupDetails = format(SOAP_TEMPLATE,
            "<v01:getDirectionalDNSGroupDetails><GroupId>AAAAAAAAAAAAAAAA</GroupId></v01:getDirectionalDNSGroupDetails>");

    static String getDirectionalDNSGroupDetailsResponseEurope = "" //
            + "<?xml version=\"1.0\"?>\n"//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + " <soap:Body>\n"//
            + "         <ns1:getDirectionalDNSGroupDetailsResponseEurope xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "                 <DirectionalDNSGroupDetail xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" GroupName=\"Europe\">\n"//
            + "                         <ns2:DirectionalDNSRegion>\n"//
            + "                                 <ns2:RegionForNewGroups RegionName=\"Europe\"\n"//
            + "                                         TerritoryName=\"Aland Islands;Albania;Andorra;Armenia;Austria;Azerbaijan;Belarus;Belgium;Bosnia-Herzegovina;Bulgaria;Croatia;Czech Republic;Denmark;Estonia;Faroe Islands;Finland;France;Georgia;Germany;Gibraltar;Greece;Guernsey;Hungary;Iceland;Ireland;Isle of Man;Italy;Jersey;Latvia;Liechtenstein;Lithuania;Luxembourg;Macedonia, the former Yugoslav Republic of;Malta;Moldova, Republic of;Monaco;Montenegro;Netherlands;Norway;Poland;Portugal;Romania;San Marino;Serbia;Slovakia;Slovenia;Spain;Svalbard and Jan Mayen;Sweden;Switzerland;Ukraine;Undefined Europe;United Kingdom - England, Northern Ireland, Scotland, Wales;Vatican City\" />\n"//
            + "                         </ns2:DirectionalDNSRegion>\n"//
            + "                 </DirectionalDNSGroupDetail>\n"//
            + "         </ns1:getDirectionalDNSGroupDetailsResponseEurope>\n"//
            + " </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void getDirectionalGroup() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));
        server.play();

        try {
            DirectionalGroup group = mockApi(server.getPort()).getDirectionalDNSGroupDetails("AAAAAAAAAAAAAAAA");
            assertEquals(group.name, "Europe");
            assertEquals(group.regionToTerritories.get("Europe"), ImmutableSet.of("Aland Islands", "Albania",
                    "Andorra", "Armenia", "Austria", "Azerbaijan", "Belarus", "Belgium", "Bosnia-Herzegovina",
                    "Bulgaria", "Croatia", "Czech Republic", "Denmark", "Estonia", "Faroe Islands", "Finland",
                    "France", "Georgia", "Germany", "Gibraltar", "Greece", "Guernsey", "Hungary", "Iceland", "Ireland",
                    "Isle of Man", "Italy", "Jersey", "Latvia", "Liechtenstein", "Lithuania", "Luxembourg",
                    "Macedonia, the former Yugoslav Republic of", "Malta", "Moldova, Republic of", "Monaco",
                    "Montenegro", "Netherlands", "Norway", "Poland", "Portugal", "Romania", "San Marino", "Serbia",
                    "Slovakia", "Slovenia", "Spain", "Svalbard and Jan Mayen", "Sweden", "Switzerland", "Ukraine",
                    "Undefined Europe", "United Kingdom - England, Northern Ireland, Scotland, Wales", "Vatican City"));

            assertEquals(new String(server.takeRequest().getBody()), getDirectionalDNSGroupDetails);
        } finally {
            server.shutdown();
        }
    }

    static String getAvailableRegions = format(SOAP_TEMPLATE, "<v01:getAvailableRegions />");

    static String getAvailableRegionsResponse = ""//
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"//
            + "<soap:Body>\n"//
            + "      <ns1:getAvailableRegionsResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "          <DirectionalDNSAvailableRegionList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n"//
            + "              <ns2:Region TerritoryName=\"Anonymous Proxy\" RegionName=\"Anonymous Proxy (A1)\" RegionID=\"14\" />\n"//
            + "              <ns2:Region TerritoryName=\"Antarctica;Bouvet Island;French Southern Territories\" RegionName=\"Antarctica\" RegionID=\"3\" />\n"//
            + "          </DirectionalDNSAvailableRegionList>\n"//
            + "      </ns1:getAvailableRegionsResponse>\n"//
            + "  </soap:Body>\n"//
            + "</soap:Envelope>";

    @Test
    public void getRegionsByIdAndName() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(getAvailableRegionsResponse));
        server.play();

        try {
            Map<String, Collection<String>> group = mockApi(server.getPort()).getAvailableRegions();
            assertEquals(group.get("Anonymous Proxy (A1)"), ImmutableSortedSet.of("Anonymous Proxy"));
            assertEquals(
                    group.get("Antarctica"),
                    ImmutableSortedSet.<String> naturalOrder().add("Antarctica").add("Bouvet Island")
                            .add("French Southern Territories").build());

            assertEquals(new String(server.takeRequest().getBody()), getAvailableRegions);
        } finally {
            server.shutdown();
        }
    }

    static UltraDNS mockApi(final int port) {
        return Feign.create(new UltraDNSTarget(new UltraDNSProvider() {
            @Override
            public String url() {
                return "http://localhost:" + port;
            }
        }, new Provider<Credentials>() {

            @Override
            public Credentials get() {
                return ListCredentials.from("joe", "letmein");
            }

        }), new UltraDNSProvider.FeignModule());
    }

    static void checkSOARecord(Record soaRecord) throws ParseException {
        assertEquals(soaRecord.id, "04053D8E57C7A22F");
        assertEquals(soaRecord.created, iso8601SimpleDateFormat.parse("2013-02-22T08:22:48.000Z"));
        assertEquals(soaRecord.name, "denominator.io.");
        assertEquals(soaRecord.typeCode, 6);
        assertEquals(soaRecord.ttl, 86400);
        assertEquals(soaRecord.rdata.get(0), "pdns75.ultradns.com.");
        assertEquals(soaRecord.rdata.get(1), "adrianc.netflix.com.");
        assertEquals(soaRecord.rdata.get(2), "2013022200");
        assertEquals(soaRecord.rdata.get(3), "86400");
        assertEquals(soaRecord.rdata.get(4), "86400");
        assertEquals(soaRecord.rdata.get(5), "86400");
        assertEquals(soaRecord.rdata.get(6), "86400");
    }

    static final SimpleDateFormat iso8601SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", US);
}
