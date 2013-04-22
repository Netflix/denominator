package denominator.ultradns;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static java.lang.String.format;
import static org.jclouds.Constants.PROPERTY_MAX_RETRIES;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

@Test(singleThreaded = true)
public class UltraDNSResourceRecordSetApiMockTest {
    static Set<Module> modules = ImmutableSet.<Module> of(new ExecutorServiceModule(sameThreadExecutor(),
            sameThreadExecutor()));

    static UltraDNSWSApi mockUltraDNSWSApi(String uri) {
        Properties overrides = new Properties();
        overrides.setProperty(PROPERTY_MAX_RETRIES, "1");
        return ContextBuilder.newBuilder("ultradns-ws")
                             .credentials("joe", "letmein")
                             .endpoint(uri)
                             .overrides(overrides)
                             .modules(modules)
                             .buildApi(UltraDNSWSApi.class);
    }

    private static final String ZONE_NAME = "foo.com.";

    static UltraDNSResourceRecordSetApi mockUltraDNSResourceRecordSetApi(MockWebServer server) {
        String uri = server.getUrl("/").toString();
        UltraDNSWSApi wsApi = mockUltraDNSWSApi(uri);
        return new UltraDNSResourceRecordSetApi(wsApi.getResourceRecordApiForZone(ZONE_NAME),
                new UltraDNSRoundRobinPoolApi(wsApi.getRoundRobinPoolApiForZone(ZONE_NAME)));
    }

    private String getResourceRecordsOfZone = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body><v01:getResourceRecordsOfZone><zoneName>foo.com.</zoneName><rrType>0</rrType></v01:getResourceRecordsOfZone></soapenv:Body></soapenv:Envelope>";

    private String getResourceRecordsOfZoneResponseHeader = "<?xml version=\"1.0\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:getResourceRecordsOfZoneResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><ResourceRecordList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">";
    private String getResourceRecordsOfZoneResponseFooter = "</ResourceRecordList></ns1:getResourceRecordsOfZoneResponse></soap:Body></soap:Envelope>";

    private String noRecords = new StringBuilder(getResourceRecordsOfZoneResponseHeader)
                                    .append(getResourceRecordsOfZoneResponseFooter).toString();

    @Test
    public void listByNameWhenNoneMatch() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            assertFalse(api.listByName("www.foo.com.").hasNext());
        } finally {
            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfZone);

            server.shutdown();
        }
    }

    private String aRecordTTLGuidAddressTemplate = "<ns2:ResourceRecord ZoneName=\"foo.com.\" Type=\"1\" DName=\"www.foo.com.\" TTL=\"%d\" Guid=\"%s\" ZoneId=\"0000000000000001\" LName=\"www.foo.com.\" Created=\"2009-10-12T12:02:23.000Z\" Modified=\"2011-09-27T23:49:22.000Z\"><ns2:InfoValues Info1Value=\"%s\"/></ns2:ResourceRecord>";

    private String records1And2 = new StringBuilder(getResourceRecordsOfZoneResponseHeader)
            .append(format(aRecordTTLGuidAddressTemplate, 3600, "AAAAAAAAAAAA", "1.2.3.4"))
            .append(format(aRecordTTLGuidAddressTemplate, 3600, "BBBBBBBBBBBB", "5.6.7.8"))
            .append(getResourceRecordsOfZoneResponseFooter).toString();

    @Test
    public void listByNameWhenMatch() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1And2));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            assertEquals(api.listByName("www.foo.com.").next(),
                    a("www.foo.com.", 3600, ImmutableList.of("1.2.3.4", "5.6.7.8")));
        } finally {
            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfZone);

            server.shutdown();
        }
    }

    @Test
    public void getByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            assertEquals(api.getByNameAndType("www.foo.com.", "A"), Optional.absent());
        } finally {
            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfZone);

            server.shutdown();
        }
    }

    @Test
    public void getByNameAndTypeWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1And2));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            assertEquals(api.getByNameAndType("www.foo.com.", "A").get(),
                    a("www.foo.com.", 3600, ImmutableList.of("1.2.3.4", "5.6.7.8")));
        } finally {
            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfZone);

            server.shutdown();
        }
    }

    private String getLoadBalancingPoolsByZone = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body><v01:getLoadBalancingPoolsByZone><zoneName>foo.com.</zoneName><lbPoolType>RR</lbPoolType></v01:getLoadBalancingPoolsByZone></soapenv:Body></soapenv:Envelope>";

    private String getLoadBalancingPoolsByZoneResponseHeader = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:getLoadBalancingPoolsByZoneResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><LBPoolList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">";
    private String getLoadBalancingPoolsByZoneResponseFooter = "</LBPoolList></ns1:getLoadBalancingPoolsByZoneResponse></soap:Body></soap:Envelope>";

    private String noPools = new StringBuilder(getLoadBalancingPoolsByZoneResponseHeader).append(
            getLoadBalancingPoolsByZoneResponseFooter).toString();

    private String addRRLBPoolTemplate = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body><v01:addRRLBPool><transactionID /><zoneName>foo.com.</zoneName><hostName>www.foo.com.</hostName><description>%s</description><poolRecordType>%s</poolRecordType><rrGUID /></v01:addRRLBPool></soapenv:Body></soapenv:Envelope>";
    private String addRRLBPoolResponseTemplate = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:addRRLBPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><RRPoolID xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">%s</RRPoolID></ns1:addRRLBPoolResponse></soap:Body></soap:Envelope>";

    private String addRecordToRRPoolTemplate = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body><v01:addRecordToRRPool><transactionID /><roundRobinRecord lbPoolID=\"%s\" info1Value=\"%s\" ZoneName=\"foo.com.\" Type=\"%s\" TTL=\"%s\"/></v01:addRecordToRRPool></soapenv:Body></soapenv:Envelope>";
    private String addRecordToRRPoolResponseTemplate = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:addRecordToRRPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><guid xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">%s</guid></ns1:addRecordToRRPoolResponse></soap:Body></soap:Envelope>";

    @Test
    public void addFirstACreatesRoundRobinPoolThenAddsRecordToIt() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noPools));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRRLBPoolResponseTemplate, "POOLA")));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            api.add(a("www.foo.com.", 3600, "1.2.3.4"));
        } finally {
            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfZone);

            RecordedRequest getLoadBalancingPoolsByZone = server.takeRequest();
            assertEquals(getLoadBalancingPoolsByZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getLoadBalancingPoolsByZone.getBody()), this.getLoadBalancingPoolsByZone);

            RecordedRequest addLBPoolA = server.takeRequest();
            assertEquals(addLBPoolA.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(addLBPoolA.getBody()), format(addRRLBPoolTemplate, "A", "1"));

            RecordedRequest addRecord1 = server.takeRequest();
            assertEquals(addRecord1.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(addRecord1.getBody()),
                    format(addRecordToRRPoolTemplate, "POOLA", "1.2.3.4", "1", 3600));

            server.shutdown();
        }
    }

    private String poolNameAndIDTemplate = "<ns2:LBPoolData zoneid=\"0000000000000001\"><ns2:PoolData description=\"%s\" PoolId=\"%s\" PoolType=\"RD\" PoolDName=\"www.foo.com.\" ResponseMethod=\"RR\"/></ns2:LBPoolData>";
    private String poolsForAandAAAA = new StringBuilder(getLoadBalancingPoolsByZoneResponseHeader)
            .append(format(poolNameAndIDTemplate, "A", "POOLA"))
            .append(format(poolNameAndIDTemplate, "AAAA", "POOLAAAA"))
            .append(getLoadBalancingPoolsByZoneResponseFooter).toString();

    @Test
    public void addFirstAReusesExistingEmptyRoundRobinPool() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(poolsForAandAAAA));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            api.add(a("www.foo.com.", 3600, "1.2.3.4"));
        } finally {
            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfZone);

            RecordedRequest getLoadBalancingPoolsByZone = server.takeRequest();
            assertEquals(getLoadBalancingPoolsByZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getLoadBalancingPoolsByZone.getBody()), this.getLoadBalancingPoolsByZone);

            RecordedRequest addRecord1 = server.takeRequest();
            assertEquals(addRecord1.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(addRecord1.getBody()),
                    format(addRecordToRRPoolTemplate, "POOLA", "1.2.3.4", "1", 3600));

            server.shutdown();
        }
    }

    private String record1 = new StringBuilder(getResourceRecordsOfZoneResponseHeader)
            .append(format(aRecordTTLGuidAddressTemplate, 3600, "AAAAAAAAAAAA", "1.2.3.4"))
            .append(getResourceRecordsOfZoneResponseFooter).toString();

    private String getRRPoolRecordsTemplate = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body><v01:getRRPoolRecords><lbPoolId>%s</lbPoolId></v01:getRRPoolRecords></soapenv:Body></soapenv:Envelope>";

    private String getRRPoolRecordsResponseHeader = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:getRRPoolRecordsResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><ResourceRecordList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">";
    private String getRRPoolRecordsResponseFooter = "</ResourceRecordList></ns1:getRRPoolRecordsResponse></soap:Body></soap:Envelope>";

    private String pooledRecord1 = new StringBuilder(getRRPoolRecordsResponseHeader)
            .append(format(aRecordTTLGuidAddressTemplate, 3600, "AAAAAAAAAAAA", "1.2.3.4"))
            .append(getRRPoolRecordsResponseFooter).toString();

    private String deleteRecordOfRRPoolTemplate = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body><v01:deleteRecordOfRRPool><transactionID /><guid>%s</guid></v01:deleteRecordOfRRPool></soapenv:Body></soapenv:Envelope>";
    private String deleteRecordOfRRPoolResponse = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:deleteRecordOfRRPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result></ns1:deleteRecordOfRRPoolResponse></soap:Body></soap:Envelope>";

    private String deleteLBPoolTemplate = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body><v01:deleteLBPool><transactionID /><lbPoolID>%s</lbPoolID><DeleteAll>Yes</DeleteAll><retainRecordId /></v01:deleteLBPool></soapenv:Body></soapenv:Envelope>";
    private String deleteLBPoolResponse = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:deleteLBPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result></ns1:deleteLBPoolResponse></soap:Body></soap:Envelope>";

    @Test
    public void removeOnlyRecordAlsoRemovesPool() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(record1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(poolsForAandAAAA));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(pooledRecord1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(deleteRecordOfRRPoolResponse));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(poolsForAandAAAA));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(deleteLBPoolResponse));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            api.remove(a("www.foo.com.", "1.2.3.4"));
        } finally {
            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfZone);

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

            server.shutdown();
        }
    }

    @Test
    public void addSecondAAddsRecordToExistingPool() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(record1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(poolsForAandAAAA));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRecordToRRPoolResponseTemplate, "BBBBBBBBBBBB")));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            api.add(a("www.foo.com.", "5.6.7.8"));
        } finally {
            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfZone);

            RecordedRequest getLoadBalancingPoolsByZone = server.takeRequest();
            assertEquals(getLoadBalancingPoolsByZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getLoadBalancingPoolsByZone.getBody()), this.getLoadBalancingPoolsByZone);

            RecordedRequest addRecord2 = server.takeRequest();
            assertEquals(addRecord2.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(addRecord2.getBody()),
                    format(addRecordToRRPoolTemplate, "POOLA", "5.6.7.8", "1", 3600));

            server.shutdown();
        }
    }

    @Test
    public void addFirstAAAACreatesRoundRobinPoolThenAddsRecordToIt() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noPools));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRRLBPoolResponseTemplate, "POOLAAAA")));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            api.add(aaaa("www.foo.com.", 3600, "2001:0DB8:85A3:0000:0000:8A2E:0370:7334"));
        } finally {
            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfZone);

            RecordedRequest getLoadBalancingPoolsByZone = server.takeRequest();
            assertEquals(getLoadBalancingPoolsByZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getLoadBalancingPoolsByZone.getBody()), this.getLoadBalancingPoolsByZone);

            RecordedRequest addLBPoolA = server.takeRequest();
            assertEquals(addLBPoolA.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(addLBPoolA.getBody()), format(addRRLBPoolTemplate, "AAAA", "28"));

            RecordedRequest addRecord1 = server.takeRequest();
            assertEquals(addRecord1.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(addRecord1.getBody()),
                    format(addRecordToRRPoolTemplate, "POOLAAAA", "2001:0DB8:85A3:0000:0000:8A2E:0370:7334", "28", 3600));

            server.shutdown();
        }
    }

    @Test
    public void addFirstAAAAReusesExistingEmptyRoundRobinPool() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(poolsForAandAAAA));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                format(addRecordToRRPoolResponseTemplate, "AAAAAAAAAAAA")));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            api.add(aaaa("www.foo.com.", 3600, "2001:0DB8:85A3:0000:0000:8A2E:0370:7334"));
        } finally {
            RecordedRequest getResourceRecordsOfZone = server.takeRequest();
            assertEquals(getResourceRecordsOfZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getResourceRecordsOfZone.getBody()), this.getResourceRecordsOfZone);

            RecordedRequest getLoadBalancingPoolsByZone = server.takeRequest();
            assertEquals(getLoadBalancingPoolsByZone.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(new String(getLoadBalancingPoolsByZone.getBody()), this.getLoadBalancingPoolsByZone);

            RecordedRequest addRecord1 = server.takeRequest();
            assertEquals(addRecord1.getRequestLine(), "POST / HTTP/1.1");
            assertEquals(
                    new String(addRecord1.getBody()),
                    format(addRecordToRRPoolTemplate, "POOLAAAA", "2001:0DB8:85A3:0000:0000:8A2E:0370:7334", "28", 3600));

            server.shutdown();
        }
    }
}
