package denominator.dynect;

import static javax.ws.rs.core.Response.Status.OK;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.URL;

import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import feign.Feign;

/**
 * 
 * @author Adrian Cole
 */
public class DynECTTest {

    String running = "{\"status\": \"running\", \"data\": {}, \"job_id\": 274509427, \"msgs\": [{\"INFO\": \"token: This session already has a job running\", \"SOURCE\": \"API-B\", \"ERR_CD\": \"OPERATION_FAILED\", \"LVL\": \"ERROR\"}]}";

    @Test
    public void runningDoesntRetry() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(running));
        server.play();

        DynECT api = mockApi(server.getUrl(""));

        try {
            api.zones();
            fail();
        } catch (DynECTException e) {
            assertEquals(server.getRequestCount(), 1);
            assertEquals(e.getMessage(), "DynECT#zones() status running: [OPERATION_FAILED: token: This session already has a job running]");

            assertEquals(e.status(), "running");
            assertEquals(e.messages().size(), 1);
            assertEquals(e.messages().get(0).code().get(), "OPERATION_FAILED");
            assertEquals(e.messages().get(0).info(), "token: This session already has a job running");
        } finally {
            server.shutdown();
        }
    }

    String taskBlocking = "[{\"status\": \"failure\", \"data\": {}, \"job_id\": 275545493, \"msgs\": [{\"INFO\": \"zone: Operation blocked by current task\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"ILLEGAL_OPERATION\", \"LVL\": \"ERROR\"}, {\"INFO\": \"task_name: ProvisionZone\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}, {\"INFO\": \"task_id: 39120953\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}]";

    @Test
    public void blockedDoesntRetry() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(taskBlocking));
        server.play();

        DynECT api = mockApi(server.getUrl(""));

        try {
            api.zones();
            fail();
        } catch (DynECTException e) {
            assertEquals(server.getRequestCount(), 1);
            assertEquals(e.getMessage(), "DynECT#zones() status failure: [ILLEGAL_OPERATION: zone: Operation blocked by current task, task_name: ProvisionZone, task_id: 39120953]");

            assertEquals(e.status(), "failure");
            assertEquals(e.messages().size(), 3);
            assertEquals(e.messages().get(0).code().get(), "ILLEGAL_OPERATION");
            assertEquals(e.messages().get(0).info(), "zone: Operation blocked by current task");
            assertFalse(e.messages().get(1).code().isPresent());
            assertEquals(e.messages().get(1).info(), "task_name: ProvisionZone");
            assertFalse(e.messages().get(1).code().isPresent());
            assertEquals(e.messages().get(2).info(), "task_id: 39120953");
        } finally {
            server.shutdown();
        }
    }

    String targetExists = "[{\"status\": \"failure\", \"data\": {}, \"job_id\": 275533917, \"msgs\": [{\"INFO\": \"name: Name already exists\", \"SOURCE\": \"BLL\", \"ERR_CD\": \"TARGET_EXISTS\", \"LVL\": \"ERROR\"}, {\"INFO\": \"create: You already have this zone.\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}]";

    @Test
    public void alreadyExistsDoesntRetry() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(OK.getStatusCode()).setBody(targetExists));
        server.play();

        DynECT api = mockApi(server.getUrl(""));

        try {
            api.zones();
            fail();
        } catch (DynECTException e) {
            assertEquals(server.getRequestCount(), 1);
            assertEquals(e.getMessage(), "DynECT#zones() status failure: [TARGET_EXISTS: name: Name already exists, create: You already have this zone.]");

            assertEquals(e.status(), "failure");
            assertEquals(e.messages().size(), 2);
            assertEquals(e.messages().get(0).code().get(), "TARGET_EXISTS");
            assertEquals(e.messages().get(0).info(), "name: Name already exists");
            assertFalse(e.messages().get(1).code().isPresent());
            assertEquals(e.messages().get(1).info(), "create: You already have this zone.");
        } finally {
            server.shutdown();
        }
    }

    static javax.inject.Provider<String> lazyToken = new javax.inject.Provider<String>() {

        @Override
        public String get() {
            return "FFFFFFFFFF";
        }

    };

    static DynECT mockApi(final URL url) {
        return Feign.create(new DynECTTarget(new DynECTProvider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, lazyToken), new DynECTProvider.FeignModule());
    }
}
