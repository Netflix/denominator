package denominator.model;

import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.profileContainsType;
import static denominator.model.ResourceRecordSets.ns;
import static denominator.model.ResourceRecordSets.ptr;
import static denominator.model.ResourceRecordSets.spf;
import static denominator.model.ResourceRecordSets.toProfile;
import static denominator.model.ResourceRecordSets.txt;
import static denominator.model.ResourceRecordSets.withoutProfile;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import denominator.model.profile.Geo;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.PTRData;
import denominator.model.rdata.SPFData;
import denominator.model.rdata.TXTData;

@Test
public class ResourceRecordSetsTest {

    ResourceRecordSet<AData> aRRS = ResourceRecordSet.<AData> builder()
                                                     .name("www.denominator.io.")
                                                     .type("A")
                                                     .ttl(3600)
                                                     .add(AData.create("192.0.2.1")).build();

    public void nameEqualToReturnsFalseOnNull() {
        assertFalse(ResourceRecordSets.nameEqualTo(aRRS.getName()).apply(null));
    }

    public void nameEqualToReturnsFalseOnDifferentName() {
        assertFalse(ResourceRecordSets.nameEqualTo("www.foo.com").apply(aRRS));
    }

    public void nameEqualToReturnsTrueOnSameName() {
        assertTrue(ResourceRecordSets.nameEqualTo(aRRS.getName()).apply(aRRS));
    }

    public void typeEqualToReturnsFalseOnNull() {
        assertFalse(ResourceRecordSets.typeEqualTo(aRRS.getType()).apply(null));
    }

    public void typeEqualToReturnsFalseOnDifferentType() {
        assertFalse(ResourceRecordSets.typeEqualTo("TXT").apply(aRRS));
    }

    public void typeEqualToReturnsTrueOnSameType() {
        assertTrue(ResourceRecordSets.typeEqualTo(aRRS.getType()).apply(aRRS));
    }

    public void containsRDataReturnsFalseOnNull() {
        assertFalse(ResourceRecordSets.containsRData(aRRS.get(0)).apply(null));
    }

    public void containsRDataReturnsFalseWhenRDataDifferent() {
        assertFalse(ResourceRecordSets.containsRData(AData.create("198.51.100.1")).apply(aRRS));
    }

    public void containsRDataReturnsTrueWhenRDataEqual() {
        assertTrue(ResourceRecordSets.containsRData(AData.create("192.0.2.1")).apply(aRRS));
    }

    public void containsRDataReturnsTrueWhenRDataEqualButDifferentType() {
        assertTrue(ResourceRecordSets.containsRData(ImmutableMap.of("address", "192.0.2.1")).apply(aRRS));
    }

    Geo geo = Geo.create("US-East", ImmutableMultimap.of("US", "US-VA"));

    ResourceRecordSet<AData> geoRRS = ResourceRecordSet.<AData> builder()
                                                       .name("www.denominator.io.")
                                                       .type("A")
                                                       .ttl(3600)
                                                       .add(AData.create("1.1.1.1"))
                                                       .addProfile(geo).build();

    public void withoutProfileReturnsFalseOnNull() {
        assertFalse(withoutProfile().apply(null));
    }

    public void withoutProfileReturnsFalseWhenProfileNotEmpty() {
        assertFalse(withoutProfile().apply(geoRRS));
    }

    public void withoutProfileReturnsTrueWhenProfileEmpty() {
        assertTrue(withoutProfile().apply(aRRS));
    }

    public void profileContainsTypeReturnsFalseOnNull() {
        assertFalse(profileContainsType(Geo.class).apply(null));
    }

    public void profileContainsTypeReturnsFalseOnDifferentType() {
        assertFalse(profileContainsType(String.class).apply(geoRRS));
    }

    public void profileContainsTypeReturnsFalseOnAbsent() {
        assertFalse(profileContainsType(Geo.class).apply(aRRS));
    }

    public void profileContainsTypeReturnsTrueOnSameType() {
        assertTrue(profileContainsType(Geo.class).apply(geoRRS));
    }

    public void toProfileReturnsNullOnNull() {
        assertEquals(toProfile(Geo.class).apply(null), null);
    }

    static final class Foo extends ForwardingMap<String, Object> {

        @Override
        protected Map<String, Object> delegate() {
            return null;
        }

    }

    public void toProfileReturnsNullOnDifferentType() {
        assertEquals(toProfile(Foo.class).apply(geoRRS), null);
    }

    public void toProfileReturnsNullOnAbsent() {
        assertEquals(toProfile(Geo.class).apply(aRRS), null);
    }

    public void toProfileReturnsProfileOnSameType() {
        assertEquals(toProfile(Geo.class).apply(geoRRS), geo);
    }

    @DataProvider(name = "a")
    public Object[][] createData() {
        Object[][] data = new Object[28][2];
        data[0][0] = a("www.denominator.io.", "192.0.2.1");
        data[0][1] = ResourceRecordSet.<AData> builder()
                                      .name("www.denominator.io.")
                                      .type("A")
                                      .add(AData.create("192.0.2.1")).build();
        data[1][0] = a("www.denominator.io.", 3600, "192.0.2.1");
        data[1][1] = ResourceRecordSet.<AData> builder()
                                      .name("www.denominator.io.")
                                      .type("A")
                                      .ttl(3600)
                                      .add(AData.create("192.0.2.1")).build();
        data[2][0] = a("www.denominator.io.", ImmutableSet.of("192.0.2.1"));
        data[2][1] = data[0][1];
        data[3][0] = a("www.denominator.io.", 3600, ImmutableSet.of("192.0.2.1"));
        data[3][1] = data[1][1];
        data[4][0] = cname("www.denominator.io.", "1234:ab00:ff00::6b14:abcd");
        data[4][1] = ResourceRecordSet.<CNAMEData> builder()
                                      .name("www.denominator.io.")
                                      .type("CNAME")
                                      .add(CNAMEData.create("1234:ab00:ff00::6b14:abcd")).build();
        data[5][0] = cname("www.denominator.io.", 3600, "1234:ab00:ff00::6b14:abcd");
        data[5][1] = ResourceRecordSet.<CNAMEData> builder()
                                      .name("www.denominator.io.")
                                      .type("CNAME")
                                      .ttl(3600)
                                      .add(CNAMEData.create("1234:ab00:ff00::6b14:abcd")).build();
        data[6][0] = cname("www.denominator.io.", ImmutableSet.of("1234:ab00:ff00::6b14:abcd"));
        data[6][1] = data[4][1];
        data[7][0] = cname("www.denominator.io.", 3600, ImmutableSet.of("1234:ab00:ff00::6b14:abcd"));
        data[7][1] = data[5][1];
        data[8][0] = cname("www.denominator.io.", "www1.denominator.io.");
        data[8][1] = ResourceRecordSet.<CNAMEData> builder()
                                      .name("www.denominator.io.")
                                      .type("CNAME")
                                      .add(CNAMEData.create("www1.denominator.io.")).build();
        data[9][0] = cname("www.denominator.io.", 3600, "www1.denominator.io.");
        data[9][1] = ResourceRecordSet.<CNAMEData> builder()
                                      .name("www.denominator.io.")
                                      .type("CNAME")
                                      .ttl(3600)
                                      .add(CNAMEData.create("www1.denominator.io.")).build();
        data[10][0] = cname("www.denominator.io.", ImmutableSet.of("www1.denominator.io."));
        data[10][1] = data[8][1];
        data[11][0] = cname("www.denominator.io.", 3600, ImmutableSet.of("www1.denominator.io."));
        data[11][1] = data[9][1];
        data[12][0] = ns("denominator.io.", "ns.denominator.io.");
        data[12][1] = ResourceRecordSet.<NSData> builder()
                                      .name("denominator.io.")
                                      .type("NS")
                                      .add(NSData.create("ns.denominator.io.")).build();
        data[13][0] = ns("denominator.io.", 3600, "ns.denominator.io.");
        data[13][1] = ResourceRecordSet.<NSData> builder()
                                      .name("denominator.io.")
                                      .type("NS")
                                      .ttl(3600)
                                      .add(NSData.create("ns.denominator.io.")).build();
        data[14][0] = ns("denominator.io.", ImmutableSet.of("ns.denominator.io."));
        data[14][1] = data[12][1];
        data[15][0] = ns("denominator.io.", 3600, ImmutableSet.of("ns.denominator.io."));
        data[15][1] = data[13][1];
        data[16][0] = ptr("denominator.io.", "ptr.denominator.io.");
        data[16][1] = ResourceRecordSet.<PTRData> builder()
                                      .name("denominator.io.")
                                      .type("PTR")
                                      .add(PTRData.create("ptr.denominator.io.")).build();
        data[17][0] = ptr("denominator.io.", 3600, "ptr.denominator.io.");
        data[17][1] = ResourceRecordSet.<PTRData> builder()
                                      .name("denominator.io.")
                                      .type("PTR")
                                      .ttl(3600)
                                      .add(PTRData.create("ptr.denominator.io.")).build();
        data[18][0] = ptr("denominator.io.", ImmutableSet.of("ptr.denominator.io."));
        data[18][1] = data[16][1];
        data[19][0] = ptr("denominator.io.", 3600, ImmutableSet.of("ptr.denominator.io."));
        data[19][1] = data[17][1];
        data[20][0] = txt("denominator.io.", "\"made in sweden\"");
        data[20][1] = ResourceRecordSet.<TXTData> builder()
                                      .name("denominator.io.")
                                      .type("TXT")
                                      .add(TXTData.create("\"made in sweden\"")).build();
        data[21][0] = txt("denominator.io.", 3600, "\"made in sweden\"");
        data[21][1] = ResourceRecordSet.<TXTData> builder()
                                      .name("denominator.io.")
                                      .type("TXT")
                                      .ttl(3600)
                                      .add(TXTData.create("\"made in sweden\"")).build();
        data[22][0] = txt("denominator.io.", ImmutableSet.of("\"made in sweden\""));
        data[22][1] = data[20][1];
        data[23][0] = txt("denominator.io.", 3600, ImmutableSet.of("\"made in sweden\""));
        data[23][1] = data[21][1];
        data[24][0] = spf("denominator.io.", "\"v=spf1 a mx -all\"");
        data[24][1] = ResourceRecordSet.<SPFData> builder()
                                      .name("denominator.io.")
                                      .type("SPF")
                                      .add(SPFData.create("\"v=spf1 a mx -all\"")).build();
        data[25][0] = spf("denominator.io.", 3600, "\"v=spf1 a mx -all\"");
        data[25][1] = ResourceRecordSet.<SPFData> builder()
                                      .name("denominator.io.")
                                      .type("SPF")
                                      .ttl(3600)
                                      .add(SPFData.create("\"v=spf1 a mx -all\"")).build();
        data[26][0] = spf("denominator.io.", ImmutableSet.of("\"v=spf1 a mx -all\""));
        data[26][1] = data[24][1];
        data[27][0] = spf("denominator.io.", 3600, ImmutableSet.of("\"v=spf1 a mx -all\""));
        data[27][1] = data[25][1];        
        return data;
    }

    @Test(dataProvider = "a")
    public void shortFormEqualsLongForm(ResourceRecordSet<?> shortForm, ResourceRecordSet<?> longForm) {
        assertEquals(shortForm, longForm);
        assertEquals(shortForm.getName(), longForm.getName());
        assertEquals(shortForm.getType(), longForm.getType());
        assertEquals(shortForm.getTTL(), longForm.getTTL());
        assertEquals(ImmutableList.copyOf(shortForm), ImmutableList.copyOf(longForm));
    }
}
