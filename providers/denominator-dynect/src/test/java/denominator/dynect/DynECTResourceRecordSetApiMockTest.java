package denominator.dynect;

import static com.google.common.io.Resources.getResource;
import static denominator.CredentialsConfiguration.credentials;
import static denominator.model.ResourceRecordSets.a;
import static org.jclouds.util.Strings2.toStringAndClose;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

import denominator.Denominator;
import denominator.ResourceRecordSetApi;
import denominator.model.ResourceRecordSet;

@Test(singleThreaded = true)
public class DynECTResourceRecordSetApiMockTest {

    String session = "{\"status\": \"success\", \"data\": {\"token\": \"FFFFFFFFFF\", \"version\": \"3.3.8\"}, \"job_id\": 254417252, \"msgs\": [{\"INFO\": \"login: Login successful\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
    String success = "{\"status\": \"success\", \"data\": {}, \"job_id\": 262989027, \"msgs\": [{\"INFO\": \"thing done\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    String createRecord1 = "{\"rdata\":{\"address\":\"192.0.2.1\"},\"ttl\":3600}";

    @Test
    public void addFirstRecordPostsAndPublishes() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.add(a("www.denominator.io", 3600, "192.0.2.1"));

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
    public void addAddExistingRecordDoesNothing() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.add(a("www.denominator.io", 3600, "192.0.2.1"));

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    String createRecord2 = "{\"rdata\":{\"address\":\"198.51.100.1\"},\"ttl\":3600}";

    @Test
    public void addSecondRecordPostsRecordWithOldTTLAndPublishes() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.add(a("www.denominator.io", "198.51.100.1"));

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
    
    String createRecord1OverriddenTTL = "{\"rdata\":{\"address\":\"192.0.2.1\"},\"ttl\":10000000}";
    String createRecord2OverriddenTTL = "{\"rdata\":{\"address\":\"198.51.100.1\"},\"ttl\":10000000}";

    @Test
    public void addSecondRecordWithNewTTLRecreatesFirstRecordWithTTLAndPublishes() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.add(a("www.denominator.io", 10000000, "198.51.100.1"));

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
    public void removeRecordSendsDeleteAndPublish() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.remove(a("www.denominator.io", "192.0.2.1"));

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

    String records1And2 = "{\"status\": \"success\", \"data\": [{\"zone\": \"denominator.io\", \"ttl\": 3600, \"fqdn\": \"www.denominator.io\", \"record_type\": \"A\", \"rdata\": {\"address\": \"192.0.2.1\"}, \"record_id\": 1}, {\"zone\": \"denominator.io\", \"ttl\": 3600, \"fqdn\": \"www.denominator.io\", \"record_type\": \"A\", \"rdata\": {\"address\": \"198.51.100.1\"}, \"record_id\": 2}], \"job_id\": 273523368, \"msgs\": [{\"INFO\": \"get_tree: Here is your zone tree\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    @Test
    public void removeRecord1ResultReplacesRRSet() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1And2));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.remove(a("www.denominator.io", "198.51.100.1"));

            assertEquals(server.getRequestCount(), 4);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "DELETE /ARecord/denominator.io/www.denominator.io/2 HTTP/1.1");

            RecordedRequest publish = server.takeRequest();
            assertEquals(publish.getRequestLine(), "PUT /Zone/denominator.io HTTP/1.1");
            assertEquals(new String(publish.getBody()), "{\"publish\":true}");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void applyTTLDoesNothingWhenTTLIsExpected() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1And2));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.applyTTLToNameAndType(3600, "www.denominator.io", "A");

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void applyTTLDoesNothingWhenRecordsArentFound() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.applyTTLToNameAndType(3600, "www.boo.com", "A");

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.boo.com?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void applyTTLRecreatesRecordsWithSameRDataWhenDifferent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1And2));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.applyTTLToNameAndType(10000000, "www.denominator.io", "A");

            assertEquals(server.getRequestCount(), 7);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "DELETE /ARecord/denominator.io/www.denominator.io/1 HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "DELETE /ARecord/denominator.io/www.denominator.io/2 HTTP/1.1");

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

    String records;
    String recordsByName;

    DynECTResourceRecordSetApiMockTest() throws IOException {
        records = toStringAndClose(getResource("records.json").openStream());
        recordsByName = toStringAndClose(getResource("recordsByName.json").openStream());
    }

    @Test
    public void listWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
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

    @Test
    public void listWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertFalse(api.iterator().hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /AllRecord/denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(recordsByName));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
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
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
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
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1And2));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertEquals(api.getByNameAndType("www.denominator.io", "A").get(),
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
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertEquals(api.getByNameAndType("www.denominator.io", "A"), Optional.absent());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void replaceRecordSet() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.replace(a("www.denominator.io", 10000000, ImmutableSet.of("192.0.2.1", "198.51.100.1")));

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
    public void replaceRecordSetSkipsWhenEqual() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.replace(a("www.denominator.io", 3600, "192.0.2.1"));

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void removeAbsentRecordDoesNothing() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.remove(a("www.denominator.io", "198.51.100.1"));

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
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(records1And2));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.deleteByNameAndType("www.denominator.io", "A");

            assertEquals(server.getRequestCount(), 5);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
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
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.play();

        try {
            ResourceRecordSetApi api = mockApi(server.getUrl("/"));
            api.deleteByNameAndType("www.denominator.io", "A");

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /ARecord/denominator.io/www.denominator.io?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    private static ResourceRecordSetApi mockApi(final URL url) {
        return Denominator.create(new DynECTProvider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, credentials("jclouds", "joe", "letmein")).api().basicRecordSetsInZone("denominator.io");
    }
}
