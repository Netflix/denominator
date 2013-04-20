package denominator.route53;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.cname;
import static org.jclouds.Constants.PROPERTY_MAX_RETRIES;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.route53.Route53Api;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

@Test(singleThreaded = true)
public class Route53ResourceRecordSetApiMockTest {
    static Set<Module> modules = ImmutableSet.<Module> of(new ExecutorServiceModule(sameThreadExecutor(),
            sameThreadExecutor()));

    static org.jclouds.route53.features.ResourceRecordSetApi mockRoute53Api(String uri) {
        Properties overrides = new Properties();
        overrides.setProperty(PROPERTY_MAX_RETRIES, "1");
        return ContextBuilder.newBuilder("aws-route53")
                             .credentials("accessKey", "secretKey")
                             .endpoint(uri)
                             .overrides(overrides)
                             .modules(modules)
                             .buildApi(Route53Api.class)
                             .getResourceRecordSetApiForHostedZone("Z1PA6795UKMFR9");
    }

    String weightedRecords = "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.foo.com.</Name><Type>CNAME</Type><SetIdentifier>Route53Service:us-east-1:PLATFORMSERVICE:i-7f0aec0d:20130313205017</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www1.foo.com.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet><ResourceRecordSet><Name>www.foo.com.</Name><Type>CNAME</Type><SetIdentifier>Route53Service:us-east-1:PLATFORMSERVICE:i-fbe41089:20130312203418</SetIdentifier><Weight>1</Weight><TTL>0</TTL><ResourceRecords><ResourceRecord><Value>www2.foo.com.</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";

    @Test
    public void listWeightedRecordSubsetsAggregateOnNameAndType() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(weightedRecords));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            assertEquals(api.list().next(),
                    cname("www.foo.com.", 0, ImmutableList.of("www1.foo.com.", "www2.foo.com.")));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");

            server.shutdown();
        }
    }

    @Test
    public void listByNameWeightedRecordSubsetsAggregateOnNameAndType() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(weightedRecords));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            assertEquals(api.listByName("www.foo.com.").next(),
                    cname("www.foo.com.", 0, ImmutableList.of("www1.foo.com.", "www2.foo.com.")));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com. HTTP/1.1");

            server.shutdown();
        }
    }

    @Test
    public void getByNameAndTypeWeightedRecordSubsetsAggregateOnNameAndType() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(weightedRecords));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            assertEquals(api.getByNameAndType("www.foo.com.", "CNAME").get(),
                    cname("www.foo.com.", 0, ImmutableList.of("www1.foo.com.", "www2.foo.com.")));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=CNAME HTTP/1.1");

            server.shutdown();
        }
    }

    String noRecords = "<ListResourceRecordSetsResponse><ResourceRecordSets></ResourceRecordSets></ListResourceRecordSetsResponse>";
    String createARecordSet = "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-02-29/\"><ChangeBatch><Changes><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";
    String changeSynced = "<GetChangeResponse><ChangeInfo><Id>/change/C2682N5HXP0BZ4</Id><Status>INSYNC</Status><SubmittedAt>2011-09-10T01:36:41.958Z</SubmittedAt></ChangeInfo></GetChangeResponse>";

    @Test
    public void addFirstRecordCreatesNewRRSet() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.add(a("www.foo.com.", 3600, "192.0.2.1"));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");

            RecordedRequest createRRSet = server.takeRequest();
            assertEquals(createRRSet.getRequestLine(), "POST /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
            assertEquals(new String(createRRSet.getBody()), createARecordSet);

            server.shutdown();
        }
    }

    String oneRecord = "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";
    String replaceWith2ElementRecordSet = "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-02-29/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";

    @Test
    public void addSecondRecordRecreatesRRSetAndRetainsTTL() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.add(a("www.foo.com.", "198.51.100.1"));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");

            RecordedRequest createRRSet = server.takeRequest();
            assertEquals(createRRSet.getRequestLine(), "POST /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
            assertEquals(new String(createRRSet.getBody()), replaceWith2ElementRecordSet);

            server.shutdown();
        }
    }

    String replaceWith2ElementRecordSetOverridingTTL = "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-02-29/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>10000000</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";

    @Test
    public void addSecondRecordRecreatesRRSetAndOverridesTTLWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.add(a("www.foo.com.", 10000000, "198.51.100.1"));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");

            RecordedRequest createRRSet = server.takeRequest();
            assertEquals(createRRSet.getRequestLine(), "POST /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
            assertEquals(new String(createRRSet.getBody()), replaceWith2ElementRecordSetOverridingTTL);

            server.shutdown();
        }
    }

    String deleteARecordSet = "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-02-29/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";

    @Test
    public void removeOnlyRecordDoesntAdd() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.remove(a("www.foo.com.", "192.0.2.1"));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");

            RecordedRequest deleteRRSet = server.takeRequest();
            assertEquals(deleteRRSet.getRequestLine(), "POST /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
            assertEquals(new String(deleteRRSet.getBody()), deleteARecordSet);

            server.shutdown();
        }
    }

    String twoRecords = "<ListResourceRecordSetsResponse><ResourceRecordSets><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></ResourceRecordSets></ListResourceRecordSetsResponse>";
    String replaceWith1ElementRecordSet = "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-02-29/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";

    @Test
    public void removeOneRecordReplacesRRSet() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.remove(a("www.foo.com.", "198.51.100.1"));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");

            RecordedRequest createRRSet = server.takeRequest();
            assertEquals(createRRSet.getRequestLine(), "POST /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
            assertEquals(new String(createRRSet.getBody()), replaceWith1ElementRecordSet);

            server.shutdown();
        }
    }

    @Test
    public void applyTTLDoesNothingWhenTTLIsExpected() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.applyTTLToNameAndType(3600, "www.foo.com.", "A");
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");
            server.shutdown();
        }
    }

    @Test
    public void applyTTLDoesNothingWhenRecordsArentFound() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.applyTTLToNameAndType(3600, "www.boo.com.", "A");
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.boo.com.&type=A HTTP/1.1");
            server.shutdown();
        }
    }

    String recreate2ElementRecordSetWithTTL = "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-02-29/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change><Change><Action>CREATE</Action><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>10000000</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";

    @Test
    public void applyTTLRecreatesRecordsWithSameRDataWhenDifferent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.applyTTLToNameAndType(10000000, "www.foo.com.", "A");
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");

            RecordedRequest createRRSet = server.takeRequest();
            assertEquals(createRRSet.getRequestLine(), "POST /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
            assertEquals(new String(createRRSet.getBody()), recreate2ElementRecordSetWithTTL);
            server.shutdown();
        }
    }

    @Test
    public void listByNameWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            assertEquals(api.listByName("www.foo.com.").next(),
                    a("www.foo.com.", 3600, ImmutableList.of("192.0.2.1", "198.51.100.1")));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com. HTTP/1.1");

            server.shutdown();
        }
    }

    @Test
    public void listByNameWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            assertFalse(api.listByName("www.foo.com.").hasNext());
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com. HTTP/1.1");

            server.shutdown();
        }
    }

    @Test
    public void getByNameAndTypeWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            assertEquals(api.getByNameAndType("www.foo.com.", "A").get(),
                    a("www.foo.com.", 3600, ImmutableList.of("192.0.2.1", "198.51.100.1")));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");

            server.shutdown();
        }
    }

    @Test
    public void getByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noRecords));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            assertEquals(api.getByNameAndType("www.foo.com.", "A"), Optional.absent());
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");

            server.shutdown();
        }
    }

    @Test
    public void replaceRecordSet() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.replace(a("www.foo.com.", 10000000, ImmutableSet.of("192.0.2.1", "198.51.100.1")));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");

            RecordedRequest createRRSet = server.takeRequest();
            assertEquals(createRRSet.getRequestLine(), "POST /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
            assertEquals(new String(createRRSet.getBody()), replaceWith2ElementRecordSetOverridingTTL);

            server.shutdown();
        }
    }

    @Test
    public void replaceRecordSetSkipsWhenEqual() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.replace(a("www.foo.com.", 3600, "192.0.2.1"));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");
            server.shutdown();
        }
    }

    @Test
    public void removeAbsentRecordDoesNothing() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.remove(a("www.foo.com.", "198.51.100.1"));
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");
            server.shutdown();
        }
    }

    String delete2ElementRecordSet = "<ChangeResourceRecordSetsRequest xmlns=\"https://route53.amazonaws.com/doc/2012-02-29/\"><ChangeBatch><Changes><Change><Action>DELETE</Action><ResourceRecordSet><Name>www.foo.com.</Name><Type>A</Type><TTL>3600</TTL><ResourceRecords><ResourceRecord><Value>192.0.2.1</Value></ResourceRecord><ResourceRecord><Value>198.51.100.1</Value></ResourceRecord></ResourceRecords></ResourceRecordSet></Change></Changes></ChangeBatch></ChangeResourceRecordSetsRequest>";

    @Test
    public void deleteRRSet() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(twoRecords));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(changeSynced));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.deleteByNameAndType("www.foo.com.", "A");
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www.foo.com.&type=A HTTP/1.1");

            RecordedRequest createRRSet = server.takeRequest();
            assertEquals(createRRSet.getRequestLine(), "POST /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset HTTP/1.1");
            assertEquals(new String(createRRSet.getBody()), delete2ElementRecordSet);

            server.shutdown();
        }
    }

    @Test
    public void deleteAbsentRRSDoesNothing() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(oneRecord));
        server.play();

        try {
            Route53ResourceRecordSetApi api = new Route53ResourceRecordSetApi(mockRoute53Api(server.getUrl("/")
                    .toString()));
            api.deleteByNameAndType("www1.foo.com.", "A");
        } finally {
            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(),
                    "GET /2012-02-29/hostedzone/Z1PA6795UKMFR9/rrset?name=www1.foo.com.&type=A HTTP/1.1");
            server.shutdown();
        }
    }
}
