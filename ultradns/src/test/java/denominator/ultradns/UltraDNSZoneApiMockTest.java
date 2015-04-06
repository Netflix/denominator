package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;

import org.junit.Rule;
import org.junit.Test;

import denominator.ZoneApi;
import denominator.model.Zone;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.ultradns.UltraDNSException.INVALID_ZONE_NAME;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUser;
import static denominator.ultradns.UltraDNSTest.getAccountsListOfUserResponse;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfDNameByType;
import static denominator.ultradns.UltraDNSTest.getResourceRecordsOfDNameByTypeResponsePresent;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccount;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponseAbsent;
import static denominator.ultradns.UltraDNSTest.getZonesOfAccountResponsePresent;

public class UltraDNSZoneApiMockTest {

  @Rule
  public final MockUltraDNSServer server = new MockUltraDNSServer();

  @Test
  public void iteratorWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getZonesOfAccountResponsePresent));
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponsePresent));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).containsExactly(
        Zone.create("denominator.io.", "denominator.io.", 86400, "adrianc.netflix.com.")
    );

    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(getZonesOfAccount);
    server.assertSoapBody(getResourceRecordsOfDNameByType);
  }

  @Test
  public void iteratorWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse().setBody(getZonesOfAccountResponseAbsent));
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponsePresent));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterator()).isEmpty();

    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(getZonesOfAccount);
  }

  @Test
  public void iteratorByName() throws Exception {
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponsePresent));

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.")).containsExactly(
        Zone.create("denominator.io.", "denominator.io.", 86400, "adrianc.netflix.com.")
    );

    server.assertSoapBody(getResourceRecordsOfDNameByType);
  }

  @Test
  public void iteratorByNameWhenNotFound() throws Exception {
    server.enqueueError(INVALID_ZONE_NAME, "Invalid zone name.");

    ZoneApi api = server.connect().api().zones();

    assertThat(api.iterateByName("denominator.io.")).isEmpty();

    server.assertSoapBody(getResourceRecordsOfDNameByType);
  }

  @Test
  public void putWhenAbsent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueue(new MockResponse());
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponsePresent));
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create(null, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());

    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(
        "<v01:createPrimaryZone><transactionID/><accountId>AAAAAAAAAAAAAAAA</accountId><zoneName>denominator.io.</zoneName><forceImport>false</forceImport></v01:createPrimaryZone>");
    server.assertSoapBody(
        "<v01:getResourceRecordsOfDNameByType><zoneName>denominator.io.</zoneName><hostName>denominator.io.</hostName><rrType>6</rrType></v01:getResourceRecordsOfDNameByType>");
    server.assertSoapBody(
        "<v01:updateResourceRecord><transactionID /><resourceRecord Guid=\"04053D8E57C7A22F\" ZoneName=\"denominator.io.\" Type=\"6\" DName=\"denominator.io.\" TTL=\"3601\"><InfoValues Info1Value=\"pdns75.ultradns.com.\" Info2Value=\"nil@denominator.io\" Info3Value=\"2013022200\" Info4Value=\"86400\" Info5Value=\"86400\" Info6Value=\"86400\" Info7Value=\"3601\" /></resourceRecord></v01:updateResourceRecord>");
  }

  @Test
  public void putWhenPresent() throws Exception {
    server.enqueue(new MockResponse().setBody(getAccountsListOfUserResponse));
    server.enqueueError(1802, "Zone already exists in the system.");
    server.enqueue(new MockResponse().setBody(getResourceRecordsOfDNameByTypeResponsePresent));
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();

    Zone zone = Zone.create(null, "denominator.io.", 3601, "nil@denominator.io");
    assertThat(api.put(zone)).isEqualTo(zone.name());

    server.assertSoapBody(getAccountsListOfUser);
    server.assertSoapBody(
        "<v01:createPrimaryZone><transactionID/><accountId>AAAAAAAAAAAAAAAA</accountId><zoneName>denominator.io.</zoneName><forceImport>false</forceImport></v01:createPrimaryZone>");
    server.assertSoapBody(
        "<v01:getResourceRecordsOfDNameByType><zoneName>denominator.io.</zoneName><hostName>denominator.io.</hostName><rrType>6</rrType></v01:getResourceRecordsOfDNameByType>");
    server.assertSoapBody(
        "<v01:updateResourceRecord><transactionID /><resourceRecord Guid=\"04053D8E57C7A22F\" ZoneName=\"denominator.io.\" Type=\"6\" DName=\"denominator.io.\" TTL=\"3601\"><InfoValues Info1Value=\"pdns75.ultradns.com.\" Info2Value=\"nil@denominator.io\" Info3Value=\"2013022200\" Info4Value=\"86400\" Info5Value=\"86400\" Info6Value=\"86400\" Info7Value=\"3601\" /></resourceRecord></v01:updateResourceRecord>");
  }

  @Test
  public void deleteWhenPresent() throws Exception {
    server.enqueue(new MockResponse());

    ZoneApi api = server.connect().api().zones();
    api.delete("denominator.io.");

    server.assertSoapBody(
        "<v01:deleteZone><transactionID /><zoneName>denominator.io.</zoneName></v01:deleteZone>");
  }

  @Test
  public void deleteWhenAbsent() throws Exception {
    server.enqueueError(1801, "Zone does not exist in the system.");

    ZoneApi api = server.connect().api().zones();
    api.delete("denominator.io.");

    server.assertSoapBody(
        "<v01:deleteZone><transactionID /><zoneName>denominator.io.</zoneName></v01:deleteZone>");
  }
}
