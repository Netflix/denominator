package denominator.dynect;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static denominator.CredentialsConfiguration.credentials;
import static denominator.dynect.DynECTProviderDynamicUpdateMockTest.session;
import static denominator.dynect.DynECTTest.noneWithName;
import static denominator.dynect.DynECTTest.noneWithNameAndType;
import static denominator.model.ResourceRecordSets.a;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.util.Iterator;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

import denominator.Denominator;
import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

@Test(singleThreaded = true)
public class DynECTResourceRecordSetApiMockTest {

    String records;
    String recordsByName;

    DynECTResourceRecordSetApiMockTest() throws IOException {
        records = Resources.toString(getResource("records.json"), UTF_8);
        recordsByName = Resources.toString(getResource("recordsByName.json"), UTF_8);
    }

    String success = "{\"status\": \"success\", \"data\": {}, \"job_id\": 262989027, \"msgs\": [{\"INFO\": \"thing done\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    String createRecord1 = ""//
            + "{\n"//
            + "  \"ttl\": 3600,\n"//
            + "  \"rdata\": {\n"//
            + "    \"address\": \"192.0.2.1\"\n"//
            + "  }\n"//
            + "}";

    @Test
    public void putFirstRecordPostsAndPublishes() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithNameAndType));
        server.enqueue(new MockResponse().setBody(success));
        server.enqueue(new MockResponse().setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            api.put(a("www.denominator.io", 3600, "192.0.2.1"));

            assertEquals(server.getRequestCount(), 4);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");

            RecordedRequest postRecord1 = server.takeRequest();
            assertEquals(postRecord1.getRequestLine(), "POST /ARecord/denominator.io/www.denominator.io HTTP/1.1");
            assertEquals(new String(postRecord1.getBody()), createRecord1);

            RecordedRequest publish = server.takeRequest();
            assertEquals(publish.getRequestLine(), "PUT /Zone/denominator.io HTTP/1.1");
            assertEquals(new String(publish.getBody()), "{\"publish\":true}");
        } finally {
            server.shutdown();
        }
    }

    String records1 = "{\"status\": \"success\", \"data\": [{\"zone\": \"denominator.io\", \"ttl\": 3600, \"fqdn\": \"www.denominator.io\", \"record_type\": \"A\", \"rdata\": {\"address\": \"192.0.2.1\"}, \"record_id\": 1}], \"job_id\": 273523368, \"msgs\": [{\"INFO\": \"get_tree: Here is your zone tree\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    @Test
    public void putExistingRecordDoesNothing() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setBody(records1));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            api.put(a("www.denominator.io", 3600, "192.0.2.1"));

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    String createRecord2 =  ""//
            + "{\n"//
            + "  \"ttl\": 3600,\n"//
            + "  \"rdata\": {\n"//
            + "    \"address\": \"198.51.100.1\"\n"//
            + "  }\n"//
            + "}";

    @Test
    public void putSecondRecordPostsRecordPublishes() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setBody(records1));
        server.enqueue(new MockResponse().setBody(success));
        server.enqueue(new MockResponse().setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            api.put(a("www.denominator.io", 3600, ImmutableSet.of("192.0.2.1", "198.51.100.1")));

            assertEquals(server.getRequestCount(), 4);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");

            RecordedRequest postRecord2 = server.takeRequest();
            assertEquals(postRecord2.getRequestLine(), "POST /ARecord/denominator.io/www.denominator.io HTTP/1.1");
            assertEquals(new String(postRecord2.getBody()), createRecord2);

            RecordedRequest publish = server.takeRequest();
            assertEquals(publish.getRequestLine(), "PUT /Zone/denominator.io HTTP/1.1");
            assertEquals(new String(publish.getBody()), "{\"publish\":true}");
        } finally {
            server.shutdown();
        }
    }

    String createRecord1OverriddenTTL = ""//
            + "{\n"//
            + "  \"ttl\": 10000000,\n"//
            + "  \"rdata\": {\n"//
            + "    \"address\": \"192.0.2.1\"\n"//
            + "  }\n"//
            + "}";

    String createRecord2OverriddenTTL = ""//
            + "{\n"//
            + "  \"ttl\": 10000000,\n"//
            + "  \"rdata\": {\n"//
            + "    \"address\": \"198.51.100.1\"\n"//
            + "  }\n"//
            + "}";

    @Test
    public void putReplacingRecordSet() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setBody(records1));
        server.enqueue(new MockResponse().setBody(success));
        server.enqueue(new MockResponse().setBody(success));
        server.enqueue(new MockResponse().setBody(success));
        server.enqueue(new MockResponse().setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            api.put(a("www.denominator.io", 10000000, ImmutableSet.of("192.0.2.1", "198.51.100.1")));

            assertEquals(server.getRequestCount(), 6);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "DELETE /ARecord/denominator.io/www.denominator.io/1 HTTP/1.1");

            RecordedRequest postRecord1 = server.takeRequest();
            assertEquals(postRecord1.getRequestLine(), "POST /ARecord/denominator.io/www.denominator.io HTTP/1.1");
            assertEquals(new String(postRecord1.getBody()), createRecord1OverriddenTTL);

            RecordedRequest postRecord2 = server.takeRequest();
            assertEquals(postRecord2.getRequestLine(), "POST /ARecord/denominator.io/www.denominator.io HTTP/1.1");
            assertEquals(new String(postRecord2.getBody()), createRecord2OverriddenTTL);

            RecordedRequest publish = server.takeRequest();
            assertEquals(publish.getRequestLine(), "PUT /Zone/denominator.io HTTP/1.1");
            assertEquals(new String(publish.getBody()), "{\"publish\":true}");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void putRecordSetSkipsWhenEqual() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setBody(records1));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            api.put(a("www.denominator.io", 3600, "192.0.2.1"));

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    String recordIds1And2 = "{\"status\": \"success\", \"data\": [\"/REST/ARecord/denominator.io/www.denominator.io/1\", \"/REST/ARecord/denominator.io/www.denominator.io/2\"], \"job_id\": 273523368, \"msgs\": [{\"INFO\": \"get_tree: Here is your zone tree\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
    String records1And2 = "{\"status\": \"success\", \"data\": [{\"zone\": \"denominator.io\", \"ttl\": 3600, \"fqdn\": \"www.denominator.io\", \"record_type\": \"A\", \"rdata\": {\"address\": \"192.0.2.1\"}, \"record_id\": 1}, {\"zone\": \"denominator.io\", \"ttl\": 3600, \"fqdn\": \"www.denominator.io\", \"record_type\": \"A\", \"rdata\": {\"address\": \"198.51.100.1\"}, \"record_id\": 2}], \"job_id\": 273523368, \"msgs\": [{\"INFO\": \"get_tree: Here is your zone tree\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    @Test
    public void putOneLessRecordSendsDeleteAndPublish() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setBody(records1And2));
        server.enqueue(new MockResponse().setBody(success));
        server.enqueue(new MockResponse().setBody(success));
        server.enqueue(new MockResponse().setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            api.put(a("www.denominator.io", 3600, "198.51.100.1"));

            assertEquals(server.getRequestCount(), 4);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "DELETE /ARecord/denominator.io/www.denominator.io/1 HTTP/1.1");

            RecordedRequest publish = server.takeRequest();
            assertEquals(publish.getRequestLine(), "PUT /Zone/denominator.io HTTP/1.1");
            assertEquals(new String(publish.getBody()), "{\"publish\":true}");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void listWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setBody(records));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            Iterator<ResourceRecordSet<?>> iterator = api.iterator();
            iterator.next();
            iterator.next();
            assertEquals(iterator.next(),
                    a("www.denominator.io", 3600, ImmutableList.of("192.0.2.1", "198.51.100.1")));

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /AllRecord/denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "zone denominator.io not found")
    public void listWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithName));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            api.iterator().hasNext();
        } finally {
            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /AllRecord/denominator.io?detail=Y HTTP/1.1");
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setBody(recordsByName));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            assertEquals(api.iterateByName("www.denominator.io").next(),
                    a("www.denominator.io", 3600, ImmutableList.of("192.0.2.1", "198.51.100.1")));

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /AllRecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithName));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            assertFalse(api.iterateByName("www.denominator.io").hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /AllRecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void getByNameAndTypeWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setBody(records1And2));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            assertEquals(api.getByNameAndType("www.denominator.io", "A"),
                    a("www.denominator.io", 3600, ImmutableList.of("192.0.2.1", "198.51.100.1")));

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void getByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithNameAndType));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            assertNull(api.getByNameAndType("www.denominator.io", "A"));

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void deleteRRSet() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setBody(recordIds1And2));
        server.enqueue(new MockResponse().setBody(success));
        server.enqueue(new MockResponse().setBody(success));
        server.enqueue(new MockResponse().setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            api.deleteByNameAndType("www.denominator.io", "A");

            assertEquals(server.getRequestCount(), 5);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "DELETE /ARecord/denominator.io/www.denominator.io/1 HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "DELETE /ARecord/denominator.io/www.denominator.io/2 HTTP/1.1");

            RecordedRequest publish = server.takeRequest();
            assertEquals(publish.getRequestLine(), "PUT /Zone/denominator.io HTTP/1.1");
            assertEquals(new String(publish.getBody()), "{\"publish\":true}");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void deleteAbsentRRSDoesNothing() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404).setBody(noneWithNameAndType));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getPort());
            api.deleteByNameAndType("www.denominator.io", "A");

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    private static ResourceRecordSetApi mockApi(final int port) {
        return Denominator.create(new DynECTProvider() {
            @Override
            public String url() {
                return "http://localhost:" + port;
            }
        }, credentials("jclouds", "joe", "letmein")).api().basicRecordSetsInZone("denominator.io");
    }
}
