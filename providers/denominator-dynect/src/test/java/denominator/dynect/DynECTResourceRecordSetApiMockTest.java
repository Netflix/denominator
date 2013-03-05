package denominator.dynect;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static denominator.model.ResourceRecordSets.a;
import static org.jclouds.Constants.PROPERTY_MAX_RETRIES;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.dynect.v3.DynECTApi;
import org.jclouds.dynect.v3.DynECTApiMetadata;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

@Test(singleThreaded = true)
public class DynECTResourceRecordSetApiMockTest {
    static Set<Module> modules = ImmutableSet.<Module> of(new ExecutorServiceModule(sameThreadExecutor(),
            sameThreadExecutor()));

    static DynECTApi mockDynECTApi(String uri) {
        Properties overrides = new Properties();
        overrides.setProperty(PROPERTY_MAX_RETRIES, "1");
        return ContextBuilder.newBuilder("dynect")
                             .credentials("jclouds:joe", "letmein")
                             .endpoint(uri)
                             .overrides(overrides)
                             .modules(modules)
                             .build(DynECTApiMetadata.CONTEXT_TOKEN).getApi();
    }

    String session = "{\"status\": \"success\", \"data\": {\"token\": \"FFFFFFFFFF\", \"version\": \"3.3.8\"}, \"job_id\": 254417252, \"msgs\": [{\"INFO\": \"login: Login successful\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
    String success = "{\"status\": \"success\", \"data\": {}, \"job_id\": 262989027, \"msgs\": [{\"INFO\": \"thing done\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    String createRecord1 = "{\"rdata\":{\"address\":\"1.2.3.4\"},\"ttl\":3600}";

    @Test
    public void addFirstRecordPostsAndPublishes() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(404)); // no existing records
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            DynECTResourceRecordSetApi api = new DynECTResourceRecordSetApi(
                    mockDynECTApi(server.getUrl("/").toString()), "foo.com");
            api.add(a("www.foo.com", 3600, "1.2.3.4"));
        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(), "GET /ARecord/foo.com/www.foo.com HTTP/1.1");

            RecordedRequest postRecord1 = server.takeRequest();
            assertEquals(postRecord1.getRequestLine(), "POST /ARecord/foo.com/www.foo.com HTTP/1.1");
            assertEquals(new String(postRecord1.getBody()), createRecord1);

            RecordedRequest publish = server.takeRequest();
            assertEquals(publish.getRequestLine(), "PUT /Zone/foo.com HTTP/1.1");
            assertEquals(new String(publish.getBody()), "{\"publish\":true}");

            server.shutdown();
        }
    }

    String recordIdsWithRecord1 = "{\"status\": \"success\", \"data\": [\"/REST/ARecord/foo.com/www.foo.com/1\"], \"job_id\": 273523368, \"msgs\": [{\"INFO\": \"get_tree: Here is your zone tree\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
    String record1Result = "{\"status\": \"success\", \"data\": {\"zone\": \"foo.com\", \"ttl\": 3600, \"fqdn\": \"www.foo.com\", \"record_type\": \"A\", \"rdata\": {\"address\": \"1.2.3.4\"}, \"record_id\": 1}, \"job_id\": 274279510, \"msgs\": [{\"INFO\": \"get: Found the record\", \"SOURCE\": \"API-B\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    @Test
    public void addAddExistingRecordDoesNothing() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(recordIdsWithRecord1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(record1Result));
        server.play();

        try {
            DynECTResourceRecordSetApi api = new DynECTResourceRecordSetApi(
                    mockDynECTApi(server.getUrl("/").toString()), "foo.com");
            api.add(a("www.foo.com", 3600, "1.2.3.4"));
        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(), "GET /ARecord/foo.com/www.foo.com HTTP/1.1");

            RecordedRequest getRecord1 = server.takeRequest();
            assertEquals(getRecord1.getRequestLine(), "GET /ARecord/foo.com/www.foo.com/1 HTTP/1.1");

            server.shutdown();
        }
    }

    String createRecord2 = "{\"rdata\":{\"address\":\"5.6.7.8\"},\"ttl\":3600}";

    @Test
    public void addSecondRecordPostsRecordWithOldTTLAndPublishes() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(recordIdsWithRecord1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(record1Result));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            DynECTResourceRecordSetApi api = new DynECTResourceRecordSetApi(
                    mockDynECTApi(server.getUrl("/").toString()), "foo.com");
            api.add(a("www.foo.com", "5.6.7.8"));
        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(), "GET /ARecord/foo.com/www.foo.com HTTP/1.1");

            RecordedRequest getRecord1 = server.takeRequest();
            assertEquals(getRecord1.getRequestLine(), "GET /ARecord/foo.com/www.foo.com/1 HTTP/1.1");

            RecordedRequest postRecord2 = server.takeRequest();
            assertEquals(postRecord2.getRequestLine(), "POST /ARecord/foo.com/www.foo.com HTTP/1.1");
            assertEquals(new String(postRecord2.getBody()), createRecord2);

            RecordedRequest publish = server.takeRequest();
            assertEquals(publish.getRequestLine(), "PUT /Zone/foo.com HTTP/1.1");
            assertEquals(new String(publish.getBody()), "{\"publish\":true}");

            server.shutdown();
        }
    }
    
    String createRecord1OverriddenTTL = "{\"rdata\":{\"address\":\"1.2.3.4\"},\"ttl\":10000000}";
    String createRecord2OverriddenTTL = "{\"rdata\":{\"address\":\"5.6.7.8\"},\"ttl\":10000000}";

    @Test
    public void addSecondRecordWithNewTTLRecreatesFirstRecordWithTTLAndPublishes() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(recordIdsWithRecord1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(record1Result));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            DynECTResourceRecordSetApi api = new DynECTResourceRecordSetApi(
                    mockDynECTApi(server.getUrl("/").toString()), "foo.com");
            api.add(a("www.foo.com", 10000000, "5.6.7.8"));
        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(), "GET /ARecord/foo.com/www.foo.com HTTP/1.1");

            RecordedRequest getRecord1 = server.takeRequest();
            assertEquals(getRecord1.getRequestLine(), "GET /ARecord/foo.com/www.foo.com/1 HTTP/1.1");

            RecordedRequest deleteRecord1 = server.takeRequest();
            assertEquals(deleteRecord1.getRequestLine(), "DELETE /ARecord/foo.com/www.foo.com/1 HTTP/1.1");

            RecordedRequest postRecord1 = server.takeRequest();
            assertEquals(postRecord1.getRequestLine(), "POST /ARecord/foo.com/www.foo.com HTTP/1.1");
            assertEquals(new String(postRecord1.getBody()), createRecord1OverriddenTTL);

            RecordedRequest postRecord2 = server.takeRequest();
            assertEquals(postRecord2.getRequestLine(), "POST /ARecord/foo.com/www.foo.com HTTP/1.1");
            assertEquals(new String(postRecord2.getBody()), createRecord2OverriddenTTL);

            RecordedRequest publish = server.takeRequest();
            assertEquals(publish.getRequestLine(), "PUT /Zone/foo.com HTTP/1.1");
            assertEquals(new String(publish.getBody()), "{\"publish\":true}");

            server.shutdown();
        }
    }

    @Test
    public void removeRecordSendsDeleteAndPublish() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(recordIdsWithRecord1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(record1Result));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            DynECTResourceRecordSetApi api = new DynECTResourceRecordSetApi(
                    mockDynECTApi(server.getUrl("/").toString()), "foo.com");
            api.remove(a("www.foo.com", "1.2.3.4"));
        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(), "GET /ARecord/foo.com/www.foo.com HTTP/1.1");

            RecordedRequest getRecord1 = server.takeRequest();
            assertEquals(getRecord1.getRequestLine(), "GET /ARecord/foo.com/www.foo.com/1 HTTP/1.1");

            RecordedRequest deleteRecord1 = server.takeRequest();
            assertEquals(deleteRecord1.getRequestLine(), "DELETE /ARecord/foo.com/www.foo.com/1 HTTP/1.1");

            RecordedRequest publish = server.takeRequest();
            assertEquals(publish.getRequestLine(), "PUT /Zone/foo.com HTTP/1.1");
            assertEquals(new String(publish.getBody()), "{\"publish\":true}");

            server.shutdown();
        }
    }

    String recordIdsWithRecords1And2 = "{\"status\": \"success\", \"data\": [\"/REST/ARecord/foo.com/www.foo.com/1\",\"/REST/ARecord/foo.com/www.foo.com/2\"], \"job_id\": 273523368, \"msgs\": [{\"INFO\": \"get_tree: Here is your zone tree\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
    String record2Result = "{\"status\": \"success\", \"data\": {\"zone\": \"foo.com\", \"ttl\": 3600, \"fqdn\": \"www.foo.com\", \"record_type\": \"A\", \"rdata\": {\"address\": \"5.6.7.8\"}, \"record_id\": 2}, \"job_id\": 274279510, \"msgs\": [{\"INFO\": \"get: Found the record\", \"SOURCE\": \"API-B\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    @Test
    public void removeRecord1ResultReplacesRRSet() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(recordIdsWithRecords1And2));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(record1Result));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(record2Result));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(success));
        server.play();

        try {
            DynECTResourceRecordSetApi api = new DynECTResourceRecordSetApi(
                    mockDynECTApi(server.getUrl("/").toString()), "foo.com");
            api.remove(a("www.foo.com", "5.6.7.8"));
        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(), "GET /ARecord/foo.com/www.foo.com HTTP/1.1");

            RecordedRequest getRecord1 = server.takeRequest();
            assertEquals(getRecord1.getRequestLine(), "GET /ARecord/foo.com/www.foo.com/1 HTTP/1.1");

            RecordedRequest getRecord2 = server.takeRequest();
            assertEquals(getRecord2.getRequestLine(), "GET /ARecord/foo.com/www.foo.com/2 HTTP/1.1");

            RecordedRequest deleteRecord2 = server.takeRequest();
            assertEquals(deleteRecord2.getRequestLine(), "DELETE /ARecord/foo.com/www.foo.com/2 HTTP/1.1");

            RecordedRequest publish = server.takeRequest();
            assertEquals(publish.getRequestLine(), "PUT /Zone/foo.com HTTP/1.1");
            assertEquals(new String(publish.getBody()), "{\"publish\":true}");


            server.shutdown();
        }
    }

    @Test
    public void removeWrongRecordDoesNothing() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(recordIdsWithRecord1));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(record1Result));

        server.play();

        try {
            DynECTResourceRecordSetApi api = new DynECTResourceRecordSetApi(
                    mockDynECTApi(server.getUrl("/").toString()), "foo.com");
            api.remove(a("www.foo.com", "5.6.7.8"));
        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listNameAndType = server.takeRequest();
            assertEquals(listNameAndType.getRequestLine(), "GET /ARecord/foo.com/www.foo.com HTTP/1.1");

            RecordedRequest getRecord1 = server.takeRequest();
            assertEquals(getRecord1.getRequestLine(), "GET /ARecord/foo.com/www.foo.com/1 HTTP/1.1");

            server.shutdown();
        }
    }
}
