package denominator.model;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import denominator.ResourceTypeToValue;
import denominator.model.profile.Geo;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CERTData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NAPTRData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.SSHFPData;
import denominator.model.rdata.TXTData;

import static denominator.assertj.ModelAssertions.assertThat;
import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static denominator.model.ResourceRecordSets.cert;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.mx;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.nameTypeAndQualifierEqualTo;
import static denominator.model.ResourceRecordSets.naptr;
import static denominator.model.ResourceRecordSets.ns;
import static denominator.model.ResourceRecordSets.ptr;
import static denominator.model.ResourceRecordSets.spf;
import static denominator.model.ResourceRecordSets.srv;
import static denominator.model.ResourceRecordSets.sshfp;
import static denominator.model.ResourceRecordSets.txt;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class ResourceRecordSetsTest {

  ResourceRecordSet<AData> aRRS = ResourceRecordSet.<AData>builder()
      .name("www.denominator.io.")
      .type("A")
      .ttl(3600)
      .add(AData.create("192.0.2.1")).build();
  ResourceRecordSet<AData> geoRRS = ResourceRecordSet.<AData>builder()
      .name("www.denominator.io.")
      .type("A")
      .qualifier("US-East")
      .ttl(3600)
      .add(AData.create("1.1.1.1"))
      .geo(Geo.create(new LinkedHashMap<String, Collection<String>>() {
        {
          put("US", Arrays.asList("US-VA"));
        }
      })).build();

  @Test
  public void nameEqualToReturnsFalseOnNull() {
    assertFalse(nameEqualTo(aRRS.name()).apply(null));
  }

  @Test
  public void nameEqualToReturnsFalseOnDifferentName() {
    assertFalse(nameEqualTo("www.foo.com").apply(aRRS));
  }

  @Test
  public void nameEqualToReturnsTrueOnSameName() {
    assertTrue(nameEqualTo(aRRS.name()).apply(aRRS));
  }

  @Test
  public void typeEqualToReturnsFalseOnNull() {
    assertFalse(nameAndTypeEqualTo(aRRS.name(), aRRS.type()).apply(null));
  }

  @Test
  public void typeEqualToReturnsFalseOnDifferentType() {
    assertFalse(nameAndTypeEqualTo(aRRS.name(), "TXT").apply(aRRS));
  }

  @Test
  public void typeEqualToReturnsTrueOnSameType() {
    assertTrue(nameAndTypeEqualTo(aRRS.name(), aRRS.type()).apply(aRRS));
  }

  @Test
  public void containsRecordReturnsFalseOnNull() {
    assertFalse(ResourceRecordSets.containsRecord(aRRS.records().get(0)).apply(null));
  }

  @Test
  public void containsRecordReturnsFalseWhenRDataDifferent() {
    assertFalse(ResourceRecordSets.containsRecord(AData.create("198.51.100.1")).apply(aRRS));
  }

  @Test
  public void containsRecordReturnsTrueWhenRDataEqual() {
    assertTrue(ResourceRecordSets.containsRecord(AData.create("192.0.2.1")).apply(aRRS));
  }

  @Test
  public void containsRecordReturnsTrueWhenRDataEqualButDifferentType() {
    Map<String, String> record = new LinkedHashMap<String, String>();
    record.put("address", "192.0.2.1");
    assertTrue(ResourceRecordSets.containsRecord(record).apply(aRRS));
  }

  @Test
  public void qualifierEqualToReturnsFalseOnNull() {
    assertFalse(
        nameTypeAndQualifierEqualTo(geoRRS.name(), geoRRS.type(), geoRRS.qualifier()).apply(null));
  }

  @Test
  public void qualifierEqualToReturnsFalseOnDifferentQualifier() {
    assertFalse(nameTypeAndQualifierEqualTo(geoRRS.name(), geoRRS.type(), "TXT").apply(geoRRS));
  }

  @Test
  public void qualifierEqualToReturnsFalseOnAbsentQualifier() {
    assertFalse(nameTypeAndQualifierEqualTo(geoRRS.name(), geoRRS.type(), "TXT").apply(aRRS));
  }

  @Test
  public void qualifierEqualToReturnsTrueOnSameQualifier() {
    assertTrue(nameTypeAndQualifierEqualTo(geoRRS.name(), geoRRS.type(), geoRRS.qualifier())
                   .apply(geoRRS));
  }

  @RunWith(Parameterized.class)
  public static class ShortFormEqualsLongFormTest {

    private final ResourceRecordSet<?> shortForm;
    private final ResourceRecordSet<?> longForm;

    public ShortFormEqualsLongFormTest(ResourceRecordSet<?> shortForm,
                                       ResourceRecordSet<?> longForm) {
      this.shortForm = shortForm;
      this.longForm = longForm;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
      Object[][] data = new Object[33][2];
      data[0][0] = a("www.denominator.io.", "192.0.2.1");
      data[0][1] = ResourceRecordSet.<AData>builder()
          .name("www.denominator.io.")
          .type("A")
          .add(AData.create("192.0.2.1")).build();
      data[1][0] = a("www.denominator.io.", 3600, "192.0.2.1");
      data[1][1] = ResourceRecordSet.<AData>builder()
          .name("www.denominator.io.")
          .type("A")
          .ttl(3600)
          .add(AData.create("192.0.2.1")).build();
      data[2][0] = a("www.denominator.io.", Arrays.asList("192.0.2.1"));
      data[2][1] = data[0][1];
      data[3][0] = a("www.denominator.io.", 3600, Arrays.asList("192.0.2.1"));
      data[3][1] = data[1][1];
      data[4][0] = aaaa("www.denominator.io.", "1234:ab00:ff00::6b14:abcd");
      data[4][1] = ResourceRecordSet.<AAAAData>builder()
          .name("www.denominator.io.")
          .type("AAAA")
          .add(AAAAData.create("1234:ab00:ff00::6b14:abcd")).build();
      data[5][0] = aaaa("www.denominator.io.", 3600, "1234:ab00:ff00::6b14:abcd");
      data[5][1] = ResourceRecordSet.<AAAAData>builder()
          .name("www.denominator.io.")
          .type("AAAA")
          .ttl(3600)
          .add(AAAAData.create("1234:ab00:ff00::6b14:abcd")).build();
      data[6][0] = aaaa("www.denominator.io.", Arrays.asList("1234:ab00:ff00::6b14:abcd"));
      data[6][1] = data[4][1];
      data[7][0] = aaaa("www.denominator.io.", 3600, Arrays.asList("1234:ab00:ff00::6b14:abcd"));
      data[7][1] = data[5][1];
      data[8][0] = cname("www.denominator.io.", "www1.denominator.io.");
      data[8][1] = ResourceRecordSet.<CNAMEData>builder()
          .name("www.denominator.io.")
          .type("CNAME")
          .add(CNAMEData.create("www1.denominator.io.")).build();
      data[9][0] = cname("www.denominator.io.", 3600, "www1.denominator.io.");
      data[9][1] = ResourceRecordSet.<CNAMEData>builder()
          .name("www.denominator.io.")
          .type("CNAME")
          .ttl(3600)
          .add(CNAMEData.create("www1.denominator.io.")).build();
      data[10][0] = cname("www.denominator.io.", Arrays.asList("www1.denominator.io."));
      data[10][1] = data[8][1];
      data[11][0] = cname("www.denominator.io.", 3600, Arrays.asList("www1.denominator.io."));
      data[11][1] = data[9][1];
      data[12][0] = ns("denominator.io.", "ns.denominator.io.");
      data[12][1] = ResourceRecordSet.<NSData>builder()
          .name("denominator.io.")
          .type("NS")
          .add(NSData.create("ns.denominator.io.")).build();
      data[13][0] = ns("denominator.io.", 3600, "ns.denominator.io.");
      data[13][1] = ResourceRecordSet.<NSData>builder()
          .name("denominator.io.")
          .type("NS")
          .ttl(3600)
          .add(NSData.create("ns.denominator.io.")).build();
      data[14][0] = ns("denominator.io.", Arrays.asList("ns.denominator.io."));
      data[14][1] = data[12][1];
      data[15][0] = ns("denominator.io.", 3600, Arrays.asList("ns.denominator.io."));
      data[15][1] = data[13][1];
      data[16][0] = ptr("denominator.io.", "ptr.denominator.io.");
      data[16][1] = ResourceRecordSet.<PTRData>builder()
          .name("denominator.io.")
          .type("PTR")
          .add(PTRData.create("ptr.denominator.io.")).build();
      data[17][0] = ptr("denominator.io.", 3600, "ptr.denominator.io.");
      data[17][1] = ResourceRecordSet.<PTRData>builder()
          .name("denominator.io.")
          .type("PTR")
          .ttl(3600)
          .add(PTRData.create("ptr.denominator.io.")).build();
      data[18][0] = ptr("denominator.io.", Arrays.asList("ptr.denominator.io."));
      data[18][1] = data[16][1];
      data[19][0] = ptr("denominator.io.", 3600, Arrays.asList("ptr.denominator.io."));
      data[19][1] = data[17][1];
      data[20][0] = txt("denominator.io.", "\"made in sweden\"");
      data[20][1] = ResourceRecordSet.<TXTData>builder()
          .name("denominator.io.")
          .type("TXT")
          .add(TXTData.create("\"made in sweden\"")).build();
      data[21][0] = txt("denominator.io.", 3600, "\"made in sweden\"");
      data[21][1] = ResourceRecordSet.<TXTData>builder()
          .name("denominator.io.")
          .type("TXT")
          .ttl(3600)
          .add(TXTData.create("\"made in sweden\"")).build();
      data[22][0] = txt("denominator.io.", Arrays.asList("\"made in sweden\""));
      data[22][1] = data[20][1];
      data[23][0] = txt("denominator.io.", 3600, Arrays.asList("\"made in sweden\""));
      data[23][1] = data[21][1];
      data[24][0] = spf("denominator.io.", "\"v=spf1 a mx -all\"");
      data[24][1] = ResourceRecordSet.<SPFData>builder()
          .name("denominator.io.")
          .type("SPF")
          .add(SPFData.create("\"v=spf1 a mx -all\"")).build();
      data[25][0] = spf("denominator.io.", 3600, "\"v=spf1 a mx -all\"");
      data[25][1] = ResourceRecordSet.<SPFData>builder()
          .name("denominator.io.")
          .type("SPF")
          .ttl(3600)
          .add(SPFData.create("\"v=spf1 a mx -all\"")).build();
      data[26][0] = spf("denominator.io.", Arrays.asList("\"v=spf1 a mx -all\""));
      data[26][1] = data[24][1];
      data[27][0] = spf("denominator.io.", 3600, Arrays.asList("\"v=spf1 a mx -all\""));
      data[27][1] = data[25][1];
      data[28][0] = mx("denominator.io.", 3600, Arrays.asList("1 mx1.denominator.io."));
      data[28][1] = ResourceRecordSet.<MXData>builder()
          .name("denominator.io.")
          .type("MX")
          .ttl(3600)
          .add(MXData.create(1, "mx1.denominator.io.")).build();
      data[29][0] = srv("denominator.io.", 3600, Arrays.asList("0 1 80 srv.denominator.io."));
      data[29][1] = ResourceRecordSet.<SRVData>builder()
          .name("denominator.io.")
          .type("SRV")
          .ttl(3600)
          .add(SRVData.builder().priority(0).weight(1).port(80).target("srv.denominator.io.")
                   .build())
          .build();
      data[30][0] = cert("www.denominator.io.", 3600, Arrays.asList("12345 1 1 B33F"));
      data[30][1] = ResourceRecordSet.<CERTData>builder()
          .name("www.denominator.io.")
          .type("CERT")
          .ttl(3600)
          .add(CERTData.builder().format(12345).tag(1).algorithm(1).certificate("B33F").build())
          .build();
      data[31][0] =
          naptr("phone.denominator.io.", 3600,
                Arrays.asList("1 1 U E2U+sip !^.*$!sip:customer-service@example.com! ."));
      data[31][1] = ResourceRecordSet.<NAPTRData>builder()
          .name("phone.denominator.io.")
          .type("NAPTR")
          .ttl(3600)
          .add(NAPTRData.builder().order(1).preference(1).flags("U").services("E2U+sip")
                   .regexp("!^.*$!sip:customer-service@example.com!").replacement(".").build())
          .build();
      data[32][0] =
          sshfp("server1.denominator.io.", 3600,
                Arrays.asList("2 1 123456789abcdef67890123456789abcdef67890"));
      data[32][1] = ResourceRecordSet.<SSHFPData>builder()
          .name("server1.denominator.io.")
          .type("SSHFP")
          .ttl(3600)
          .add(SSHFPData.createDSA("123456789abcdef67890123456789abcdef67890")).build();
      return Arrays.asList(data);
    }

    @Test
    public void shortFormEqualsLongForm() throws IOException {
      assertThat(shortForm)
          .isEqualTo(longForm)
          .hasName(longForm.name())
          .hasType(longForm.type())
          .hasTtl(longForm.ttl())
          .containsExactlyRecords(longForm.records());
      ResourceTypeToValue.lookup(longForm.type());
    }
  }
}
