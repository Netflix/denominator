package denominator;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class ResourceTypeToValueTest {

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "ResourceTypes do not include RRRR; types: \\[A, NS, CNAME, SOA, PTR, MX, TXT, AAAA, SSHFP, SPF, SRV\\]")
    public void testNiceExceptionOnNotFound() {
        ResourceTypeToValue.lookup("RRRR");
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "resource type was null")
    public void testNiceExceptionOnNull() {
        ResourceTypeToValue.lookup((String)null);
    }

    @Test
    public void testBasicCase() {
        assertEquals(ResourceTypeToValue.lookup("AAAA"), Integer.valueOf(28));
    }
}
