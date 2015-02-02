package denominator.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.bind.MapTypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import denominator.model.profile.Geo;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CERTData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.DSData;
import denominator.model.rdata.LOCData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NAPTRData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.SSHFPData;
import denominator.model.rdata.TLSAData;
import denominator.model.rdata.TXTData;

import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.aaaa;
import static denominator.model.ResourceRecordSets.cert;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.ds;
import static denominator.model.ResourceRecordSets.loc;
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
import static denominator.model.ResourceRecordSets.tlsa;
import static denominator.model.ResourceRecordSets.txt;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Test
public class ResourceRecordSetsTest {

  private static final TypeToken<Map<String, Object>>
      MAP_STRING_OBJECT =
      new TypeToken<Map<String, Object>>() {
      };
  private static final TypeToken<ResourceRecordSet<Map<String, Object>>>
      RRSET_STRING_OBJECT =
      new TypeToken<ResourceRecordSet<Map<String, Object>>>() {
      };
  private static final TypeAdapter<Map<String, Object>>
      doubleToInt =
      new TypeAdapter<Map<String, Object>>() {
        TypeAdapter<Map<String, Object>>
            delegate =
            new MapTypeAdapterFactory(new ConstructorConstructor(
                Collections.<Type, InstanceCreator<?>>emptyMap()), false)
                .create(new Gson(), MAP_STRING_OBJECT);

        @Override
        public void write(JsonWriter out, Map<String, Object> value) throws IOException {
          delegate.write(out, value);
        }

        @Override
        public Map<String, Object> read(JsonReader in) throws IOException {
          Map<String, Object> map = delegate.read(in);
          for (Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Double) {
              entry.setValue(Double.class.cast(entry.getValue()).intValue());
            }
          }
          return map;
        }
      }.nullSafe();
  // deals with scenario where gson Object type treats all numbers as doubles.
  public final static Gson gson = new GsonBuilder()//
      .disableHtmlEscaping()//
      .registerTypeAdapter(Map.class, doubleToInt)//
      .registerTypeAdapter(MAP_STRING_OBJECT.getType(), doubleToInt).create();
  ResourceRecordSet<AData> aRRS = ResourceRecordSet.<AData>builder()
      .name("www.denominator.io.")
      .type("A")
      .ttl(3600)
      .add(AData.create("192.0.2.1")).build();
  Geo geo = Geo.create(ImmutableMultimap.of("US", "US-VA").asMap());
  ResourceRecordSet<AData> geoRRS = ResourceRecordSet.<AData>builder()
      .name("www.denominator.io.")
      .type("A")
      .qualifier("US-East")
      .ttl(3600)
      .add(AData.create("1.1.1.1"))
      .geo(geo).build();

  public void nameEqualToReturnsFalseOnNull() {
    assertFalse(nameEqualTo(aRRS.name()).apply(null));
  }

  public void nameEqualToReturnsFalseOnDifferentName() {
    assertFalse(nameEqualTo("www.foo.com").apply(aRRS));
  }

  public void nameEqualToReturnsTrueOnSameName() {
    assertTrue(nameEqualTo(aRRS.name()).apply(aRRS));
  }

  public void typeEqualToReturnsFalseOnNull() {
    assertFalse(nameAndTypeEqualTo(aRRS.name(), aRRS.type()).apply(null));
  }

  public void typeEqualToReturnsFalseOnDifferentType() {
    assertFalse(nameAndTypeEqualTo(aRRS.name(), "TXT").apply(aRRS));
  }

  public void typeEqualToReturnsTrueOnSameType() {
    assertTrue(nameAndTypeEqualTo(aRRS.name(), aRRS.type()).apply(aRRS));
  }

  public void containsRecordReturnsFalseOnNull() {
    assertFalse(ResourceRecordSets.containsRecord(aRRS.records().get(0)).apply(null));
  }

  public void containsRecordReturnsFalseWhenRDataDifferent() {
    assertFalse(ResourceRecordSets.containsRecord(AData.create("198.51.100.1")).apply(aRRS));
  }

  public void containsRecordReturnsTrueWhenRDataEqual() {
    assertTrue(ResourceRecordSets.containsRecord(AData.create("192.0.2.1")).apply(aRRS));
  }

  public void containsRecordReturnsTrueWhenRDataEqualButDifferentType() {
    assertTrue(
        ResourceRecordSets.containsRecord(ImmutableMap.of("address", "192.0.2.1")).apply(aRRS));
  }

  public void qualifierEqualToReturnsFalseOnNull() {
    assertFalse(
        nameTypeAndQualifierEqualTo(geoRRS.name(), geoRRS.type(), geoRRS.qualifier()).apply(null));
  }

  public void qualifierEqualToReturnsFalseOnDifferentQualifier() {
    assertFalse(nameTypeAndQualifierEqualTo(geoRRS.name(), geoRRS.type(), "TXT").apply(geoRRS));
  }

  public void qualifierEqualToReturnsFalseOnAbsentQualifier() {
    assertFalse(nameTypeAndQualifierEqualTo(geoRRS.name(), geoRRS.type(), "TXT").apply(aRRS));
  }

  public void qualifierEqualToReturnsTrueOnSameQualifier() {
    assertTrue(nameTypeAndQualifierEqualTo(geoRRS.name(), geoRRS.type(), geoRRS.qualifier())
                   .apply(geoRRS));
  }

  @DataProvider(name = "a")
  public Object[][] createData() {
    Object[][] data = new Object[37][3];
    data[0][0] = a("www.denominator.io.", "192.0.2.1");
    data[0][1] = ResourceRecordSet.<AData>builder()
        .name("www.denominator.io.")
        .type("A")
        .add(AData.create("192.0.2.1")).build();
    data[0][2] =
        "{\"name\":\"www.denominator.io.\",\"type\":\"A\",\"records\":[{\"address\":\"192.0.2.1\"}]}";
    data[1][0] = a("www.denominator.io.", 3600, "192.0.2.1");
    data[1][1] = ResourceRecordSet.<AData>builder()
        .name("www.denominator.io.")
        .type("A")
        .ttl(3600)
        .add(AData.create("192.0.2.1")).build();
    data[1][2] =
        "{\"name\":\"www.denominator.io.\",\"type\":\"A\",\"ttl\":3600,\"records\":[{\"address\":\"192.0.2.1\"}]}";
    data[2][0] = a("www.denominator.io.", ImmutableSet.of("192.0.2.1"));
    data[2][1] = data[0][1];
    data[2][2] = data[0][2];
    data[3][0] = a("www.denominator.io.", 3600, ImmutableSet.of("192.0.2.1"));
    data[3][1] = data[1][1];
    data[3][2] = data[1][2];
    data[4][0] = aaaa("www.denominator.io.", "1234:ab00:ff00::6b14:abcd");
    data[4][1] = ResourceRecordSet.<AAAAData>builder()
        .name("www.denominator.io.")
        .type("AAAA")
        .add(AAAAData.create("1234:ab00:ff00::6b14:abcd")).build();
    data[4][2] =
        "{\"name\":\"www.denominator.io.\",\"type\":\"AAAA\",\"records\":[{\"address\":\"1234:ab00:ff00::6b14:abcd\"}]}";
    data[5][0] = aaaa("www.denominator.io.", 3600, "1234:ab00:ff00::6b14:abcd");
    data[5][1] = ResourceRecordSet.<AAAAData>builder()
        .name("www.denominator.io.")
        .type("AAAA")
        .ttl(3600)
        .add(AAAAData.create("1234:ab00:ff00::6b14:abcd")).build();
    data[5][2] =
        "{\"name\":\"www.denominator.io.\",\"type\":\"AAAA\",\"ttl\":3600,\"records\":[{\"address\":\"1234:ab00:ff00::6b14:abcd\"}]}";
    data[6][0] = aaaa("www.denominator.io.", ImmutableSet.of("1234:ab00:ff00::6b14:abcd"));
    data[6][1] = data[4][1];
    data[6][2] = data[4][2];
    data[7][0] = aaaa("www.denominator.io.", 3600, ImmutableSet.of("1234:ab00:ff00::6b14:abcd"));
    data[7][1] = data[5][1];
    data[7][2] = data[5][2];
    data[8][0] = cname("www.denominator.io.", "www1.denominator.io.");
    data[8][1] = ResourceRecordSet.<CNAMEData>builder()
        .name("www.denominator.io.")
        .type("CNAME")
        .add(CNAMEData.create("www1.denominator.io.")).build();
    data[8][2] =
        "{\"name\":\"www.denominator.io.\",\"type\":\"CNAME\",\"records\":[{\"cname\":\"www1.denominator.io.\"}]}";
    data[9][0] = cname("www.denominator.io.", 3600, "www1.denominator.io.");
    data[9][1] = ResourceRecordSet.<CNAMEData>builder()
        .name("www.denominator.io.")
        .type("CNAME")
        .ttl(3600)
        .add(CNAMEData.create("www1.denominator.io.")).build();
    data[9][2] =
        "{\"name\":\"www.denominator.io.\",\"type\":\"CNAME\",\"ttl\":3600,\"records\":[{\"cname\":\"www1.denominator.io.\"}]}";
    data[10][0] = cname("www.denominator.io.", ImmutableSet.of("www1.denominator.io."));
    data[10][1] = data[8][1];
    data[10][2] = data[8][2];
    data[11][0] = cname("www.denominator.io.", 3600, ImmutableSet.of("www1.denominator.io."));
    data[11][1] = data[9][1];
    data[11][2] = data[9][2];
    data[12][0] = ns("denominator.io.", "ns.denominator.io.");
    data[12][1] = ResourceRecordSet.<NSData>builder()
        .name("denominator.io.")
        .type("NS")
        .add(NSData.create("ns.denominator.io.")).build();
    data[12][2] =
        "{\"name\":\"denominator.io.\",\"type\":\"NS\",\"records\":[{\"nsdname\":\"ns.denominator.io.\"}]}";
    data[13][0] = ns("denominator.io.", 3600, "ns.denominator.io.");
    data[13][1] = ResourceRecordSet.<NSData>builder()
        .name("denominator.io.")
        .type("NS")
        .ttl(3600)
        .add(NSData.create("ns.denominator.io.")).build();
    data[13][2] =
        "{\"name\":\"denominator.io.\",\"type\":\"NS\",\"ttl\":3600,\"records\":[{\"nsdname\":\"ns.denominator.io.\"}]}";
    data[14][0] = ns("denominator.io.", ImmutableSet.of("ns.denominator.io."));
    data[14][1] = data[12][1];
    data[14][2] = data[12][2];
    data[15][0] = ns("denominator.io.", 3600, ImmutableSet.of("ns.denominator.io."));
    data[15][1] = data[13][1];
    data[15][2] = data[13][2];
    data[16][0] = ptr("denominator.io.", "ptr.denominator.io.");
    data[16][1] = ResourceRecordSet.<PTRData>builder()
        .name("denominator.io.")
        .type("PTR")
        .add(PTRData.create("ptr.denominator.io.")).build();
    data[16][2] =
        "{\"name\":\"denominator.io.\",\"type\":\"PTR\",\"records\":[{\"ptrdname\":\"ptr.denominator.io.\"}]}";
    data[17][0] = ptr("denominator.io.", 3600, "ptr.denominator.io.");
    data[17][1] = ResourceRecordSet.<PTRData>builder()
        .name("denominator.io.")
        .type("PTR")
        .ttl(3600)
        .add(PTRData.create("ptr.denominator.io.")).build();
    data[17][2] =
        "{\"name\":\"denominator.io.\",\"type\":\"PTR\",\"ttl\":3600,\"records\":[{\"ptrdname\":\"ptr.denominator.io.\"}]}";
    data[18][0] = ptr("denominator.io.", ImmutableSet.of("ptr.denominator.io."));
    data[18][1] = data[16][1];
    data[18][2] = data[16][2];
    data[19][0] = ptr("denominator.io.", 3600, ImmutableSet.of("ptr.denominator.io."));
    data[19][1] = data[17][1];
    data[19][2] = data[17][2];
    data[20][0] = txt("denominator.io.", "\"made in sweden\"");
    data[20][1] = ResourceRecordSet.<TXTData>builder()
        .name("denominator.io.")
        .type("TXT")
        .add(TXTData.create("\"made in sweden\"")).build();
    data[20][2] =
        "{\"name\":\"denominator.io.\",\"type\":\"TXT\",\"records\":[{\"txtdata\":\"\\\"made in sweden\\\"\"}]}";
    data[21][0] = txt("denominator.io.", 3600, "\"made in sweden\"");
    data[21][1] = ResourceRecordSet.<TXTData>builder()
        .name("denominator.io.")
        .type("TXT")
        .ttl(3600)
        .add(TXTData.create("\"made in sweden\"")).build();
    data[21][2] =
        "{\"name\":\"denominator.io.\",\"type\":\"TXT\",\"ttl\":3600,\"records\":[{\"txtdata\":\"\\\"made in sweden\\\"\"}]}";
    data[22][0] = txt("denominator.io.", ImmutableSet.of("\"made in sweden\""));
    data[22][1] = data[20][1];
    data[22][2] = data[20][2];
    data[23][0] = txt("denominator.io.", 3600, ImmutableSet.of("\"made in sweden\""));
    data[23][1] = data[21][1];
    data[23][2] = data[21][2];
    data[24][0] = spf("denominator.io.", "\"v=spf1 a mx -all\"");
    data[24][1] = ResourceRecordSet.<SPFData>builder()
        .name("denominator.io.")
        .type("SPF")
        .add(SPFData.create("\"v=spf1 a mx -all\"")).build();
    data[24][2] =
        "{\"name\":\"denominator.io.\",\"type\":\"SPF\",\"records\":[{\"txtdata\":\"\\\"v=spf1 a mx -all\\\"\"}]}";
    data[25][0] = spf("denominator.io.", 3600, "\"v=spf1 a mx -all\"");
    data[25][1] = ResourceRecordSet.<SPFData>builder()
        .name("denominator.io.")
        .type("SPF")
        .ttl(3600)
        .add(SPFData.create("\"v=spf1 a mx -all\"")).build();
    data[25][2] =
        "{\"name\":\"denominator.io.\",\"type\":\"SPF\",\"ttl\":3600,\"records\":[{\"txtdata\":\"\\\"v=spf1 a mx -all\\\"\"}]}";
    data[26][0] = spf("denominator.io.", ImmutableSet.of("\"v=spf1 a mx -all\""));
    data[26][1] = data[24][1];
    data[26][2] = data[24][2];
    data[27][0] = spf("denominator.io.", 3600, ImmutableSet.of("\"v=spf1 a mx -all\""));
    data[27][1] = data[25][1];
    data[27][2] = data[25][2];
    data[28][0] = mx("denominator.io.", 3600, ImmutableSet.of("1 mx1.denominator.io."));
    data[28][1] = ResourceRecordSet.<MXData>builder()
        .name("denominator.io.")
        .type("MX")
        .ttl(3600)
        .add(MXData.create(1, "mx1.denominator.io.")).build();
    data[28][2] =
        "{\"name\":\"denominator.io.\",\"type\":\"MX\",\"ttl\":3600,\"records\":[{\"preference\":1,\"exchange\":\"mx1.denominator.io.\"}]}";
    data[29][0] = srv("denominator.io.", 3600, ImmutableSet.of("0 1 80 srv.denominator.io."));
    data[29][1] = ResourceRecordSet.<SRVData>builder()
        .name("denominator.io.")
        .type("SRV")
        .ttl(3600)
        .add(SRVData.builder().priority(0).weight(1).port(80).target("srv.denominator.io.").build())
        .build();
    data[29][2] =
        "{\"name\":\"denominator.io.\",\"type\":\"SRV\",\"ttl\":3600,\"records\":[{\"priority\":0,\"weight\":1,\"port\":80,\"target\":\"srv.denominator.io.\"}]}";
    data[30][0] = ds("ds.denominator.io.", 3600, ImmutableSet.of("12345 1 1 B33F"));
    data[30][1] = ResourceRecordSet.<DSData>builder()
        .name("ds.denominator.io.")
        .type("DS")
        .ttl(3600)
        .add(DSData.builder().keyTag(12345).algorithmId(1).digestId(1).digest("B33F").build())
        .build();
    data[30][2] =
        "{\"name\":\"ds.denominator.io.\",\"type\":\"DS\",\"ttl\":3600,\"records\":[{\"keyTag\":12345,\"algorithmId\":1,\"digestId\":1,\"digest\":\"B33F\"}]}";
    data[31][0] = cert("www.denominator.io.", 3600, ImmutableSet.of("12345 1 1 B33F"));
    data[31][1] = ResourceRecordSet.<CERTData>builder()
        .name("www.denominator.io.")
        .type("CERT")
        .ttl(3600)
        .add(CERTData.builder().certType(12345).keyTag(1).algorithm(1).cert("B33F").build())
        .build();
    data[31][2] =
        "{\"name\":\"www.denominator.io.\",\"type\":\"CERT\",\"ttl\":3600,\"records\":[{\"certType\":12345,\"keyTag\":1,\"algorithm\":1,\"cert\":\"B33F\"}]}";
    data[32][0] =
        naptr("phone.denominator.io.", 3600,
              ImmutableSet.of("1 1 U E2U+sip !^.*$!sip:customer-service@example.com! ."));
    data[32][1] = ResourceRecordSet.<NAPTRData>builder()
        .name("phone.denominator.io.")
        .type("NAPTR")
        .ttl(3600)
        .add(NAPTRData.builder().order(1).preference(1).flags("U").services("E2U+sip")
                 .regexp("!^.*$!sip:customer-service@example.com!").replacement(".").build())
        .build();
    data[32][2] =
        "{\"name\":\"phone.denominator.io.\",\"type\":\"NAPTR\",\"ttl\":3600,\"records\":[{\"order\":1,\"preference\":1,\"flags\":\"U\",\"services\":\"E2U+sip\",\"regexp\":\"!^.*$!sip:customer-service@example.com!\",\"replacement\":\".\"}]}";
    data[33][0] =
        sshfp("server1.denominator.io.", 3600,
              ImmutableSet.of("2 1 123456789abcdef67890123456789abcdef67890"));
    data[33][1] = ResourceRecordSet.<SSHFPData>builder()
        .name("server1.denominator.io.")
        .type("SSHFP")
        .ttl(3600)
        .add(SSHFPData.createDSA("123456789abcdef67890123456789abcdef67890")).build();
    data[33][2] =
        "{\"name\":\"server1.denominator.io.\",\"type\":\"SSHFP\",\"ttl\":3600,\"records\":[{\"algorithm\":2,\"fptype\":1,\"fingerprint\":\"123456789abcdef67890123456789abcdef67890\"}]}";
    data[34][0] =
        loc("server1.denominator.io.", 3600,
            ImmutableSet.of("37 48 48.892 S 144 57 57.502 E 26m 10m 100m 10m"));
    data[34][1] = ResourceRecordSet.<LOCData>builder()
        .name("server1.denominator.io.")
        .type("LOC")
        .ttl(3600)
        .add(LOCData.builder().latitude("37 48 48.892 S").longitude("144 57 57.502 E")
                 .altitude("26m").diameter("10m").hprecision("100m").vprecision("10m").build())
        .build();
    data[34][2] =
        "{\"name\":\"server1.denominator.io.\",\"type\":\"LOC\",\"ttl\":3600,\"records\":[{\"latitude\":\"37 48 48.892 S\",\"longitude\":\"144 57 57.502 E\",\"altitude\":\"26m\",\"diameter\":\"10m\",\"hprecision\":\"100m\",\"vprecision\":\"10m\"}]}";
    data[35][0] = tlsa("server1.denominator.io.", 3600, ImmutableSet.of("1 1 1 B33F"));
    data[35][1] = ResourceRecordSet.<TLSAData>builder()
        .name("server1.denominator.io.")
        .type("TLSA")
        .ttl(3600)
        .add(TLSAData.builder().usage(1).selector(1).matchingType(1)
                 .certificateAssociationData("B33F").build()).build();
    data[35][2] =
        "{\"name\":\"server1.denominator.io.\",\"type\":\"TLSA\",\"ttl\":3600,\"records\":[{\"usage\":1,\"selector\":1,\"matchingType\":1,\"certificateAssociationData\":\"B33F\"}]}";
    data[36][0] =
        loc("server1.denominator.io.", 3600, ImmutableSet.of("37 48 48.892 S 144 57 57.502 E"));
    data[36][1] = ResourceRecordSet.<LOCData>builder()
        .name("server1.denominator.io.")
        .type("LOC")
        .ttl(3600)
        .add(LOCData.builder().latitude("37 48 48.892 S").longitude("144 57 57.502 E").build())
        .build();
    data[36][2] =
        "{\"name\":\"server1.denominator.io.\",\"type\":\"LOC\",\"ttl\":3600,\"records\":[{\"latitude\":\"37 48 48.892 S\",\"longitude\":\"144 57 57.502 E\"}]}";
    return data;
  }

  @Test(dataProvider = "a")
  public void shortFormEqualsLongForm(ResourceRecordSet<?> shortForm, ResourceRecordSet<?> longForm,
                                      String json)
      throws IOException {
    assertEquals(shortForm, longForm);
    assertEquals(shortForm.name(), longForm.name());
    assertEquals(shortForm.type(), longForm.type());
    assertEquals(shortForm.ttl(), longForm.ttl());
    assertEquals(ImmutableList.copyOf(shortForm.records()),
                 ImmutableList.copyOf(longForm.records()));
    assertEquals(gson.toJson(shortForm), json);
    assertEquals(gson.fromJson(json, RRSET_STRING_OBJECT.getType()), shortForm);
    assertEquals(gson.fromJson(json, ResourceRecordSet.class), shortForm);
  }
}
