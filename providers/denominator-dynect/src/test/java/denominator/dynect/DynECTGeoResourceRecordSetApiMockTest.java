package denominator.dynect;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static org.jclouds.Constants.PROPERTY_MAX_RETRIES;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.jclouds.ContextBuilder;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.jclouds.dynect.v3.DynECTApi;
import org.jclouds.util.Strings2;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.google.inject.Module;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

import dagger.ObjectGraph;
import dagger.Provides;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.model.rdata.CNAMEData;
import denominator.profile.GeoResourceRecordSetApi;

@Test(singleThreaded = true)
public class DynECTGeoResourceRecordSetApiMockTest {

    String session = "{\"status\": \"success\", \"data\": {\"token\": \"FFFFFFFFFF\", \"version\": \"3.3.8\"}, \"job_id\": 254417252, \"msgs\": [{\"INFO\": \"login: Login successful\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    String noGeoServices = "{\"status\": \"success\", \"data\": [] }";
    String geoServices = "{\"status\": \"success\", \"data\": [\"/REST/Geo/CCS/\"] }";
    String geoService;

    DynECTGeoResourceRecordSetApiMockTest() throws IOException{
        geoService = Strings2.toStringAndClose(Resources.getResource("geoservice.json").openStream());
    }

    ResourceRecordSet<CNAMEData> europe = ResourceRecordSet.<CNAMEData> builder()
            .name("srv.denominator.io")
            .type("CNAME")
            .ttl(300)
            .add(CNAMEData.create("srv-000000001.eu-west-1.elb.amazonaws.com."))
            .addProfile(Geo.create("Europe", ImmutableMultimap.of("13", "13")))
            .build();

    ResourceRecordSet<CNAMEData> everywhereElse = ResourceRecordSet.<CNAMEData> builder()
            .name("srv.denominator.io")
            .type("CNAME")
            .ttl(300)
            .add(CNAMEData.create("srv-000000001.us-east-1.elb.amazonaws.com."))
            .addProfile(Geo.create("Everywhere Else",
                                ImmutableMultimap.<String, String> builder()
                                                 .put("11", "11")
                                                 .put("16", "16")
                                                 .put("12", "12")
                                                 .put("17", "17")
                                                 .put("15", "15")
                                                 .put("14", "14").build()))                                                   
            .build();
    
    ResourceRecordSet<CNAMEData> fallback = ResourceRecordSet.<CNAMEData> builder()
            .name("srv.denominator.io")
            .type("CNAME")
            .ttl(60)
            .add(CNAMEData.create("srv-000000002.us-east-1.elb.amazonaws.com."))
            .addProfile(Geo.create("Fallback",
                                ImmutableMultimap.<String, String> builder()
                                                 .put("@!", "@!")
                                                 .put("@@", "@@").build()))
            .build();

    @Test
    public void listWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoServices));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoService));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockedGeoApiForZone(server, "denominator.io");
                         
            Iterator<ResourceRecordSet<?>> iterator = api.list();
            assertEquals(iterator.next(), everywhereElse);
            assertEquals(iterator.next(), europe);
            assertEquals(iterator.next(), fallback);
            assertFalse(iterator.hasNext());

            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listGeos = server.takeRequest();
            assertEquals(listGeos.getRequestLine(), "GET /Geo HTTP/1.1");

            RecordedRequest getGeo = server.takeRequest();
            assertEquals(getGeo.getRequestLine(), "GET /Geo/CCS HTTP/1.1");

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void listByNameWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoServices));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoService));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockedGeoApiForZone(server, "denominator.io");
                                                    
            Iterator<ResourceRecordSet<?>> iterator = api.listByName("srv.denominator.io");
            assertEquals(iterator.next(), everywhereElse);
            assertEquals(iterator.next(), europe);
            assertEquals(iterator.next(), fallback);
            assertFalse(iterator.hasNext());

            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listGeos = server.takeRequest();
            assertEquals(listGeos.getRequestLine(), "GET /Geo HTTP/1.1");

            RecordedRequest getGeo = server.takeRequest();
            assertEquals(getGeo.getRequestLine(), "GET /Geo/CCS HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void listByNameWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noGeoServices));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockedGeoApiForZone(server, "denominator.io");

            assertFalse(api.listByName("www.denominator.io").hasNext());
            
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listGeos = server.takeRequest();
            assertEquals(listGeos.getRequestLine(), "GET /Geo HTTP/1.1");

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void listByNameAndTypeWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoServices));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoService));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockedGeoApiForZone(server, "denominator.io");
            
            Iterator<ResourceRecordSet<?>> iterator = api.listByNameAndType("srv.denominator.io", "CNAME");
            assertEquals(iterator.next(), everywhereElse);
            assertEquals(iterator.next(), europe);
            assertEquals(iterator.next(), fallback);
            assertFalse(iterator.hasNext());

            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listGeos = server.takeRequest();
            assertEquals(listGeos.getRequestLine(), "GET /Geo HTTP/1.1");

            RecordedRequest getGeo = server.takeRequest();
            assertEquals(getGeo.getRequestLine(), "GET /Geo/CCS HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void listByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noGeoServices));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockedGeoApiForZone(server, "denominator.io");

            assertFalse(api.listByNameAndType("www.denominator.io", "A").hasNext());

            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listGeos = server.takeRequest();
            assertEquals(listGeos.getRequestLine(), "GET /Geo HTTP/1.1");

        } finally {
            server.shutdown();
        }
    }

    @Test
    public void getByNameTypeAndGroupWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoServices));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoService));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockedGeoApiForZone(server, "denominator.io");
            
            assertEquals(api.getByNameTypeAndGroup("srv.denominator.io", "CNAME", "Fallback").get(), fallback);

            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listGeos = server.takeRequest();
            assertEquals(listGeos.getRequestLine(), "GET /Geo HTTP/1.1");

            RecordedRequest getGeo = server.takeRequest();
            assertEquals(getGeo.getRequestLine(), "GET /Geo/CCS HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void getByNameTypeAndGroupWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noGeoServices));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockedGeoApiForZone(server, "denominator.io");

            assertFalse(api.getByNameTypeAndGroup("www.denominator.io", "A", "Fallback").isPresent());

            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");

            RecordedRequest listGeos = server.takeRequest();
            assertEquals(listGeos.getRequestLine(), "GET /Geo HTTP/1.1");

        } finally {
            server.shutdown();
        }
    }

    private static GeoResourceRecordSetApi mockedGeoApiForZone(MockWebServer server, String zoneName) {
        return ObjectGraph.create(new Mock(mockDynECTApi(server.getUrl("/").toString())))
                .get(DynECTGeoResourceRecordSetApi.Factory.class).create(zoneName).get();
    }

    @dagger.Module(entryPoints = DynECTGeoResourceRecordSetApi.Factory.class, complete = false)
    private static class Mock extends DynECTGeoSupport {
        private final DynECTApi api;

        private Mock(DynECTApi api) {
            this.api = api;
        }

        @Provides
        DynECTApi provideApi() {
            return api;
        }
    }

    private static DynECTApi mockDynECTApi(String uri) {
        Properties overrides = new Properties();
        overrides.setProperty(PROPERTY_MAX_RETRIES, "1");
        return ContextBuilder.newBuilder("dynect")
                             .credentials("jclouds:joe", "letmein")
                             .endpoint(uri)
                             .overrides(overrides)
                             .modules(modules)
                             .buildApi(DynECTApi.class);
    }

    private static Set<Module> modules = ImmutableSet.<Module> of(
            new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()));
}
