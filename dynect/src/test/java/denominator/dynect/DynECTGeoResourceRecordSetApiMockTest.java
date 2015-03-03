package denominator.dynect;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;

import denominator.common.Util;
import denominator.model.ResourceRecordSet;
import denominator.model.profile.Geo;
import denominator.model.rdata.CNAMEData;
import denominator.profile.GeoResourceRecordSetApi;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.dynect.DynECTTest.allGeoPermissions;
import static denominator.dynect.DynECTTest.noGeoPermissions;

public class DynECTGeoResourceRecordSetApiMockTest {

  @Rule
  public MockDynECTServer server = new MockDynECTServer();

  @Test
  public void listWhenPresent() throws Exception {
    server.enqueueSessionResponse();
    server.enqueue(new MockResponse().setBody(allGeoPermissions));
    server.enqueue(new MockResponse().setBody(geoService));

    GeoResourceRecordSetApi api = server.connect().api().geoRecordSetsInZone("denominator.io");
    assertThat(api.iterator())
        .containsExactly(everywhereElse, europe, fallback);

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
    assertThat(api.iterateByName("srv.denominator.io"))
        .containsExactly(everywhereElse, europe, fallback);

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

    assertThat(server.connect().api().geoRecordSetsInZone("denominator.io")).isNull();

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
    assertThat(api.iterateByName("www.denominator.io")).isEmpty();

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
    assertThat(api.iterateByNameAndType("srv.denominator.io", "CNAME"))
        .containsOnly(everywhereElse, europe, fallback);

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
    assertThat(api.iterateByNameAndType("www.denominator.io", "A")).isEmpty();

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
    assertThat(api.getByNameTypeAndQualifier("srv.denominator.io", "CNAME", "Fallback"))
        .isEqualTo(fallback);

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
    assertThat(api.getByNameTypeAndQualifier("www.denominator.io", "A", "Fallback")).isNull();

    server.assertSessionRequest();
    server.assertRequest()
        .hasMethod("POST")
        .hasPath("/CheckPermissionReport");
    server.assertRequest()
        .hasMethod("GET")
        .hasPath("/Geo?detail=Y");
  }

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

  public DynECTGeoResourceRecordSetApiMockTest() throws IOException {
    geoService =
        Util.slurp(new InputStreamReader(getClass().getResourceAsStream("/geoservice.json")));
  }
}
