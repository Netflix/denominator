package denominator.model;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;


import denominator.model.rdata.AData;

@Test
public class ResourceRecordSetTest {

    public void canBuildARecordSetInLongForm() {
        ResourceRecordSet<AData> record = ResourceRecordSet.<AData> builder()
                                                           .name("www.denominator.io.")
                                                           .type("A")
                                                           .ttl(3600)
                                                           .add(AData.create("192.0.2.1")).build();

        assertEquals(record.name(), "www.denominator.io.");
        assertEquals(record.type(), "A");
        assertEquals(record.ttl(), Integer.valueOf(3600));
        assertEquals(record.rdata().get(0), AData.create("192.0.2.1"));
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "rdata")
    public void testNullRdataNPE() {
        ResourceRecordSet.<AData> builder().add(null);
    }
    
    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Invalid ttl.*")
    public void testInvalidTTL() {
        ResourceRecordSet.<AData> builder()
            .name("www.denominator.io.")
            .type("A")
            .ttl(0xFFFFFFFF)
            .add(AData.create("192.0.2.1")).build();
    }
}
