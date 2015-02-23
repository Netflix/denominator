package denominator.dynect;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.model.rdata.CNAMEData;
import denominator.profile.GeoResourceRecordSetApi;

import static denominator.dynect.DynECTTest.allGeoPermissions;
import static denominator.dynect.DynECTTest.noGeoPermissions;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

@Test(singleThreaded = true)
public class DynECTGeoResourceRecordSetApiMockTest {

  MockDynECTServer server;

  String noGeoServices = "{\"status\": \"success\", \"data\": [] }";
  String geoService;
  ResourceRecordSet<CNAMEData> europe = ResourceRecordSet.<CNAMEData>builder()
      .name("srv.denominator.io")
      .type("CNAME")
      .qualifier("Europe")
      .ttl(300)
      .add(CNAMEData.create("srv-000000001.eu-west-1.elb.amazonaws.com."))
      .geo(Geo.create(new LinkedHashMap<String, Collection<String>>() {
        {
          put("13", Arrays.asList("13"));
        }
      }))
      .build();
  ResourceRecordSet<CNAMEData> everywhereElse = ResourceRecordSet.<CNAMEData>builder()
      .name("srv.denominator.io")
      .type("CNAME")
      .qualifier("Everywhere Else")
      .ttl(300)
      .add(CNAMEData.create("srv-000000001.us-east-1.elb.amazonaws.com."))
      .geo(Geo.create(new LinkedHashMap<String, Collection<String>>() {
        {
          put("11", Arrays.asList("11"));
          put("16", Arrays.asList("16"));
          put("12", Arrays.asList("12"));
          put("17", Arrays.asList("17"));
          put("15", Arrays.asList("15"));
          put("14", Arrays.asList("14"));
        }
      }))
      .build();
  ResourceRecordSet<CNAMEData> fallback = ResourceRecordSet.<CNAMEData>builder()
      .name("srv.denominator.io")
      .type("CNAME")
      .qualifier("Fallback")
      .ttl(300)
      .add(CNAMEData.create("srv-000000002.us-east-1.elb.amazonaws.com."))
      .geo(Geo.create(new LinkedHashMap<String, Collection<String>>() {
        {
          put("Unknown IP", Arrays.asList("@!"));
          put("Fallback", Arrays.asList("@@"));
        }
      }))
      .build();

  DynECTGeoResourceRecordSetApiMockTest() throws IOException {
    geoService =
        Util.slurp(new InputStreamReader(getClass().getResourceAsStream("/geoservice.json")));
  }

  @Test
  public void listWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    ;
    server.enqueue(new MockResponse().setBody(allGeoPermissions));
    server.enqueue(new MockResponse().setBody(geoService));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io");
    Iterator<ResourceRecordSet<?>> iterator = api.iterator();
    assertEquals(iterator.next(), everywhereElse);
    assertEquals(iterator.next(), europe);
    assertEquals(iterator.next(), fallback);
    assertFalse(iterator.hasNext());

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/Geo?detail=Y");
  }

  @Test
  public void iterateByNameWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(allGeoPermissions));
    server.enqueue(new MockResponse().setBody(geoService));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io");
    Iterator<ResourceRecordSet<?>> iterator = api.iterateByName("srv.denominator.io");
    assertEquals(iterator.next(), everywhereElse);
    assertEquals(iterator.next(), europe);
    assertEquals(iterator.next(), fallback);
    assertFalse(iterator.hasNext());

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/Geo?detail=Y");
  }

  @Test
  public void iterateByNameWhenNoPermissions() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(noGeoPermissions));

    assertNull(server.connect().api().geoRecordSetsInZone("denominator.io"));

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport");
  }

  @Test
  public void iterateByNameWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(allGeoPermissions));
    server.enqueue(new MockResponse().setBody(noGeoServices));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io");
    assertFalse(api.iterateByName("www.denominator.io").hasNext());

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/Geo?detail=Y");
  }

  @Test
  public void iterateByNameAndTypeWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(allGeoPermissions));
    server.enqueue(new MockResponse().setBody(geoService));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io");
    Iterator<ResourceRecordSet<?>>
        iterator =
        api.iterateByNameAndType("srv.denominator.io", "CNAME");
    assertEquals(iterator.next(), everywhereElse);
    assertEquals(iterator.next(), europe);
    assertEquals(iterator.next(), fallback);
    assertFalse(iterator.hasNext());

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/Geo?detail=Y");
  }

  @Test
  public void iterateByNameAndTypeWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(allGeoPermissions));
    server.enqueue(new MockResponse().setBody(noGeoServices));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io");
    assertFalse(api.iterateByNameAndType("www.denominator.io", "A").hasNext());

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/Geo?detail=Y");
  }

  @Test
  public void getByNameTypeAndQualifierWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(allGeoPermissions));
    server.enqueue(new MockResponse().setBody(geoService));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io");
    assertEquals(api.getByNameTypeAndQualifier("srv.denominator.io", "CNAME", "Fallback"),
                 fallback);

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/Geo?detail=Y");
  }

  @Test
  public void getByNameTypeAndQualifierWhenAbsent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(allGeoPermissions));
    server.enqueue(new MockResponse().setBody(noGeoServices));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io");
    assertNull(api.getByNameTypeAndQualifier("www.denominator.io", "A", "Fallback"));

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/Geo?detail=Y");
  }

  @BeforeMethod
  public void resetServer() throws IOException {
    server = new MockDynECTServer();
  }

  @AfterMethod
  public void shutdownServer() throws IOException {
    server.shutdown();
  }
}
