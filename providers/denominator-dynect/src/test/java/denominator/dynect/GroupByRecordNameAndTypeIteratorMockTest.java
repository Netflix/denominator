package denominator.dynect;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static denominator.model.ResourceRecordSets.ns;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jclouds.Constants.PROPERTY_MAX_RETRIES;
import static org.jclouds.dynect.v3.domain.RecordId.recordIdBuilder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.dynect.v3.DynECTApi;
import org.jclouds.dynect.v3.domain.RecordId;
import org.jclouds.dynect.v3.domain.RecordId.Builder;
import org.jclouds.dynect.v3.features.RecordApi;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.model.ResourceRecordSet;
import denominator.model.rdata.SOAData;

@Test(singleThreaded = true)
public class GroupByRecordNameAndTypeIteratorMockTest {
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
                             .buildApi(DynECTApi.class);
    }

    Builder<?> builder = recordIdBuilder().zone("denominator.io").fqdn("denominator.io");

    ImmutableList<RecordId> recordIds = ImmutableList.<RecordId> builder()
                                                     .add(builder.type("SOA").id(50976579l).build())
                                                     .add(builder.type("NS").id(50976580l).build())
                                                     .add(builder.type("NS").id(50976580l).build())
                                                     .build();

    String session = "{\"status\": \"success\", \"data\": {\"token\": \"FFFFFFFFFF\", \"version\": \"3.3.8\"}, \"job_id\": 254417252, \"msgs\": [{\"INFO\": \"login: Login successful\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    String soa = "{\"status\": \"success\", \"data\": {\"zone\": \"denominator.io\", \"ttl\": 3600, \"fqdn\": \"denominator.io\", \"record_type\": \"SOA\", \"rdata\": {\"rname\": \"admin.denominator.io.\", \"retry\": 600, \"mname\": \"ns1.p28.dynect.net.\", \"minimum\": 60, \"refresh\": 3600, \"expire\": 604800, \"serial\": 1}, \"record_id\": 50976579, \"serial_style\": \"increment\"}, \"job_id\": 273523378, \"msgs\": [{\"INFO\": \"get: Found the record\", \"SOURCE\": \"API-B\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";
    String ns = "{\"status\": \"success\", \"data\": {\"zone\": \"denominator.io\", \"ttl\": 86400, \"fqdn\": \"denominator.io\", \"record_type\": \"NS\", \"rdata\": {\"nsdname\": \"ns4.p28.dynect.net.\"}, \"record_id\": 50976580}, \"job_id\": 274279510, \"msgs\": [{\"INFO\": \"get: Found the record\", \"SOURCE\": \"API-B\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";   

    /**
     * until dynect supports {@code GET ?detail=y}, we have to do a get on each record.
     */
    public void getOnEachRecordAggregatingSameNameAndType() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).addHeader(CONTENT_TYPE, APPLICATION_JSON).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).addHeader(CONTENT_TYPE, APPLICATION_JSON).setBody(soa));
        server.enqueue(new MockResponse().setResponseCode(200).addHeader(CONTENT_TYPE, APPLICATION_JSON).setBody(ns));
        server.enqueue(new MockResponse().setResponseCode(200).addHeader(CONTENT_TYPE, APPLICATION_JSON).setBody(ns));
        server.play();
        
        try {
            RecordApi api = mockDynECTApi(server.getUrl("/").toString()).getRecordApiForZone("denominator.io");

            Iterator<ResourceRecordSet<?>> iterator  = new GroupByRecordNameAndTypeIterator(api, recordIds.iterator());
            assertEquals(iterator.next().toString(), ResourceRecordSet.<SOAData> builder()
                                                           .name("denominator.io")
                                                           .type("SOA")
                                                           .ttl(3600)
                                                           .add(SOAData.builder()
                                                                       .rname("admin.denominator.io.")
                                                                       .mname("ns1.p28.dynect.net.")
                                                                       .serial(1)
                                                                       .refresh(3600)
                                                                       .retry(600)
                                                                       .expire(604800)
                                                                       .minimum(60).build()).build().toString());
            assertEquals(iterator.next(), ns("denominator.io", 86400, ImmutableList.of("ns4.p28.dynect.net.", "ns4.p28.dynect.net.")));
            assertFalse(iterator.hasNext());
        } finally {
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /SOARecord/denominator.io/denominator.io/50976579 HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /NSRecord/denominator.io/denominator.io/50976580 HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /NSRecord/denominator.io/denominator.io/50976580 HTTP/1.1");
            server.shutdown();
        }
    }
}
