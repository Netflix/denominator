package denominator.ultradns;

import static denominator.CredentialsConfiguration.credentials;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;

import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

import denominator.Denominator;
import denominator.ResourceRecordSetApi;

@Test(singleThreaded = true)
public class UltraDNSResourceRecordSetApiMockTest {
    private String getResourceRecordsOfZone = format(SOAP_TEMPLATE, "<v01:getResourceRecordsOfZone><zoneName>denominator.io.</zoneName><rrType>0</rrType></v01:getResourceRecordsOfZone>");

    private String getResourceRecordsOfZoneResponseHeader = "<?xml version=\"1.0\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:getResourceRecordsOfZoneResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><ResourceRecordList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">";
    private String getResourceRecordsOfZoneResponseFooter = "</ResourceRecordList></ns1:getResourceRecordsOfZoneResponse></soap:Body></soap:Envelope>";

    private String noRecords = new StringBuilder(getResourceRecordsOfZoneResponseHeader)
                                    .append(getResourceRecordsOfZoneResponseFooter).toString();

    @Test
    public void listWhenNoneMatch() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertFalse(api.iterator().hasNext());

            assertEquals(server.getRequestCount(), 1);

            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfZone);
        } finally {
            server.shutdown();
        }
    }

    private String getResourceRecordsOfDNameByTypeTemplate = format(
            SOAP_TEMPLATE,
            "<v01:getResourceRecordsOfDNameByType><zoneName>denominator.io.</zoneName><hostName>%s</hostName><rrType>%s</rrType></v01:getResourceRecordsOfDNameByType>");
    private String getResourceRecordsOfDNameByTypeAll = format(getResourceRecordsOfDNameByTypeTemplate,
            "www.denominator.io.", 0);

    @Test
    public void iterateByNameWhenNoneMatch() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertFalse(api.iterateByName("www.denominator.io.").hasNext());

            assertEquals(server.getRequestCount(), 1);

            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfDNameByTypeAll);
        } finally {
            server.shutdown();
        }
    }

    private String aRecordTTLGuidAddressTemplate = "<ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"1\" DName=\"www.denominator.io.\" TTL=\"%d\" Guid=\"%s\" ZoneId=\"0000000000000001\" LName=\"www.denominator.io.\" Created=\"2009-10-12T12:02:23.000Z\" Modified=\"2011-09-27T23:49:22.000Z\"><ns2:InfoValues Info1Value=\"%s\"/></ns2:ResourceRecord>";

    private String records1And2 = new StringBuilder(getResourceRecordsOfZoneResponseHeader)
            .append(format(aRecordTTLGuidAddressTemplate, 3600, "AAAAAAAAAAAA", "192.0.2.1"))
            .append(format(aRecordTTLGuidAddressTemplate, 3600, "BBBBBBBBBBBB", "198.51.100.1"))
            .append(getResourceRecordsOfZoneResponseFooter).toString();

    @Test
    public void iterateByNameWhenMatch() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1And2));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertEquals(api.iterateByName("www.denominator.io.").next(),
                    a("www.denominator.io.", 3600, ImmutableList.of("192.0.2.1", "198.51.100.1")));

            assertEquals(server.getRequestCount(), 1);

            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfDNameByTypeAll);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void getByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertEquals(api.getByNameAndType("www.denominator.io.", "A"), Optional.absent());
            assertEquals(server.getRequestCount(), 1);

            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfDNameByTypeA);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void getByNameAndTypeWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1And2));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertEquals(api.getByNameAndType("www.denominator.io.", "A").get(),
                    a("www.denominator.io.", 3600, ImmutableList.of("192.0.2.1", "198.51.100.1")));
            assertEquals(server.getRequestCount(), 1);

            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfDNameByTypeA);
        } finally {
            server.shutdown();
        }
    }

    private String getLoadBalancingPoolsByZone = format(SOAP_TEMPLATE, "<v01:getLoadBalancingPoolsByZone><zoneName>denominator.io.</zoneName><lbPoolType>RR</lbPoolType></v01:getLoadBalancingPoolsByZone>");
    private String getLoadBalancingPoolsByZoneResponseHeader = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:getLoadBalancingPoolsByZoneResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><LBPoolList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">";
    private String getLoadBalancingPoolsByZoneResponseFooter = "</LBPoolList></ns1:getLoadBalancingPoolsByZoneResponse></soap:Body></soap:Envelope>";

    private String noPools = new StringBuilder(getLoadBalancingPoolsByZoneResponseHeader).append(
            getLoadBalancingPoolsByZoneResponseFooter).toString();

    private String addRRLBPoolTemplate = format(SOAP_TEMPLATE, "<v01:addRRLBPool><transactionID /><zoneName>denominator.io.</zoneName><hostName>www.denominator.io.</hostName><description>%s</description><poolRecordType>%s</poolRecordType><rrGUID /></v01:addRRLBPool>");
    private String addRRLBPoolResponseTemplate = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:addRRLBPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><RRPoolID xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">%s</RRPoolID></ns1:addRRLBPoolResponse></soap:Body></soap:Envelope>";

    private String addRecordToRRPoolTemplate = format(SOAP_TEMPLATE, "<v01:addRecordToRRPool><transactionID /><roundRobinRecord lbPoolID=\"%s\" info1Value=\"%s\" ZoneName=\"denominator.io.\" Type=\"%s\" TTL=\"%s\"/></v01:addRecordToRRPool>");
    private String addRecordToRRPoolResponseTemplate = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:addRecordToRRPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><guid xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">%s</guid></ns1:addRecordToRRPoolResponse></soap:Body></soap:Envelope>";

    @Test
    public void putFirstACreatesRoundRobinPoolThenAddsRecordToIt() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noPools));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(format(addRRLBPoolResponseTemplate, "POOLA")));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

            assertEquals(server.getRequestCount(), 4);

            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfDNameByTypeA);

            RecordedRequest getLoadBalancingPoolsByZone = server.takeRequest();
            assertEquals(getLoadBalancingPoolsByZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getLoadBalancingPoolsByZone.getBody()), this.getLoadBalancingPoolsByZone);

            RecordedRequest addLBPoolA = server.takeRequest();
            assertEquals(addLBPoolA.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(addLBPoolA.getBody()), format(addRRLBPoolTemplate, "A", "1"));

            RecordedRequest addRecord1 = server.takeRequest();
            assertEquals(addRecord1.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(addRecord1.getBody()),
                    format(addRecordToRRPoolTemplate, "POOLA", "192.0.2.1", "1", 3600));
        } finally {
            server.shutdown();
        }
    }

    private String getResourceRecordsOfDNameByTypeA = format(getResourceRecordsOfDNameByTypeTemplate,
            "www.denominator.io.", 1);

    private String poolNameAndIDTemplate = "<ns2:LBPoolData zoneid=\"0000000000000001\"><ns2:PoolData description=\"%s\" PoolId=\"%s\" PoolType=\"RD\" PoolDName=\"www.denominator.io.\" ResponseMethod=\"RR\"/></ns2:LBPoolData>";
    private String poolsForAandAAAA = new StringBuilder(getLoadBalancingPoolsByZoneResponseHeader)
            .append(format(poolNameAndIDTemplate, "A", "POOLA"))
            .append(format(poolNameAndIDTemplate, "AAAA", "POOLAAAA"))
            .append(getLoadBalancingPoolsByZoneResponseFooter).toString();

    @Test
    public void putFirstAReusesExistingEmptyRoundRobinPool() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(poolsForAandAAAA));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.put(a("www.denominator.io.", 3600, "192.0.2.1"));

            assertEquals(server.getRequestCount(), 3);

            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfDNameByTypeA);

            RecordedRequest getLoadBalancingPoolsByZone = server.takeRequest();
            assertEquals(getLoadBalancingPoolsByZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getLoadBalancingPoolsByZone.getBody()), this.getLoadBalancingPoolsByZone);

            RecordedRequest addRecord1 = server.takeRequest();
            assertEquals(addRecord1.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(addRecord1.getBody()),
                    format(addRecordToRRPoolTemplate, "POOLA", "192.0.2.1", "1", 3600));
        } finally {
            server.shutdown();
        }
    }

    private String record1 = new StringBuilder(getResourceRecordsOfZoneResponseHeader)
            .append(format(aRecordTTLGuidAddressTemplate, 3600, "AAAAAAAAAAAA", "192.0.2.1"))
            .append(getResourceRecordsOfZoneResponseFooter).toString();

    private String getRRPoolRecordsTemplate = format(SOAP_TEMPLATE, "<v01:getRRPoolRecords><lbPoolId>%s</lbPoolId></v01:getRRPoolRecords>");
    private String getRRPoolRecordsResponseHeader = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:getRRPoolRecordsResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><ResourceRecordList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">";
    private String getRRPoolRecordsResponseFooter = "</ResourceRecordList></ns1:getRRPoolRecordsResponse></soap:Body></soap:Envelope>";

    private String pooledRecord1 = new StringBuilder(getRRPoolRecordsResponseHeader)
            .append(format(aRecordTTLGuidAddressTemplate, 3600, "AAAAAAAAAAAA", "192.0.2.1"))
            .append(getRRPoolRecordsResponseFooter).toString();

    private String deleteRecordOfRRPoolTemplate = format(SOAP_TEMPLATE, "<v01:deleteRecordOfRRPool><transactionID /><guid>%s</guid></v01:deleteRecordOfRRPool>");
    private String deleteRecordOfRRPoolResponse = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:deleteRecordOfRRPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result></ns1:deleteRecordOfRRPoolResponse></soap:Body></soap:Envelope>";

    private String deleteLBPoolTemplate = format(SOAP_TEMPLATE, "<v01:deleteLBPool><transactionID /><lbPoolID>%s</lbPoolID><DeleteAll>Yes</DeleteAll><retainRecordId /></v01:deleteLBPool>");
    private String deleteLBPoolResponse = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:deleteLBPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result></ns1:deleteLBPoolResponse></soap:Body></soap:Envelope>";

    @Test
    public void deleteAlsoRemovesPool() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(record1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(poolsForAandAAAA));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(pooledRecord1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(deleteRecordOfRRPoolResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(poolsForAandAAAA));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(deleteLBPoolResponse));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.deleteByNameAndType("www.denominator.io.", "A");

            assertEquals(server.getRequestCount(), 6);

            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfDNameByTypeA);

            RecordedRequest getLoadBalancingPoolsByZone = server.takeRequest();
            assertEquals(getLoadBalancingPoolsByZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getLoadBalancingPoolsByZone.getBody()), this.getLoadBalancingPoolsByZone);

            RecordedRequest getRoundRobinRecordsInPoolA = server.takeRequest();
            assertEquals(getRoundRobinRecordsInPoolA.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getRoundRobinRecordsInPoolA.getBody()), format(getRRPoolRecordsTemplate, "POOLA"));

            RecordedRequest deleteRecord1 = server.takeRequest();
            assertEquals(deleteRecord1.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(deleteRecord1.getBody()), format(deleteRecordOfRRPoolTemplate, "AAAAAAAAAAAA"));

            RecordedRequest checkIfPoolAIsNowEmpty = server.takeRequest();
            assertEquals(checkIfPoolAIsNowEmpty.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(checkIfPoolAIsNowEmpty.getBody()), format(getRRPoolRecordsTemplate, "POOLA"));

            RecordedRequest deletePoolA = server.takeRequest();
            assertEquals(deletePoolA.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(deletePoolA.getBody()), format(deleteLBPoolTemplate, "POOLA"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void putSecondAAddsRecordToExistingPool() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(record1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(poolsForAandAAAA));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRecordToRRPoolResponseTemplate, "BBBBBBBBBBBB")));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.put(a("www.denominator.io.", 3600, ImmutableSet.of("192.0.2.1", "198.51.100.1")));

            assertEquals(server.getRequestCount(), 3);

            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfDNameByTypeA);

            RecordedRequest getLoadBalancingPoolsByZone = server.takeRequest();
            assertEquals(getLoadBalancingPoolsByZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getLoadBalancingPoolsByZone.getBody()), this.getLoadBalancingPoolsByZone);

            RecordedRequest addRecord2 = server.takeRequest();
            assertEquals(addRecord2.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(addRecord2.getBody()),
                    format(addRecordToRRPoolTemplate, "POOLA", "198.51.100.1", "1", 3600));
        } finally {
            server.shutdown();
        }
    }

    private String getResourceRecordsOfDNameByTypeAAAA = format(getResourceRecordsOfDNameByTypeTemplate,
            "www.denominator.io.", 28);

    @Test
    public void putFirstAAAACreatesRoundRobinPoolThenAddsRecordToIt() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noPools));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRRLBPoolResponseTemplate, "POOLAAAA")));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.put(aaaa("www.denominator.io.", 3600, "2001:0DB8:85A3:0000:0000:8A2E:0370:7334"));

            assertEquals(server.getRequestCount(), 4);

            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfDNameByTypeAAAA);

            RecordedRequest getLoadBalancingPoolsByZone = server.takeRequest();
            assertEquals(getLoadBalancingPoolsByZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getLoadBalancingPoolsByZone.getBody()), this.getLoadBalancingPoolsByZone);

            RecordedRequest addLBPoolA = server.takeRequest();
            assertEquals(addLBPoolA.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(addLBPoolA.getBody()), format(addRRLBPoolTemplate, "AAAA", "28"));

            RecordedRequest addRecord1 = server.takeRequest();
            assertEquals(addRecord1.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(
                    new String(addRecord1.getBody()),
                    format(addRecordToRRPoolTemplate, "POOLAAAA", "2001:0DB8:85A3:0000:0000:8A2E:0370:7334", "28", 3600));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void putFirstAAAAReusesExistingEmptyRoundRobinPool() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(poolsForAandAAAA));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.put(aaaa("www.denominator.io.", 3600, "2001:0DB8:85A3:0000:0000:8A2E:0370:7334"));

            assertEquals(server.getRequestCount(), 3);

            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfDNameByTypeAAAA);

            RecordedRequest getLoadBalancingPoolsByZone = server.takeRequest();
            assertEquals(getLoadBalancingPoolsByZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getLoadBalancingPoolsByZone.getBody()), this.getLoadBalancingPoolsByZone);

            RecordedRequest addRecord1 = server.takeRequest();
            assertEquals(addRecord1.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(
                    new String(addRecord1.getBody()),
                    format(addRecordToRRPoolTemplate, "POOLAAAA", "2001:0DB8:85A3:0000:0000:8A2E:0370:7334", "28", 3600));
        } finally {

            server.shutdown();
        }
    }

    private static ResourceRecordSetApi mockApi(final URL url) {
        return Denominator.create(new UltraDNSProvider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, credentials("joe", "letmein")).api().basicRecordSetsInZone("denominator.io.");
    }

    private static final String SOAP_TEMPLATE = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body>%s</soapenv:Body></soapenv:Envelope>";
}
