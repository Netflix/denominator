package denominator.model;

import static denominator.model.ResourceRecordSets.a;
import static denominator.model.ResourceRecordSets.cname;
import static denominator.model.ResourceRecordSets.ns;
import static org.testng.Assert.assertEquals;

import java.util.Map;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.NSData;

@Test
public class ResourceRecordSetsTest {

    @DataProvider(name = "a")
    public Object[][] createData() {
        Object[][] data = new Object[16][2];
        data[0][0] = a("www.denominator.io.", "1.1.1.1");
        data[0][1] = ResourceRecordSet.<AData> builder()
                                      .name("www.denominator.io.")
                                      .type("A")
                                      .add(AData.create("1.1.1.1")).build();
        data[1][0] = a("www.denominator.io.", 3600, "1.1.1.1");
        data[1][1] = ResourceRecordSet.<AData> builder()
                                      .name("www.denominator.io.")
                                      .type("A")
                                      .ttl(3600)
                                      .add(AData.create("1.1.1.1")).build();
        data[2][0] = a("www.denominator.io.", ImmutableSet.of("1.1.1.1"));
        data[2][1] = data[0][1];
        data[3][0] = a("www.denominator.io.", 3600, ImmutableSet.of("1.1.1.1"));
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

    static Object[][] toArray(Map<?, ?> data) {
        Object[][] str = null;
        {
            Object[] keys = data.keySet().toArray();
            Object[] values = data.values().toArray();
            str = new String[keys.length][values.length];
            for (int i = 0; i < keys.length; i++) {
                str[0][i] = keys[i];
                str[1][i] = values[i];
            }
        }
        return str;
    }
}
