package denominator.denominatord;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.mock.MockProvider;
import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import denominator.model.profile.Geo;
import denominator.model.profile.Weighted;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import feign.Feign;
import feign.FeignException;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;

import static denominator.model.ResourceRecordSets.a;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class DenominatorDTest {

  static DNSApiManager mock;
  static DenominatorD server;
  static DenominatorDApi client;

  @BeforeClass
  public static void start() throws IOException {
    mock = Denominator.create(new MockProvider());
    server = new DenominatorD(mock);
    int port = server.start();
    client = Feign.builder()
        .encoder(new GsonEncoder())
        .decoder(new GsonDecoder())
        .target(DenominatorDApi.class, "http://localhost:" + port);

    mock.api().basicRecordSetsInZone("denominator.io.").put(ResourceRecordSet.<AData>builder()
        .name("www.denominator.io.")
        .type("A")
        .add(AData.create("192.0.2.1")).build());

    mock.api().weightedRecordSetsInZone("denominator.io.").put(ResourceRecordSet.<CNAMEData>builder()
        .name("www.weighted.denominator.io.")
        .type("CNAME")
        .qualifier("EU-West")
        .weighted(Weighted.create(1))
        .add(CNAMEData.create("www1.denominator.io.")).build());

    mock.api().weightedRecordSetsInZone("denominator.io.").put(ResourceRecordSet.<CNAMEData>builder()
        .name("www.weighted.denominator.io.")
        .type("CNAME")
        .qualifier("US-West")
        .weighted(Weighted.create(1))
        .add(CNAMEData.create("www2.denominator.io.")).build());
  }

  @AfterClass
  public static void stop() throws IOException {
    server.shutdown();
  }

  @Test
  public void healthcheckOK() {
    assertThat(client.healthcheck().status()).isEqualTo(200);
  }

  @Test
  public void zones() {
    assertThat(client.zones())
        .containsAll(mock.api().zones());
  }

  @Test
  public void zonesByName() {
    assertThat(client.zonesByName("denominator.io."))
        .containsAll(toList(mock.api().zones().iterateByName("denominator.io.")));
  }

  @Test
  public void putZone_new() {
    Zone zone = Zone.create(null, "putzone_new.denominator.io.", 86400, "nil@denominator.io");
    String id =
        client.putZone(zone).headers().get("Location").iterator().next().replace("/zones/", "");
    assertThat(mock.api().zones().iterateByName(zone.name()))
        .containsExactly(Zone.create(id, zone.name(), zone.ttl(), zone.email()));
  }

  @Test
  public void putZone_update() {
    Zone zone = Zone.create(null, "putzone_update.denominator.io.", 86400, "nil@denominator.io");
    String id = mock.api().zones().put(zone);

    Zone update = Zone.create(id, zone.name(), 300, "test@denominator.io");
    client.putZone(update);
    assertThat(mock.api().zones().iterateByName(zone.name()))
        .containsExactly(update);
  }

  @Test
  public void deleteZone() {
    Zone zone = Zone.create(null, "zonetest.denominator.io.", 86400, "nil@denominator.io");
    String id = mock.api().zones().put(zone);

    client.deleteZone(id);
    assertThat(mock.api().zones().iterateByName(zone.name()))
        .isEmpty();
  }

  @Test
  public void recordSets() {
    assertThat(client.recordSets("denominator.io.")).isNotEmpty();
  }

  @Test
  public void recordSetsWrongZoneIs400() {
    try {
      client.recordSets("moomoo.io.");
    } catch (FeignException e) {
      assertThat(e.getMessage()).startsWith("status 400");
    }
  }

  @Test
  public void recordSetsByName() {
    ResourceRecordSet<AData> a =
        a("www.denominator.io.", asList("192.0.2.1", "198.51.100.1", "203.0.113.1"));
    mock.api().basicRecordSetsInZone("denominator.io.").put(a);

    assertThat(client.recordSetsByName("denominator.io.", a.name()))
        .containsExactly(a);
  }

  @Test
  public void recordSetsByNameWhenNotFound() {
    assertThat(client.recordSetsByName("denominator.io.", "moomoo.denominator.io.")).isEmpty();
  }

  @Test
  public void recordSetsByNameAndType() {
    assertThat(client.recordSetsByNameAndType("denominator.io.", "denominator.io.", "NS"))
        .containsAll(toList(mock.api().recordSetsInZone("denominator.io.")
                                .iterateByNameAndType("denominator.io.", "NS")));
  }

  @Test
  public void recordSetsByNameAndTypeWhenNotFound() {
    assertThat(client.recordSetsByNameAndType("denominator.io.", "denominator.io.", "A")).isEmpty();
  }

  @Test
  public void recordSetsByNameTypeAndQualifier() {
    ResourceRecordSet<CNAMEData> weighted = ResourceRecordSet.<CNAMEData>builder()
        .name("www.weighted.denominator.io.")
        .type("A")
        .qualifier("EU-West")
        .add(CNAMEData.create("www.denominator.io."))
        .weighted(Weighted.create(10))
        .build();

    mock.api().weightedRecordSetsInZone("denominator.io.").put(weighted);

    assertThat(client.recordsetsByNameAndTypeAndQualifier("denominator.io.",
                                                          weighted.name(), weighted.type(),
                                                          weighted.qualifier()))
        .containsOnly(weighted);
  }

  @Test
  public void recordSetsByNameTypeAndQualifierWhenNotFound() {
    assertThat(client.recordsetsByNameAndTypeAndQualifier("denominator.io.",
                                                          "www.weighted.denominator.io.", "CNAME",
                                                          "AU-West")).isEmpty();
  }

  @Test
  public void deleteRecordSetByNameAndType() {
    client.deleteRecordSetByNameAndType("denominator.io.", "www1.denominator.io.", "A");
    assertThat(client.recordSetsByNameAndType("denominator.io.", "www1.denominator.io.", "A"))
        .isEmpty();
  }

  @Test
  public void deleteRecordSetByNameAndTypeWhenNotFound() {
    client.deleteRecordSetByNameAndType("denominator.io.", "denominator.io.", "A");
  }

  @Test
  public void deleteRecordSetByNameTypeAndQualifier() {
    client.deleteRecordSetByNameTypeAndQualifier("denominator.io.", "www.weighted.denominator.io.",
                                                 "CNAME", "US-West");
    assertThat(client.recordsetsByNameAndTypeAndQualifier("denominator.io.",
                                                          "www.weighted.denominator.io.", "CNAME",
                                                          "US-West")).isEmpty();
  }

  @Test
  public void deleteRecordSetByNameTypeAndQualifierWhenNotFound() {
    client.deleteRecordSetByNameTypeAndQualifier("denominator.io.", "www.weighted.denominator.io.",
                                                 "CNAME", "AU-West");
  }

  @Test
  public void putRecordSet() {
    Map<String, Collection<String>> antarctica = new LinkedHashMap<String, Collection<String>>();
    antarctica.put("Antarctica",
                   Arrays.asList("Bouvet Island", "French Southern Territories", "Antarctica"));

    ResourceRecordSet<CNAMEData> recordSet = ResourceRecordSet.<CNAMEData>builder()
        .name("www.beta.denominator.io.")
        .type("CNAME")
        .qualifier("Antarctica")
        .ttl(300)
        .add(CNAMEData.create("www-south.denominator.io."))
        .geo(Geo.create(antarctica))
        .build();
    client.putRecordSet("denominator.io.", recordSet);
    assertThat(
        client.recordSetsByNameAndType("denominator.io.", recordSet.name(), recordSet.type()))
        .containsOnly(recordSet);
  }

  static <T> List<T> toList(Iterator<T> iterator) {
    List<T> inMock = new ArrayList<T>();
    while (iterator.hasNext()) {
      inMock.add(iterator.next());
    }
    return inMock;
  }
}
