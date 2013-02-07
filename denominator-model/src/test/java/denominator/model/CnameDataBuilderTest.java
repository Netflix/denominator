package denominator.model;

import static denominator.model.ResourceRecordSet.Builder.cname;
import static denominator.model.ResourceRecordSet.Builder.raw;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.testng.annotations.Test;

public class CnameDataBuilderTest {

    @Test
    public void typeSafeCnameRecordsTest() {
        ResourceRecordSet<CnameData> rrset = ResourceRecordSet.<CnameData> builder()
            .name("www")
            .ttl(3600)
            .add(cname().target("www2"))
            .build();
        
        assertEquals(1, rrset.getKlass());
        assertEquals("www", rrset.getName());
        assertEquals(3600, rrset.getTtl());
        assertEquals(5, rrset.getType());
        
        assertFalse(rrset.isEmpty());
        assertEquals(1, rrset.size());
        
        CnameData cname = rrset.iterator().next();
        assertEquals("www2", cname.toString());
        assertEquals(1, cname.getValues().size());
        assertEquals("www2", cname.getValues().get(0));
    }
    
    @Test
    public void typeFreeCnameRecordsTest() { 
        ResourceRecordSet<RData> rrset = ResourceRecordSet.<RData> builder()
            .name("abc")
            .ttl(600)
            .type(5)
            .add(raw("www2"))
            .build();
        
        assertEquals(1, rrset.getKlass());
        assertEquals("abc", rrset.getName());
        assertEquals(600, rrset.getTtl());
        assertEquals(5, rrset.getType());
        
        assertFalse(rrset.isEmpty());
        assertEquals(1, rrset.size());
        assertEquals("www2", rrset.iterator().next().toString());
    }
    
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void emptyNameTest() {
        ResourceRecordSet.<CnameData> builder()
                .name("www")
                .ttl(3600)
                .add(cname().target("")).build();
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void nullNameTest() {
        ResourceRecordSet.<CnameData> builder()
                .name("www")
                .ttl(3600)
                .add(cname().target(null)).build();
    }
}
