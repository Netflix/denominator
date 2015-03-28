package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import denominator.Credentials;
import denominator.ultradns.UltraDNS.DirectionalGroup;
import denominator.ultradns.UltraDNS.DirectionalRecord;
import denominator.ultradns.UltraDNS.NameAndType;
import denominator.ultradns.UltraDNS.NetworkStatus;
import denominator.ultradns.UltraDNS.Record;
import feign.Feign;
import feign.codec.DecodeException;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.ultradns.UltraDNSException.DIRECTIONALPOOL_NOT_FOUND;
import static denominator.ultradns.UltraDNSException.DIRECTIONALPOOL_RECORD_NOT_FOUND;
import static denominator.ultradns.UltraDNSException.GROUP_NOT_FOUND;
import static denominator.ultradns.UltraDNSException.INVALID_ZONE_NAME;
import static denominator.ultradns.UltraDNSException.POOL_ALREADY_EXISTS;
import static denominator.ultradns.UltraDNSException.POOL_NOT_FOUND;
import static denominator.ultradns.UltraDNSException.POOL_RECORD_ALREADY_EXISTS;
import static denominator.ultradns.UltraDNSException.RESOURCE_RECORD_ALREADY_EXISTS;
import static denominator.ultradns.UltraDNSException.RESOURCE_RECORD_NOT_FOUND;
import static denominator.ultradns.UltraDNSException.SYSTEM_ERROR;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.groups.Tuple.tuple;

public class UltraDNSTest {

  @Rule
  public final MockUltraDNSServer server = new MockUltraDNSServer();
  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void networkGood() throws Exception {
    server.enqueue(new MockResponse().setBody(getNeustarNetworkStatusResponse));

    assertThat(mockApi().getNeustarNetworkStatus()).isEqualTo(NetworkStatus.GOOD);

    server.assertSoapBody(getNeustarNetworkStatus);
  }

  @Test
  public void networkFailed() throws Exception {
    server.enqueue(new MockResponse().setBody(getNeustarNetworkStatusFailedResponse));

    assertThat(mockApi().getNeustarNetworkStatus()).isEqualTo(NetworkStatus.FAILED);

    server.assertSoapBody(getNeustarNetworkStatus);
  }

  @Test
  public void retryOnSystemError() throws Exception {
    server.enqueueError(SYSTEM_ERROR, "System Error");
    server.enqueue(new MockResponse().setBody(getNeustarNetworkStatusResponse));

    assertThat(mockApi().getNeustarNetworkStatus()).isEqualTo(NetworkStatus.GOOD);

    server.assertSoapBody(getNeustarNetworkStatus);
    server.assertSoapBody(getNeustarNetworkStatus);
  }

  @Test
  public void noRetryOnInvalidUser() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage("UltraDNS#getNeustarNetworkStatus() failed: Invalid User");

    server.enqueue(new MockResponse().setResponseCode(500).setBody(invalidUser));

    mockApi().getNeustarNetworkStatus();
  }

  @Test
  public void networkStatusCantParse() throws Exception {
    thrown.expect(DecodeException.class);
    thrown.expectMessage("Content is not allowed in prolog.");

    server.enqueue(new MockResponse().setBody("{\"foo\": \"bar\"}"));

    mockApi().getNeustarNetworkStatus();
  }

  @Test
  public void accountId() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));

    assertThat(mockApi().getAccountsListOfUser()).isEqualTo("AAAAAAAAAAAAAAAA");

    server.assertSoapBody(getAccountsListOfUser);
  }

  @Test
  public void zonesOfAccountPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getZonesOfAccountResponsePresent));

    assertThat(mockApi().getZonesOfAccount("AAAAAAAAAAAAAAAA"))
        .containsExactly("denominator.io.");

    server.assertSoapBody(getZonesOfAccount);
  }

  @Test
  public void zonesOfAccountAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));

    assertThat(mockApi().getZonesOfAccount("AAAAAAAAAAAAAAAA")).isEmpty();

    server.assertSoapBody(getZonesOfAccount);
  }

  @Test
  public void recordsInZonePresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponsePresent));

    List<Record> records = mockApi().getResourceRecordsOfZone("denominator.io.");
    assertThat(records).extracting("id", "created")
        .containsExactly(
            tuple("0B0338C2023F7969", 1255348943000L),
            tuple("04023A2507B6468F", 1286038636000L)
        );
    assertThat(records).extracting("name", "typeCode", "ttl", "rdata")
        .containsExactly(
            tuple("denominator.io.", 2, 86400, asList("pdns2.ultradns.net.")),
            tuple("www.denominator.io.", 1, 3600, asList("1.2.3.4"))
        );

    server.assertSoapBody(getResourceRecordsOfZone);
  }

  @Test
  public void recordsInZoneAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfZoneResponseAbsent));

    assertThat(mockApi().getResourceRecordsOfZone("denominator.io.")).isEmpty();

    server.assertSoapBody(getResourceRecordsOfZone);
  }

  @Test
  public void recordsInZoneByNameAndTypePresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponsePresent));

    assertThat(mockApi().getResourceRecordsOfDNameByType("denominator.io.", "denominator.io.", 6))
        .extracting("id", "created", "name", "typeCode", "ttl", "rdata")
        .contains(
            tuple("04053D8E57C7A22F", 1361521368000L, "denominator.io.", 6, 86400, asList(
                "pdns75.ultradns.com.",
                "adrianc.netflix.com.",
                "2013022200",
                "86400",
                "86400",
                "86400",
                "86400"))
        );

    server.assertSoapBody(getResourceRecordsOfDNameByType);
  }

  @Test
  public void recordsInZoneByNameAndTypeAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponseAbsent));

    assertThat(mockApi().getResourceRecordsOfDNameByType("denominator.io.", "denominator.io.", 6))
        .isEmpty();

    server.assertSoapBody(getResourceRecordsOfDNameByType);
  }

  @Test
  public void recordsInZoneByNameAndTypeInvalidZone() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage("2507: Invalid zone name.");

    server.enqueueError(INVALID_ZONE_NAME, "Invalid zone name.");

    mockApi().getResourceRecordsOfDNameByType("ARGHH", "ARGHH", 6);
  }

  @Test
  public void createRecordInZone() throws Exception {
    server.enqueue(new MockResponse().setBody(createResourceRecordResponse));

    Record record = new Record();
    record.name = "mail.denominator.io.";
    record.typeCode = 15;
    record.ttl = 1800;
    record.rdata.add("10");
    record.rdata.add("maileast.denominator.io.");
    mockApi().createResourceRecord(record, "denominator.io.");

    server.assertSoapBody(createResourceRecord);
  }

  @Test
  public void updateRecordInZone() throws Exception {
    server.enqueue(new MockResponse().setBody(updateResourceRecordResponse));

    Record record = new Record();
    record.id = "ABCDEF";
    record.name = "www.denominator.io.";
    record.typeCode = 1;
    record.ttl = 3600;
    record.rdata.add("1.1.1.1");
    mockApi().updateResourceRecord(record, "denominator.io.");

    server.assertSoapBody(
        "<v01:updateResourceRecord><transactionID /><resourceRecord Guid=\"ABCDEF\" ZoneName=\"denominator.io.\" Type=\"1\" DName=\"www.denominator.io.\" TTL=\"3600\"><InfoValues Info1Value=\"1.1.1.1\" /></resourceRecord></v01:updateResourceRecord>");
  }

  @Test
  public void updateRecordOfRRPool() throws Exception {
    server.enqueue(new MockResponse().setBody(updateResourceRecordResponse));

    mockApi().updateRecordOfRRPool("ABCDEF", "000000000000002", "1.1.1.1", 3600);

    server.assertSoapBody("<v01:updateRecordOfRRPool>\n"
                          + "    <transactionID/>\n"
                          + "    <resourceRecord TTL=\"3600\" info1Value=\"1.1.1.1\" lbPoolID=\"000000000000002\" rrGuid=\"ABCDEF\"/>\n"
                          + "</v01:updateRecordOfRRPool>");
  }

  /**
   * Granted, this doesn't make sense, but testing found update to return this error.
   */
  @Test
  public void updateRecordInZoneAlreadyExists() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage(
        "2111: Resource Record of type 1 with these attributes already exists in the system.");

    server.enqueueError(RESOURCE_RECORD_ALREADY_EXISTS,
                        "Resource Record of type 1 with these attributes already exists in the system.");

    Record record = new Record();
    record.id = "ABCDEF";
    record.name = "www.denominator.io.";
    record.typeCode = 1;
    record.ttl = 3600;
    record.rdata.add("1.1.1.1");
    mockApi().updateResourceRecord(record, "denominator.io.");
  }

  @Test
  public void deleteRecord() throws Exception {
    server.enqueue(new MockResponse().setBody(deleteResourceRecordResponse));

    mockApi().deleteResourceRecord("ABCDEF");

    server.assertSoapBody(deleteResourceRecord);
  }

  @Test
  public void rrPoolNameTypeToIdInZonePresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getLoadBalancingPoolsByZoneResponsePresent));

    NameAndType one = new NameAndType();
    one.name = "app-uswest1.denominator.io.";
    one.type = "A";

    NameAndType two = new NameAndType();
    two.name = "app-uswest2.denominator.io.";
    two.type = "A";

    assertThat(mockApi().getLoadBalancingPoolsByZone("denominator.io."))
        .containsEntry(one, "000000000000002")
        .containsEntry(two, "000000000000003");

    server.assertSoapBody(getLoadBalancingPoolsByZone);
  }

  @Test
  public void rrPoolNameTypeToIdInZoneAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getLoadBalancingPoolsByZoneResponseAbsent));

    assertThat(mockApi().getLoadBalancingPoolsByZone("denominator.io.")).isEmpty();

    server.assertSoapBody(getLoadBalancingPoolsByZone);
  }

  @Test
  public void recordsInRRPoolPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getRRPoolRecordsResponsePresent));

    assertThat(mockApi().getRRPoolRecords("000000000000002"))
        .extracting("id", "created", "name", "typeCode", "ttl", "rdata")
        .containsExactly(
            tuple("0B0338C2023F7969", 1255348943000L, "denominator.io.", 2, 86400, asList(
                "pdns2.ultradns.net.")),
            tuple("04023A2507B6468F", 1286038636000L, "www.denominator.io.", 1, 3600, asList(
                "1.2.3.4"))
        );

    server.assertSoapBody(getRRPoolRecords);
  }

  @Test
  public void recordsInRRPoolAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getRRPoolRecordsResponseAbsent));

    assertThat(mockApi().getRRPoolRecords("000000000000002")).isEmpty();

    server.assertSoapBody(getRRPoolRecords);
  }

  @Test
  public void createRRPoolInZoneForNameAndType() throws Exception {
    server.enqueue(new MockResponse().setBody(addRRLBPoolResponse));

    assertThat(mockApi().addRRLBPool("denominator.io.", "www.denominator.io.", 1))
        .isEqualTo("AAAAAAAAAAAAAAAA");

    server.assertSoapBody(addRRLBPool);
  }

  @Test
  public void createRRPoolInZoneForNameAndTypeWhenAlreadyExists() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage("2912: Pool already created for this host name : www.denominator.io.");

    server.enqueueError(POOL_ALREADY_EXISTS,
                        "Pool already created for this host name : www.denominator.io.");

    mockApi().addRRLBPool("denominator.io.", "www.denominator.io.", 1);
  }

  @Test
  public void addRecordIntoRRPoolInZone() throws Exception {
    server.enqueue(new MockResponse().setBody(addRecordToRRPoolResponse));

    mockApi().addRecordToRRPool(1, 300, "www1.denominator.io.", "AAAAAAAAAAAAAAAA",
                                "denominator.io.");

    server.assertSoapBody(addRecordToRRPool);
  }

  @Test
  public void deleteRRPool() throws Exception {
    server.enqueue(new MockResponse().setBody(deleteLBPoolResponse));

    mockApi().deleteLBPool("AAAAAAAAAAAAAAAA");

    server.assertSoapBody(deleteLBPool);
  }

  @Test
  public void deleteRRPoolWhenPoolNotFound() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage("2911: Pool does not exist in the system");

    server.enqueueError(POOL_NOT_FOUND, "Pool does not exist in the system");

    mockApi().deleteLBPool("AAAAAAAAAAAAAAAA");
  }

  @Test
  public void deleteRRPoolWhenRecordNotFound() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage("2103: No resource record with GUID found in the system AAAAAAAAAAAAAAAA");

    server.enqueueError(RESOURCE_RECORD_NOT_FOUND,
                        "No resource record with GUID found in the system AAAAAAAAAAAAAAAA");

    mockApi().deleteLBPool("AAAAAAAAAAAAAAAA");
  }

  @Test
  public void dirPoolNameToIdsInZonePresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getDirectionalPoolsOfZoneResponsePresent));

    assertThat(mockApi().getDirectionalPoolsOfZone("denominator.io."))
        .containsEntry("www.denominator.io.", "D000000000000001");

    server.assertSoapBody(getDirectionalPoolsOfZone);
  }

  @Test
  public void dirPoolNameToIdsInZoneAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getDirectionalPoolsOfZoneResponseAbsent));

    assertThat(mockApi().getDirectionalPoolsOfZone("denominator.io.")).isEmpty();

    server.assertSoapBody(getDirectionalPoolsOfZone);
  }

  @Test
  public void directionalRecordsByNameAndTypePresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponsePresent));

    List<DirectionalRecord> records = mockApi().getDirectionalDNSRecordsForHost(
        "denominator.io.", "www.denominator.io.", 0);

    assertThat(records).extracting("geoGroupId", "geoGroupName", "noResponseRecord", "id")
        .containsExactly(
            tuple("C000000000000001", "Europe", false, "A000000000000001"),
            tuple("C000000000000003", "Everywhere Else", false, "A000000000000003"),
            tuple("C000000000000002", "US", false, "A000000000000002")
        );
    assertThat(records).extracting("type", "ttl", "rdata")
        .containsExactly(
            tuple("CNAME", 300, asList("www-000000001.eu-west-1.elb.amazonaws.com.")),
            tuple("CNAME", 60, asList("www-000000002.us-east-1.elb.amazonaws.com.")),
            tuple("CNAME", 300, asList("www-000000001.us-east-1.elb.amazonaws.com."))
        );

    server.assertSoapBody(getDirectionalDNSRecordsForHost);
  }

  @Test
  public void directionalRecordsByNameAndTypeAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForHostResponseAbsent));

    assertThat(
        mockApi().getDirectionalDNSRecordsForHost("denominator.io.", "www.denominator.io.", 0))
        .isEmpty();

    server.assertSoapBody(getDirectionalDNSRecordsForHost);
  }

  @Test
  public void directionalRecordsByNameAndTypeWhenPoolNotFound() {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage(
        "2142: No Pool or Multiple pools of same type exists for the PoolName : www.denominator.io.");

    server.enqueueError(DIRECTIONALPOOL_NOT_FOUND,
                        "No Pool or Multiple pools of same type exists for the PoolName : www.denominator.io.");

    mockApi().getDirectionalDNSRecordsForHost("denominator.io.", "www.denominator.io.", 0);
  }

  @Test
  public void directionalRecordsByNameAndTypeFiltersOutSourceIP() throws Exception {
    server.enqueue(
        new MockResponse().setBody(getDirectionalDNSRecordsForHostResponseFiltersOutSourceIP));

    List<DirectionalRecord> records = mockApi().getDirectionalDNSRecordsForHost(
        "denominator.io.", "www.denominator.io.", 0);

    assertThat(records).extracting("geoGroupId", "geoGroupName", "noResponseRecord", "id")
        .containsExactly(tuple("C000000000000002", "US", false, "A000000000000002"));
    assertThat(records).extracting("type", "ttl", "rdata")
        .containsExactly(tuple("CNAME", 300, asList("www-000000001.us-east-1.elb.amazonaws.com.")));

    server.assertSoapBody(getDirectionalDNSRecordsForHost);
  }

  @Test
  public void directionalRecordsInZoneAndGroupByNameAndTypePresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponsePresent));

    List<DirectionalRecord> records = mockApi().getDirectionalDNSRecordsForGroup(
        "denominator.io.", "Europe", "www.denominator.io.", 1);

    assertThat(records).extracting("geoGroupId", "geoGroupName", "noResponseRecord", "id")
        .containsExactly(tuple("C000000000000001", "Europe", false, "A000000000000001"));
    assertThat(records).extracting("type", "ttl", "rdata")
        .containsExactly(tuple("CNAME", 300, asList("www-000000001.eu-west-1.elb.amazonaws.com.")));

    server.assertSoapBody(getDirectionalDNSRecordsForGroup);
  }

  @Test
  public void directionalRecordsInZoneAndGroupByNameAndTypeAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getDirectionalDNSRecordsForGroupResponseAbsent));

    assertThat(mockApi().getDirectionalDNSRecordsForGroup("denominator.io.", "Europe",
                                                          "www.denominator.io.", 1))
        .isEmpty();

    server.assertSoapBody(getDirectionalDNSRecordsForGroup);
  }

  @Test
  public void directionalRecordsInZoneAndGroupByNameAndTypeWhenGroupNotFound() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage("4003: Group does not exist.");

    server.enqueueError(GROUP_NOT_FOUND, "Group does not exist.");

    mockApi().getDirectionalDNSRecordsForGroup("denominator.io.", "Europe",
                                               "www.denominator.io.", 1);
  }

  @Test
  public void createDirectionalPoolInZoneForNameAndType() throws Exception {
    server.enqueue(new MockResponse().setBody(addDirectionalPoolResponse));

    assertThat(mockApi().addDirectionalPool("denominator.io.", "www.denominator.io.", "A"))
        .isEqualTo("AAAAAAAAAAAAAAAA");

    server.assertSoapBody(addDirectionalPool);
  }

  @Test
  public void createDirectionalPoolInZoneForNameAndTypeWhenAlreadyExists() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage("2912: Pool already created for this host name : www.denominator.io.");

    server.enqueueError(POOL_ALREADY_EXISTS,
                        "Pool already created for this host name : www.denominator.io.");

    mockApi().addDirectionalPool("denominator.io.", "www.denominator.io.", "A");
  }

  @Test
  public void deleteDirectionalPool() throws Exception {
    server.enqueue(new MockResponse().setBody(deleteDirectionalPoolResponse));

    mockApi().deleteDirectionalPool("AAAAAAAAAAAAAAAA");

    server.assertSoapBody(deleteDirectionalPool);
  }

  @Test
  public void deleteDirectionalRecord() throws Exception {
    server.enqueue(new MockResponse().setBody(deleteDirectionalPoolRecordResponse));

    mockApi().deleteDirectionalPoolRecord("00000000000");

    server.assertSoapBody(deleteDirectionalPoolRecord);
  }

  @Test
  public void deleteDirectionalRecordNotFound() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage("2705: Directional Pool Record does not exist in the system");

    server.enqueueError(DIRECTIONALPOOL_RECORD_NOT_FOUND,
                        "Directional Pool Record does not exist in the system");

    mockApi().deleteDirectionalPoolRecord("00000000000");
  }

  @Test
  public void createDirectionalRecordAndGroupInPool() throws Exception {
    server.enqueue(new MockResponse().setBody(addDirectionalPoolRecordResponse));

    DirectionalRecord record = new DirectionalRecord();
    record.name = "mail.denominator.io.";
    record.type = "MX";
    record.ttl = 1800;
    record.rdata.add("10");
    record.rdata.add("maileast.denominator.io.");

    DirectionalGroup group = new DirectionalGroup();
    group.name = "Mexas";
    group.regionToTerritories.put("United States (US)", asList("Maryland", "Texas"));

    assertThat(mockApi().addDirectionalPoolRecord(record, group, "AAAAAAAAAAAAAAAA"))
        .isEqualTo("06063DC355058294");

    server.assertSoapBody(addDirectionalPoolRecord);
  }

  @Test
  public void createDirectionalRecordAndGroupInPoolWhenExists() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage("4009: Resource Record already exists.");

    server.enqueueError(POOL_RECORD_ALREADY_EXISTS, "Resource Record already exists.");

    DirectionalRecord record = new DirectionalRecord();
    record.name = "mail.denominator.io.";
    record.type = "MX";
    record.ttl = 1800;
    record.rdata.add("10");
    record.rdata.add("maileast.denominator.io.");

    DirectionalGroup group = new DirectionalGroup();
    group.name = "Mexas";
    group.regionToTerritories.put("United States (US)", asList("Maryland", "Texas"));

    mockApi().addDirectionalPoolRecord(record, group, "AAAAAAAAAAAAAAAA");
  }

  @Test
  public void updateDirectionalRecordAndGroup() throws Exception {
    server.enqueue(new MockResponse().setBody(updateDirectionalPoolRecordResponse));

    DirectionalRecord record = new DirectionalRecord();
    record.id = "A000000000000001";
    record.name = "www.denominator.io.";
    record.type = "CNAME";
    record.ttl = 300;
    record.rdata.add("www-000000001.eu-west-1.elb.amazonaws.com.");

    DirectionalGroup group = new DirectionalGroup();
    group.name = "Europe";
    group.regionToTerritories.put("Europe", asList("Aland Islands"));

    mockApi().updateDirectionalPoolRecord(record, group);

    server.assertSoapBody(updateDirectionalPoolRecordRegions);
  }

  @Test
  public void updateDirectionalRecordAndGroupWhenExistsWithSameAttributes() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage(
        "2111: Resource Record of type CNAME with these attributes already exists in the system.");

    server.enqueueError(RESOURCE_RECORD_ALREADY_EXISTS,
                        "Resource Record of type CNAME with these attributes already exists in the system.");

    DirectionalRecord record = new DirectionalRecord();
    record.id = "ABCDEF";
    record.name = "mail.denominator.io.";
    record.type = "MX";
    record.ttl = 1800;
    record.rdata.add("10");
    record.rdata.add("maileast.denominator.io.");

    DirectionalGroup group = new DirectionalGroup();
    group.name = "Mexas";
    group.regionToTerritories.put("United States (US)", asList("Maryland", "Texas"));

    mockApi().updateDirectionalPoolRecord(record, group);
  }

  @Test
  public void getDirectionalGroup() throws Exception {
    server.enqueue(new MockResponse().setBody(getDirectionalDNSGroupDetailsResponseEurope));

    DirectionalGroup
        group =
        mockApi().getDirectionalDNSGroupDetails("AAAAAAAAAAAAAAAA");
    assertThat(group.name).isEqualTo("Europe");
    assertThat(group.regionToTerritories)
        .containsEntry("Europe", asList("Aland Islands", "Albania", "Andorra", "Armenia", "Austria",
                                        "Azerbaijan", "Belarus", "Belgium", "Bosnia-Herzegovina",
                                        "Bulgaria", "Croatia", "Czech Republic", "Denmark",
                                        "Estonia",
                                        "Faroe Islands", "Finland", "France", "Georgia", "Germany",
                                        "Gibraltar", "Greece", "Guernsey", "Hungary", "Iceland",
                                        "Ireland", "Isle of Man", "Italy", "Jersey", "Latvia",
                                        "Liechtenstein", "Lithuania", "Luxembourg",
                                        "Macedonia, the former Yugoslav Republic of", "Malta",
                                        "Moldova, Republic of", "Monaco", "Montenegro",
                                        "Netherlands",
                                        "Norway", "Poland", "Portugal", "Romania", "San Marino",
                                        "Serbia", "Slovakia", "Slovenia", "Spain",
                                        "Svalbard and Jan Mayen", "Sweden", "Switzerland",
                                        "Ukraine",
                                        "Undefined Europe",
                                        "United Kingdom - England, Northern Ireland, Scotland, Wales",
                                        "Vatican City"));

    server.assertSoapBody(getDirectionalDNSGroupDetails);
  }

  @Test
  public void getRegionsByIdAndName() throws Exception {
    server.enqueue(new MockResponse().setBody(getAvailableRegionsResponse));

    Map<String, Collection<String>> group = mockApi().getAvailableRegions();
    assertThat(group)
        .containsEntry("Anonymous Proxy (A1)", asList("Anonymous Proxy"))
        .containsEntry("Antarctica",
                       asList("Antarctica", "Bouvet Island", "French Southern Territories"));

    server.assertSoapBody(getAvailableRegions);
  }

  UltraDNS mockApi() {
    UltraDNSProvider.FeignModule module = new UltraDNSProvider.FeignModule();
    Feign feign = module.feign(module.logger(), module.logLevel());
    return feign.newInstance(new UltraDNSTarget(new UltraDNSProvider() {
      @Override
      public String url() {
        return server.url();
      }
    }, new javax.inject.Provider<Credentials>() {

      @Override
      public Credentials get() {
        return server.credentials();
      }

    }));
  }

  static String getNeustarNetworkStatus = "<v01:getNeustarNetworkStatus/>";
  static String getNeustarNetworkStatusResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:getNeustarNetworkStatusResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <NeustarNetworkStatus xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Good</NeustarNetworkStatus>\n"
      + "    </ns1:getNeustarNetworkStatusResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String getNeustarNetworkStatusFailedResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:getNeustarNetworkStatusResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <NeustarNetworkStatus xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Failed</NeustarNetworkStatus>\n"
      + "    </ns1:getNeustarNetworkStatusResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String invalidUser =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "    <soap:Body>\n"
      + "            <soap:Fault>\n"
      + "                    <faultcode>soap:Client</faultcode>\n"
      + "                    <faultstring>Invalid User</faultstring>\n"
      + "            </soap:Fault>\n"
      + "    </soap:Body>\n"
      + "</soap:Envelope>";
  static String getAccountsListOfUser = "<v01:getAccountsListOfUser/>";
  static String getAccountsListOfUserResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:getAccountsListOfUserResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <AccountsList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n"
      + "        <ns2:AccountDetailsData accountID=\"AAAAAAAAAAAAAAAA\" accountName=\"denominator\" />\n"
      + "      </AccountsList>\n"
      + "    </ns1:getAccountsListOfUserResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String
      getZonesOfAccount =
      "<v01:getZonesOfAccount><accountId>AAAAAAAAAAAAAAAA</accountId><zoneType>all</zoneType></v01:getZonesOfAccount>";
  static String getZonesOfAccountResponseHeader =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:getZonesOfAccountResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <ZoneList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";
  static String getZonesOfAccountResponseFooter = "      </ZoneList>\n"
                                                  + "    </ns1:getZonesOfAccountResponse>\n"
                                                  + "  </soap:Body>\n"
                                                  + "</soap:Envelope>";
  static String getZonesOfAccountResponsePresent = getZonesOfAccountResponseHeader
                                                   + "        <ns2:UltraZone zoneName=\"denominator.io.\" zoneType=\"1\" accountId=\"AAAAAAAAAAAAAAAA\" owner=\"EEEEEEEEEEEEEEE\" zoneId=\"0000000000000001\" dnssecStatus=\"UNSIGNED\"/>\n"
                                                   + getZonesOfAccountResponseFooter;
  static String getZonesOfAccountResponseAbsent =
      getZonesOfAccountResponseHeader + getZonesOfAccountResponseFooter;
  static String
      getResourceRecordsOfZone =
      "<v01:getResourceRecordsOfZone><zoneName>denominator.io.</zoneName><rrType>0</rrType></v01:getResourceRecordsOfZone>";
  static String getResourceRecordsOfZoneResponseHeader =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:getResourceRecordsOfZoneResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <ResourceRecordList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";
  static String getResourceRecordsOfZoneResponseFooter = "      </ResourceRecordList>\n"
                                                         + "    </ns1:getResourceRecordsOfZoneResponse>\n"
                                                         + "  </soap:Body>\n"
                                                         + "</soap:Envelope>";
  static String getResourceRecordsOfZoneResponseAbsent = getResourceRecordsOfZoneResponseHeader
                                                         + getResourceRecordsOfZoneResponseFooter;
  static String
      getResourceRecordsOfDNameByType =
      "<v01:getResourceRecordsOfDNameByType><zoneName>denominator.io.</zoneName><hostName>denominator.io.</hostName><rrType>6</rrType></v01:getResourceRecordsOfDNameByType>";
  static String getResourceRecordsOfDNameByTypeResponseHeader =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:getResourceRecordsOfDNameByTypeResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <ResourceRecordList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";
  static String getResourceRecordsOfDNameByTypeResponseFooter = "      </ResourceRecordList>\n"
                                                                + "    </ns1:getResourceRecordsOfDNameByTypeResponse>\n"
                                                                + "  </soap:Body>\n"
                                                                + "</soap:Envelope>";
  static String getResourceRecordsOfDNameByTypeResponsePresent =
      getResourceRecordsOfDNameByTypeResponseHeader
      + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"6\" DName=\"denominator.io.\" TTL=\"86400\" Guid=\"04053D8E57C7A22F\" ZoneId=\"03053D8E57C7A22A\" LName=\"denominator.io.\" Created=\"2013-02-22T08:22:48.000Z\" Modified=\"2013-02-22T08:22:49.000Z\">\n"
      + "          <ns2:InfoValues Info1Value=\"pdns75.ultradns.com.\" Info2Value=\"adrianc.netflix.com.\" Info3Value=\"2013022200\" Info4Value=\"86400\" Info5Value=\"86400\" Info6Value=\"86400\" Info7Value=\"86400\" />\n"
      + "        </ns2:ResourceRecord>\n"
      + getResourceRecordsOfDNameByTypeResponseFooter;
  static String getResourceRecordsOfDNameByTypeResponseAbsent =
      getResourceRecordsOfDNameByTypeResponseHeader
      + getResourceRecordsOfDNameByTypeResponseFooter;
  static String
      createResourceRecord =
      "<v01:createResourceRecord><transactionID /><resourceRecord ZoneName=\"denominator.io.\" Type=\"15\" DName=\"mail.denominator.io.\" TTL=\"1800\"><InfoValues Info1Value=\"10\" Info2Value=\"maileast.denominator.io.\" /></resourceRecord></v01:createResourceRecord>";
  static String createResourceRecordResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:createResourceRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <guid xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">04063D9D54C6A01F</guid>\n"
      + "    </ns1:createResourceRecordResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String updateResourceRecordResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:updateResourceRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"
      + "    </ns1:updateResourceRecordResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";

  static String deleteResourceRecord =
      "<v01:deleteResourceRecord><transactionID /><guid>ABCDEF</guid></v01:deleteResourceRecord>";
  static String deleteResourceRecordResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:deleteResourceRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"
      + "    </ns1:deleteResourceRecordResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String
      getLoadBalancingPoolsByZone =
      "<v01:getLoadBalancingPoolsByZone><zoneName>denominator.io.</zoneName><lbPoolType>RR</lbPoolType></v01:getLoadBalancingPoolsByZone>";
  static String getLoadBalancingPoolsByZoneResponseHeader =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:getLoadBalancingPoolsByZoneResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <LBPoolList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";
  static String getLoadBalancingPoolsByZoneResponseFooter = "      </LBPoolList>\n"
                                                            + "    </ns1:getLoadBalancingPoolsByZoneResponse>\n"
                                                            + "  </soap:Body>\n"
                                                            + "</soap:Envelope>";
  static String getLoadBalancingPoolsByZoneResponseAbsent =
      getLoadBalancingPoolsByZoneResponseHeader
      + getLoadBalancingPoolsByZoneResponseFooter;
  static String getRRPoolRecords =
      "<v01:getRRPoolRecords><lbPoolId>000000000000002</lbPoolId></v01:getRRPoolRecords>";
  static String getRRPoolRecordsResponseHeader =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:getRRPoolRecordsResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <ResourceRecordList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";
  static String getRRPoolRecordsResponseFooter = "      </ResourceRecordList>\n"
                                                 + "    </ns1:getRRPoolRecordsResponse>\n"
                                                 + "  </soap:Body>\n"
                                                 + "</soap:Envelope>";
  static String getRRPoolRecordsResponseAbsent =
      getRRPoolRecordsResponseHeader + getRRPoolRecordsResponseFooter;
  static String
      addRRLBPool =
      "<v01:addRRLBPool><transactionID /><zoneName>denominator.io.</zoneName><hostName>www.denominator.io.</hostName><description>1</description><poolRecordType>1</poolRecordType><rrGUID /></v01:addRRLBPool>";
  static String addRRLBPoolResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:addRRLBPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <RRPoolID>AAAAAAAAAAAAAAAA</RRPoolID>\n"
      + "    </ns1:addRRLBPoolResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String
      addRecordToRRPool =
      "<v01:addRecordToRRPool><transactionID /><roundRobinRecord lbPoolID=\"AAAAAAAAAAAAAAAA\" info1Value=\"www1.denominator.io.\" ZoneName=\"denominator.io.\" Type=\"1\" TTL=\"300\"/></v01:addRecordToRRPool>";
  static String addRecordToRRPoolResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:addRecordToRRPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <guid>012345</guid>\n"
      + "    </ns1:addRecordToRRPoolResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String
      deleteLBPool =
      "<v01:deleteLBPool><transactionID /><lbPoolID>AAAAAAAAAAAAAAAA</lbPoolID><DeleteAll>Yes</DeleteAll><retainRecordId /></v01:deleteLBPool>";
  static String deleteLBPoolResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:deleteLBPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"
      + "    </ns1:deleteLBPoolResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String
      getDirectionalPoolsOfZone =
      "<v01:getDirectionalPoolsOfZone><zoneName>denominator.io.</zoneName></v01:getDirectionalPoolsOfZone>";
  static String getDirectionalPoolsOfZoneResponseHeader =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:getDirectionalPoolsOfZoneResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <DirectionalPoolList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n";
  static String getDirectionalPoolsOfZoneResponseFooter = "      </DirectionalPoolList>\n"
                                                          + "    </ns1:getDirectionalPoolsOfZoneResponse>\n"
                                                          + "  </soap:Body>\n"
                                                          + "</soap:Envelope>";
  static String getDirectionalPoolsOfZoneResponsePresent = getDirectionalPoolsOfZoneResponseHeader

                                                           + "      <ns2:DirectionalPoolData dirpoolid=\"D000000000000001\" Zoneid=\"Z000000000000001\" Pooldname=\"www.denominator.io.\" DirPoolType=\"GEOLOCATION\" Description=\"test with ips and cnames\" />\n"
                                                           + "      <ns2:DirectionalPoolData dirpoolid=\"D000000000000002\" Zoneid=\"Z000000000000001\" Pooldname=\"www2.denominator.io.\" DirPoolType=\"SOURCEIP\" Description=\"should filter out as not geo\" />\n"
                                                           + getDirectionalPoolsOfZoneResponseFooter;
  static String getDirectionalPoolsOfZoneResponseAbsent = getDirectionalPoolsOfZoneResponseHeader
                                                          + getDirectionalPoolsOfZoneResponseFooter;
  static String
      getDirectionalDNSRecordsForHostTemplate =
      "<v01:getDirectionalDNSRecordsForHost><zoneName>%s</zoneName><hostName>%s</hostName><poolRecordType>%s</poolRecordType></v01:getDirectionalDNSRecordsForHost>";
  static String getDirectionalDNSRecordsForHost =
      format(getDirectionalDNSRecordsForHostTemplate, "denominator.io.",
             "www.denominator.io.", 0);
  static String getDirectionalDNSRecordsForHostIPV4 =
      format(getDirectionalDNSRecordsForHostTemplate,
             "denominator.io.", "www.denominator.io.", 1);
  static String getDirectionalDNSRecordsForHostIPV6 =
      format(getDirectionalDNSRecordsForHostTemplate,
             "denominator.io.", "www.denominator.io.", 28);
  static String getDirectionalDNSRecordsForHostResponseHeader =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:getDirectionalDNSRecordsForHostResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n";
  static String getDirectionalDNSRecordsForHostResponseFooter =
      "    </ns1:getDirectionalDNSRecordsForHostResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String getDirectionalDNSRecordsForHostResponsePresent =
      getDirectionalDNSRecordsForHostResponseHeader
      + "    <DirectionalDNSRecordDetailList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" ZoneName=\"denominator.io.\" DName=\"www.denominator.io.\">\n"
      + "      <ns2:DirectionalDNSRecordDetail GeolocationGroupName=\"Europe\" GeolocationGroupId=\"C000000000000001\" TerritoriesCount=\"54\" DirPoolRecordId=\"A000000000000001\">\n"
      + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"300\" noResponseRecord=\"false\">\n"
      + "          <ns2:InfoValues Info1Value=\"www-000000001.eu-west-1.elb.amazonaws.com.\" />\n"
      + "        </ns2:DirectionalDNSRecord>\n"
      + "      </ns2:DirectionalDNSRecordDetail>\n"
      + "      <ns2:DirectionalDNSRecordDetail GeolocationGroupName=\"US\" GeolocationGroupId=\"C000000000000002\" TerritoriesCount=\"3\" DirPoolRecordId=\"A000000000000002\">\n"
      + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"300\" noResponseRecord=\"false\">\n"
      + "          <ns2:InfoValues Info1Value=\"www-000000001.us-east-1.elb.amazonaws.com.\" />\n"
      + "        </ns2:DirectionalDNSRecord>\n"
      + "      </ns2:DirectionalDNSRecordDetail>\n"
      + "      <ns2:DirectionalDNSRecordDetail GeolocationGroupName=\"Everywhere Else\" GeolocationGroupId=\"C000000000000003\" TerritoriesCount=\"323\" DirPoolRecordId=\"A000000000000003\">\n"
      + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"60\" noResponseRecord=\"false\">\n"
      + "          <ns2:InfoValues Info1Value=\"www-000000002.us-east-1.elb.amazonaws.com.\" />\n"
      + "        </ns2:DirectionalDNSRecord>\n"
      + "      </ns2:DirectionalDNSRecordDetail>\n"
      + "    </DirectionalDNSRecordDetailList>\n"
      + getDirectionalDNSRecordsForHostResponseFooter;
  static String getDirectionalDNSRecordsForHostResponseAbsent =
      getDirectionalDNSRecordsForHostResponseHeader
      + getDirectionalDNSRecordsForHostResponseFooter;
  static String
      getDirectionalDNSRecordsForGroupTemplate =
      "<v01:getDirectionalDNSRecordsForGroup><groupName>%s</groupName><hostName>%s</hostName><zoneName>%s</zoneName><poolRecordType>%s</poolRecordType></v01:getDirectionalDNSRecordsForGroup>";
  static String getDirectionalDNSRecordsForGroup =
      format(getDirectionalDNSRecordsForGroupTemplate, "Europe",
             "www.denominator.io.", "denominator.io.", 1);
  static String getDirectionalDNSRecordsForGroupEuropeIPV6 =
      format(getDirectionalDNSRecordsForGroupTemplate,
             "Europe", "www.denominator.io.", "denominator.io.", 28);
  static String getDirectionalDNSRecordsForGroupResponseHeader =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:getDirectionalDNSRecordsForGroupResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n";
  static String getDirectionalDNSRecordsForGroupResponseFooter =
      "    </ns1:getDirectionalDNSRecordsForGroupResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String getDirectionalDNSRecordsForGroupResponsePresent =
      getDirectionalDNSRecordsForGroupResponseHeader
      + "    <DirectionalDNSRecordDetailList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" ZoneName=\"denominator.io.\" DName=\"www.denominator.io.\">\n"
      + "      <ns2:DirectionalDNSRecordDetail GeolocationGroupName=\"Europe\" GeolocationGroupId=\"C000000000000001\" TerritoriesCount=\"54\" DirPoolRecordId=\"A000000000000001\">\n"
      + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"300\" noResponseRecord=\"false\">\n"
      + "          <ns2:InfoValues Info1Value=\"www-000000001.eu-west-1.elb.amazonaws.com.\" />\n"
      + "        </ns2:DirectionalDNSRecord>\n"
      + "      </ns2:DirectionalDNSRecordDetail>\n"
      + "    </DirectionalDNSRecordDetailList>\n"
      + getDirectionalDNSRecordsForGroupResponseFooter;
  static String getDirectionalDNSRecordsForGroupResponseAbsent =
      getDirectionalDNSRecordsForGroupResponseHeader
      + getDirectionalDNSRecordsForGroupResponseFooter;
  static String
      addDirectionalPool =
      "<v01:addDirectionalPool><transactionID /><AddDirectionalPoolData dirPoolType=\"GEOLOCATION\" poolRecordType=\"A\" zoneName=\"denominator.io.\" hostName=\"www.denominator.io.\" description=\"A\"/></v01:addDirectionalPool>";
  static String addDirectionalPoolResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:addDirectionalPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <DirPoolID>AAAAAAAAAAAAAAAA</DirPoolID>\n"
      + "    </ns1:addDirectionalPoolResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String
      deleteDirectionalPool =
      "<v01:deleteDirectionalPool><transactionID /><dirPoolID>AAAAAAAAAAAAAAAA</dirPoolID><retainRecordID /></v01:deleteDirectionalPool>";
  static String deleteDirectionalPoolResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:deleteDirectionalPoolResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"
      + "    </ns1:deleteDirectionalPoolResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String
      deleteDirectionalPoolRecord =
      "<v01:deleteDirectionalPoolRecord><transactionID /><dirPoolRecordId>00000000000</dirPoolRecordId></v01:deleteDirectionalPoolRecord>";
  static String deleteDirectionalPoolRecordResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:deleteDirectionalPoolRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"
      + "    </ns1:deleteDirectionalPoolRecordResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String addDirectionalPoolRecord = "<v01:addDirectionalPoolRecord><transactionID />"
                                           + "<AddDirectionalRecordData directionalPoolId=\"AAAAAAAAAAAAAAAA\">"
                                           + "<DirectionalRecordConfiguration recordType=\"MX\" TTL=\"1800\" >"
                                           + "<InfoValues Info1Value=\"10\" Info2Value=\"maileast.denominator.io.\" />"
                                           + "</DirectionalRecordConfiguration>"
                                           + "<GeolocationGroupData><GroupData groupingType=\"DEFINE_NEW_GROUP\" />"
                                           + "<GeolocationGroupDetails groupName=\"Mexas\">"
                                           + "<GeolocationGroupDefinitionData regionName=\"United States (US)\" territoryNames=\"Maryland;Texas\" />"
                                           + "</GeolocationGroupDetails></GeolocationGroupData>"
                                           + "<forceOverlapTransfer>true</forceOverlapTransfer></AddDirectionalRecordData></v01:addDirectionalPoolRecord>";
  static String addDirectionalPoolRecordResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:addDirectionalPoolRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <DirectionalPoolRecordID xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">06063DC355058294</DirectionalPoolRecordID>\n"
      + "    </ns1:addDirectionalPoolRecordResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String
      updateDirectionalPoolRecordTemplate =
      "<v01:updateDirectionalPoolRecord><transactionID /><UpdateDirectionalRecordData directionalPoolRecordId=\"%s\"><DirectionalRecordConfiguration TTL=\"%s\" ><InfoValues Info1Value=\"%s\" /></DirectionalRecordConfiguration>%s</UpdateDirectionalRecordData></v01:updateDirectionalPoolRecord>";
  static String updateDirectionalPoolRecordRegions = format(
      updateDirectionalPoolRecordTemplate,
      "A000000000000001",
      300,
      "www-000000001.eu-west-1.elb.amazonaws.com.",
      "<GeolocationGroupDetails groupName=\"Europe\"><GeolocationGroupDefinitionData regionName=\"Europe\" territoryNames=\"Aland Islands\" /></GeolocationGroupDetails><forceOverlapTransfer>true</forceOverlapTransfer>");
  static String updateDirectionalPoolRecordResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "  <soap:Body>\n"
      + "    <ns1:updateDirectionalPoolRecordResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "      <result xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">Successful</result>\n"
      + "    </ns1:updateDirectionalPoolRecordResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String
      getDirectionalDNSGroupDetails =
      "<v01:getDirectionalDNSGroupDetails><GroupId>AAAAAAAAAAAAAAAA</GroupId></v01:getDirectionalDNSGroupDetails>";
  static String getDirectionalDNSGroupDetailsResponseEurope = "<?xml version=\"1.0\"?>\n"
                                                              + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                                              + " <soap:Body>\n"
                                                              + "         <ns1:getDirectionalDNSGroupDetailsResponseEurope xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
                                                              + "                 <DirectionalDNSGroupDetail xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" GroupName=\"Europe\">\n"
                                                              + "                         <ns2:DirectionalDNSRegion>\n"
                                                              + "                                 <ns2:RegionForNewGroups RegionName=\"Europe\"\n"
                                                              + "                                         TerritoryName=\"Aland Islands;Albania;Andorra;Armenia;Austria;Azerbaijan;Belarus;Belgium;Bosnia-Herzegovina;Bulgaria;Croatia;Czech Republic;Denmark;Estonia;Faroe Islands;Finland;France;Georgia;Germany;Gibraltar;Greece;Guernsey;Hungary;Iceland;Ireland;Isle of Man;Italy;Jersey;Latvia;Liechtenstein;Lithuania;Luxembourg;Macedonia, the former Yugoslav Republic of;Malta;Moldova, Republic of;Monaco;Montenegro;Netherlands;Norway;Poland;Portugal;Romania;San Marino;Serbia;Slovakia;Slovenia;Spain;Svalbard and Jan Mayen;Sweden;Switzerland;Ukraine;Undefined Europe;United Kingdom - England, Northern Ireland, Scotland, Wales;Vatican City\" />\n"
                                                              + "                         </ns2:DirectionalDNSRegion>\n"
                                                              + "                 </DirectionalDNSGroupDetail>\n"
                                                              + "         </ns1:getDirectionalDNSGroupDetailsResponseEurope>\n"
                                                              + " </soap:Body>\n"
                                                              + "</soap:Envelope>";
  static String getAvailableRegions = "<v01:getAvailableRegions />";
  static String getAvailableRegionsResponse =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "<soap:Body>\n"
      + "      <ns1:getAvailableRegionsResponse xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "          <DirectionalDNSAvailableRegionList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">\n"
      + "              <ns2:Region TerritoryName=\"Anonymous Proxy\" RegionName=\"Anonymous Proxy (A1)\" RegionID=\"14\" />\n"
      + "              <ns2:Region TerritoryName=\"Antarctica;Bouvet Island;French Southern Territories\" RegionName=\"Antarctica\" RegionID=\"3\" />\n"
      + "          </DirectionalDNSAvailableRegionList>\n"
      + "      </ns1:getAvailableRegionsResponse>\n"
      + "  </soap:Body>\n"
      + "</soap:Envelope>";
  static String getResourceRecordsOfZoneResponsePresent = getResourceRecordsOfZoneResponseHeader
                                                          + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"1\" DName=\"www.denominator.io.\" TTL=\"3600\" Guid=\"04023A2507B6468F\" ZoneId=\"0000000000000001\" LName=\"www.denominator.io.\" Created=\"2010-10-02T16:57:16.000Z\" Modified=\"2011-09-27T23:49:21.000Z\">\n"
                                                          + "          <ns2:InfoValues Info1Value=\"1.2.3.4\"/>\n"
                                                          + "        </ns2:ResourceRecord>\n"
                                                          + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"2\" DName=\"denominator.io.\" TTL=\"86400\" Guid=\"0B0338C2023F7969\" ZoneId=\"0000000000000001\" LName=\"denominator.io.\" Created=\"2009-10-12T12:02:23.000Z\" Modified=\"2009-10-12T12:02:23.000Z\">\n"
                                                          + "          <ns2:InfoValues Info1Value=\"pdns2.ultradns.net.\"/>\n"
                                                          + "        </ns2:ResourceRecord>\n"
                                                          + getResourceRecordsOfZoneResponseFooter;
  static String getLoadBalancingPoolsByZoneResponsePresent =
      getLoadBalancingPoolsByZoneResponseHeader
      + "      <ns2:LBPoolData zoneid=\"0000000000000001\">\n"
      + "        <ns2:PoolData description=\"uswest1\" PoolId=\"000000000000002\" PoolType=\"RD\" PoolRecordType=\"A\" PoolDName=\"app-uswest1.denominator.io.\" ResponseMethod=\"RR\" />\n"
      + "      </ns2:LBPoolData>\n"
      + "      <ns2:LBPoolData zoneid=\"0000000000000001\">\n"
      + "        <ns2:PoolData description=\"uswest2\" PoolId=\"000000000000003\" PoolType=\"RD\" PoolRecordType=\"A\" PoolDName=\"app-uswest2.denominator.io.\" ResponseMethod=\"RR\" />\n"
      + "      </ns2:LBPoolData>\n"
      + getLoadBalancingPoolsByZoneResponseFooter;
  static String getRRPoolRecordsResponsePresent = getRRPoolRecordsResponseHeader
                                                  + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"1\" DName=\"www.denominator.io.\" TTL=\"3600\" Guid=\"04023A2507B6468F\" ZoneId=\"0000000000000001\" LName=\"www.denominator.io.\" Created=\"2010-10-02T16:57:16.000Z\" Modified=\"2011-09-27T23:49:21.000Z\">\n"
                                                  + "          <ns2:InfoValues Info1Value=\"1.2.3.4\"/>\n"
                                                  + "        </ns2:ResourceRecord>\n"
                                                  + "        <ns2:ResourceRecord ZoneName=\"denominator.io.\" Type=\"2\" DName=\"denominator.io.\" TTL=\"86400\" Guid=\"0B0338C2023F7969\" ZoneId=\"0000000000000001\" LName=\"denominator.io.\" Created=\"2009-10-12T12:02:23.000Z\" Modified=\"2009-10-12T12:02:23.000Z\">\n"
                                                  + "          <ns2:InfoValues Info1Value=\"pdns2.ultradns.net.\"/>\n"
                                                  + "        </ns2:ResourceRecord>\n"
                                                  + getRRPoolRecordsResponseFooter;
  static String getDirectionalDNSRecordsForHostResponseFiltersOutSourceIP =
      getDirectionalDNSRecordsForHostResponseHeader
      + "    <DirectionalDNSRecordDetailList xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" ZoneName=\"denominator.io.\" DName=\"www.denominator.io.\">\n"
      + "      <ns2:DirectionalDNSRecordDetail SourceIPGroupName=\"172.16.1.0/24\" SourceIPGroupId=\"C000000000000001\" TerritoriesCount=\"54\" DirPoolRecordId=\"A000000000000001\">\n"
      + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"300\" noResponseRecord=\"false\">\n"
      + "          <ns2:InfoValues Info1Value=\"www-000000001.eu-west-1.elb.amazonaws.com.\" />\n"
      + "        </ns2:DirectionalDNSRecord>\n"
      + "      </ns2:DirectionalDNSRecordDetail>\n"
      + "      <ns2:DirectionalDNSRecordDetail GeolocationGroupName=\"US\" GeolocationGroupId=\"C000000000000002\" TerritoriesCount=\"3\" DirPoolRecordId=\"A000000000000002\">\n"
      + "        <ns2:DirectionalDNSRecord recordType=\"CNAME\" TTL=\"300\" noResponseRecord=\"false\">\n"
      + "          <ns2:InfoValues Info1Value=\"www-000000001.us-east-1.elb.amazonaws.com.\" />\n"
      + "        </ns2:DirectionalDNSRecord>\n"
      + "      </ns2:DirectionalDNSRecordDetail>\n"
      + "    </DirectionalDNSRecordDetailList>\n"
      + getDirectionalDNSRecordsForHostResponseFooter;
}
