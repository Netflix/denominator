package denominator.model;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.primitives.UnsignedInteger;

import denominator.model.rdata.AData;

@Test
public class ResourceRecordSetTest {

    public void canBuildARecordSetInLongForm() {
        ResourceRecordSet<AData> record = ResourceRecordSet.<AData> builder()
                                                           .name("www.denominator.io.")
                                                           .type("A")
                                                           .ttl(3600)
                                                           .add(AData.create("1.1.1.1")).build();

        assertEquals(record.getName(), "www.denominator.io.");
        assertEquals(record.getType(), "A");
        assertEquals(record.getTTL().get(), UnsignedInteger.fromIntBits(3600));
        assertEquals(record.get(0), AData.create("1.1.1.1"));
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "rdata")
    public void testNullRdataNPE() {
        ResourceRecordSet.<AData> builder().add(null);
    }
}
