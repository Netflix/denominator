package denominator.dynect;

import static denominator.CredentialsConfiguration.credentials;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.jclouds.util.Strings2;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.io.Resources;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

import denominator.Denominator;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.model.rdata.CNAMEData;
import denominator.profile.GeoResourceRecordSetApi;

@Test(singleThreaded = true)
public class DynECTGeoResourceRecordSetApiMockTest {

    String session = "{\"status\": \"success\", \"data\": {\"token\": \"FFFFFFFFFF\", \"version\": \"3.3.8\"}, \"job_id\": 254417252, \"msgs\": [{\"INFO\": \"login: Login successful\", \"SOURCE\": \"BLL\", \"ERR_CD\": null, \"LVL\": \"INFO\"}]}";

    String noGeoServices = "{\"status\": \"success\", \"data\": [] }";
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
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoService));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));
            Iterator<ResourceRecordSet<?>> iterator = api.iterator();
            assertEquals(iterator.next(), everywhereElse);
            assertEquals(iterator.next(), europe);
            assertEquals(iterator.next(), fallback);
            assertFalse(iterator.hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /Geo?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoService));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));
            Iterator<ResourceRecordSet<?>> iterator = api.iterateByName("srv.denominator.io");
            assertEquals(iterator.next(), everywhereElse);
            assertEquals(iterator.next(), europe);
            assertEquals(iterator.next(), fallback);
            assertFalse(iterator.hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /Geo?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noGeoServices));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertFalse(api.iterateByName("www.denominator.io").hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /Geo?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameAndTypeWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoService));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));
            Iterator<ResourceRecordSet<?>> iterator = api.iterateByNameAndType("srv.denominator.io", "CNAME");
            assertEquals(iterator.next(), everywhereElse);
            assertEquals(iterator.next(), europe);
            assertEquals(iterator.next(), fallback);
            assertFalse(iterator.hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /Geo?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void iterateByNameAndTypeWhenAbsent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(noGeoServices));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertFalse(api.iterateByNameAndType("www.denominator.io", "A").hasNext());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /Geo?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void getByNameTypeAndGroupWhenPresent() throws IOException, InterruptedException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(session));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(geoService));
        server.play();

        try {
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertEquals(api.getByNameTypeAndGroup("srv.denominator.io", "CNAME", "Fallback").get(), fallback);

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /Geo?detail=Y HTTP/1.1");
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
            GeoResourceRecordSetApi api = mockApi(server.getUrl("/"));
            assertFalse(api.getByNameTypeAndGroup("www.denominator.io", "A", "Fallback").isPresent());

            assertEquals(server.getRequestCount(), 2);
            assertEquals(server.takeRequest().getRequestLine(), "POST /Session HTTP/1.1");
            assertEquals(server.takeRequest().getRequestLine(), "GET /Geo?detail=Y HTTP/1.1");
        } finally {
            server.shutdown();
        }
    }

    private static GeoResourceRecordSetApi mockApi(final URL url) {
        return Denominator.create(new DynECTProvider() {
            @Override
            public String url() {
                return url.toString();
            }
        }, credentials("jclouds", "joe", "letmein")).api()
                                                    .geoRecordSetsInZone("denominator.io").get();
    }
}
