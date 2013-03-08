package denominator.ultradns;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static denominator.model.ResourceRecordSets.a;
import static java.lang.String.format;
import static org.jclouds.Constants.PROPERTY_MAX_RETRIES;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.ultradns.ws.UltraDNSWSApi;
import org.jclouds.ultradns.ws.UltraDNSWSApiMetadata;
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
                             .build(UltraDNSWSApiMetadata.CONTEXT_TOKEN).getApi();
    }
    
    private static final String ZONE_NAME = "denominator.io.";
    
    static UltraDNSResourceRecordSetApi mockUltraDNSResourceRecordSetApi(MockWebServer server) {
        String uri = server.getUrl("/").toString();
        UltraDNSWSApi wsApi = mockUltraDNSWSApi(uri);
        return new UltraDNSResourceRecordSetApi(
                wsApi.getResourceRecordApiForZone(ZONE_NAME),
                new UltraDNSRoundRobinPoolApi(wsApi.getRoundRobinPoolApiForZone(ZONE_NAME)));
    }
    
    String getResourceRecordsOfDNameByType = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body><v01:getResourceRecordsOfDNameByType><zoneName>denominator.io.</zoneName><hostName>www.denominator.io.</hostName><rrType>1</rrType></v01:getResourceRecordsOfDNameByType></soapenv:Body></soapenv:Envelope>";
    // TODO: temp until ultra fixes the getResourceRecordsOfDNameByType for NS records.
    String getResourceRecordsOfZone = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\"><soapenv:Header><wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken><wsse:Username>joe</wsse:Username><wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">letmein</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body><v01:getResourceRecordsOfZone><zoneName>denominator.io.</zoneName><rrType>0</rrType></v01:getResourceRecordsOfZone></soapenv:Body></soapenv:Envelope>";
    
    //
    String getResourceRecordsOfZoneResponseHeader = "<?xml version=\"1.0\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:getResourceRecordsOfZoneResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\"><ResourceRecordList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">";
    String getResourceRecordsOfZoneResponseFooter = "</ResourceRecordList></ns1:getResourceRecordsOfZoneResponse></soap:Body></soap:Envelope>";

    String noRecords = new StringBuilder(getResourceRecordsOfZoneResponseHeader).append(getResourceRecordsOfZoneResponseFooter).toString();

    @Test
    public void getByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            assertEquals(api.getByNameAndType("www.denominator.io.", "A"), Optional.absent());
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(), "POST / HTTP/1.1");
            
            // TODO: temp until ultra fixes the getResourceRecordsOfDNameByType for NS records.
            assertEquals(new String(listNameAndType.getBody()), getResourceRecordsOfZone);
            //assertEquals(new String(listNameAndType.getBody()), getResourceRecordsOfDNameByType);

            server.shutdown();
        }
    }

    String aRecordTTLGuidAddressTemplate = "<ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"1\" DName=\"www.denominator.io.\" TTL=\"%d\" Guid=\"%s\" ZoneId=\"0000000000000001\" LName=\"www.denominator.io.\" Created=\"2009-10-12T12:02:23.000Z\" Modified=\"2011-09-27T23:49:22.000Z\"><ns2:InfoValues Info1Value=\"%s\"/></ns2:ResourceRecord>";

    String records1And2 = new StringBuilder(getResourceRecordsOfZoneResponseHeader)
            .append(format(aRecordTTLGuidAddressTemplate, 3600, "AAAAAAAAAAAA", "1.2.3.4"))
            .append(format(aRecordTTLGuidAddressTemplate, 3600, "BBBBBBBBBBBB", "5.6.7.8"))
            .append(getResourceRecordsOfZoneResponseFooter).toString();

    @Test
    public void getByNameAndTypeWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1And2));
        server.play();

        try {
            UltraDNSResourceRecordSetApi api = mockUltraDNSResourceRecordSetApi(server);
            assertEquals(api.getByNameAndType("www.denominator.io.", "A").get(),
                    a("www.denominator.io.", 3600, ImmutableList.of("1.2.3.4", "5.6.7.8")));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(), "POST / HTTP/1.1");
            
            // TODO: temp until ultra fixes the getResourceRecordsOfDNameByType for NS records.
            assertEquals(new String(listNameAndType.getBody()), getResourceRecordsOfZone);
            //assertEquals(new String(listNameAndType.getBody()), getResourceRecordsOfDNameByType);

            server.shutdown();
        }
    }
}
