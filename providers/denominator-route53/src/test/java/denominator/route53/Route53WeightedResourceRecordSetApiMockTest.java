package denominator.route53;

import static denominator.CredentialsConfiguration.credentials;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

import denominator.Denominator;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Weighted;
import denominator.model.rdata.CNAMEData;
import denominator.profile.WeightedResourceRecordSetApi;

@Test(singleThreaded = true)
public class Route53WeightedResourceRecordSetApiMockTest {

    private String noRecords = "<ListResourceRecordSetsResponse><ResourceRecordSets></ResourceRecordSets></ListResourceRecordSetsResponse>";
    private String oneRecord = "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>MyService-East</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www1.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";
    private String twoRecords = "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>MyService-East</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www1.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>MyService-West</SetIdentifier><Weight>5</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www2.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";

    private String identifier1 = "MyService-East";

    private ResourceRecordSet<CNAMEData> rrset1 = ResourceRecordSet.<CNAMEData> builder()//
            .name("www.denominator.io.")//
            .type("CNAME")//
            .qualifier(identifier1)//
            .ttl(0)//
            .addProfile(Weighted.create(1))//
            .add(CNAMEData.create("www1.denominator.io.")).build();
    
    private String identifier2 = "MyService-West";

    private ResourceRecordSet<CNAMEData> rrset2 = ResourceRecordSet.<CNAMEData> builder()//
            .name("www.denominator.io.")//
            .type("CNAME")//
            .qualifier(identifier2)//
            .ttl(0)//
            .addProfile(Weighted.create(5))//
            .add(CNAMEData.create("www2.denominator.io.")).build();

    @Test
    public void iterateByNameWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));

        server.play();

        try {
            WeightedResourceRecordSetApi api = mockApi(server.getUrl(""));
            Iterator<ResourceRecordSet<?>> iterator = api.iterateByName("www.denominator.io.");
            assertEquals(iterator.next(), rrset1);
            assertEquals(iterator.next(), rrset2);

            assertEquals(server.getRequestCount(), 1);

            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io. HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));

        server.play();

        try {
            WeightedResourceRecordSetApi api = mockApi(server.getUrl(""));
            assertFalse(api.iterateByName("www.denominator.io.").hasNext());

            assertEquals(server.getRequestCount(), 1);

            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io. HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameAndTypeWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));

        server.play();

        try {
            WeightedResourceRecordSetApi api = mockApi(server.getUrl(""));
            assertEquals(api.iterateByNameAndType("www.denominator.io.", "CNAME").next(), rrset1);

            assertEquals(server.getRequestCount(), 1);

            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=CNAME HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));

        server.play();

        try {
            WeightedResourceRecordSetApi api = mockApi(server.getUrl(""));
            assertFalse(api.iterateByNameAndType("www.denominator.io.", "CNAME").hasNext());

            assertEquals(server.getRequestCount(), 1);

            assertEquals(server.takeRequest().getRequestLine(),
                    "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=CNAME HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void getByNameTypeAndQualifierWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));

        server.play();

        try {
            WeightedResourceRecordSetApi api = mockApi(server.getUrl(""));
            assertEquals(api.getByNameTypeAndQualifier("www.denominator.io.", "CNAME", identifier1), rrset1);

            assertEquals(server.getRequestCount(), 1);

            assertEquals(
                    server.takeRequest().getRequestLine(),
                    "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-East HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void getByNameTypeAndQualifierWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));

        server.play();

        try {
            WeightedResourceRecordSetApi api = mockApi(server.getUrl(""));
            assertNull(api.getByNameTypeAndQualifier("www.denominator.io.", "CNAME", identifier1));

            assertEquals(server.getRequestCount(), 1);

            assertEquals(
                    server.takeRequest().getRequestLine(),
                    "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-East HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    private String createCNAMERecordSet = "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch><Changes><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>MyService-East</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www1.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";
    private String changeSynced = "<GetChangeResponse><ChangeInfo><Id>/change/C2682N5HXP0BZ4</Id><Status>INSYNC</Status><SubmittedAt>2011-09-10T01:36:41.958Z</SubmittedAt></ChangeInfo></GetChangeResponse>";

    @Test
    public void putRecordSet() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));

        server.play();

        try {
            WeightedResourceRecordSetApi api = mockApi(server.getUrl(""));
            api.put(rrset1);

            assertEquals(server.getRequestCount(), 2);

            assertEquals(
                    server.takeRequest().getRequestLine(),
                    "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-East HTTP/1.1");

            RecordedRequest createRRSet = server.takeRequest();
            assertEquals(createRRSet.getRequestLine(), "POST /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
            assertEquals(new String(createRRSet.getBody()), createCNAMERecordSet);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void putRecordSetSkipsWhenEqual() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));

        server.play();

        try {
            WeightedResourceRecordSetApi api = mockApi(server.getUrl(""));
            api.put(rrset1);

            assertEquals(server.getRequestCount(), 1);

            assertEquals(
                    server.takeRequest().getRequestLine(),
                    "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-East HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    private String deleteQualifier1 = "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-12-12/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.denominator.io.</Name><Type>CNAME</Type><SetIdentifier>MyService-East</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www1.denominator.io.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";

    @Test
    public void deleteDoesntAffectOtherQualifiers() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));

        server.play();

        try {
            WeightedResourceRecordSetApi api = mockApi(server.getUrl(""));
            api.deleteByNameTypeAndQualifier("www.denominator.io.", "CNAME", identifier1);

            assertEquals(server.getRequestCount(), 2);

            assertEquals(
                    server.takeRequest().getRequestLine(),
                    "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-East HTTP/1.1");

            RecordedRequest deleteQualifier1 = server.takeRequest();
            assertEquals(deleteQualifier1.getRequestLine(), "POST /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
            assertEquals(new String(deleteQualifier1.getBody()), this.deleteQualifier1);
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void deleteAbsentRRSDoesNothing() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.play();

        try {
            WeightedResourceRecordSetApi api = mockApi(server.getUrl(""));
            api.deleteByNameTypeAndQualifier("www.denominator.io.", "CNAME", identifier2);

            assertEquals(server.getRequestCount(), 1);

            assertEquals(
                    server.takeRequest().getRequestLine(),
                    "GET /2012-12-12/hostedzone/Z1PA6795UKMFR9/rrset?name=www.denominator.io.&type=CNAME&identifier=MyService-West HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    private static WeightedResourceRecordSetApi mockApi(final URL url) {
        return Denominator.create(new Route53Provider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, credentials("accessKey", "secretKey")).api().weightedRecordSetsInZone("Z1PA6795UKMFR9");
    }
}
