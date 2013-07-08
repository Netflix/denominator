package denominator.designate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.StringReader;

import org.testng.annotations.Test;

import denominator.designate.KeystoneV2.TokenIdAndPublicURL;

@Test
public class KeystoneV2AccessDecoderTest {

    @Test
    public void publicURLFound() throws Throwable {
        String nameThenType = ""//
                + "            \"name\": \"DNS\",\n" //
                + "            \"endpoints\": [{\n" //
                + "                \"tenantId\": \"1234\",\n" //
                + "                \"publicURL\": \"https:\\/\\/dns.api.rackspacecloud.com\\/v1.0\\/1234\"\n" //
                + "            }],\n" //
                + "            \"type\": \"hpext:dns\"\n";

        TokenIdAndPublicURL tokenIdAndPublicUrl = new KeystoneV2AccessDecoder("hpext:dns").decode(null,
                new StringReader(ACCESS_HEADER + nameThenType + SERVICE + ACCESS_FOOTER), null);

        assertEquals(tokenIdAndPublicUrl.tokenId, "1bcd122d87494f5ab39a185b9ec5ff73");
        assertEquals(tokenIdAndPublicUrl.publicURL, "https://dns.api.rackspacecloud.com/v1.0/1234");
    }

    @Test
    public void noEndpoints() throws Throwable {
        String noEndpoints = ""//
                + "            \"name\": \"DNS\",\n" //
                + "            \"type\": \"hpext:dns\"\n";

        TokenIdAndPublicURL tokenIdAndPublicUrl = new KeystoneV2AccessDecoder("hpext:dns").decode(null,
                new StringReader(ACCESS_HEADER + noEndpoints + SERVICE + ACCESS_FOOTER), null);

        assertEquals(tokenIdAndPublicUrl.tokenId, "1bcd122d87494f5ab39a185b9ec5ff73");
        assertNull(tokenIdAndPublicUrl.publicURL);
    }

    @Test
    public void serviceNotFound() throws Throwable {
        TokenIdAndPublicURL tokenIdAndPublicUrl = new KeystoneV2AccessDecoder("hpext:dns").decode(null,
                new StringReader(ACCESS_HEADER + SERVICE + ACCESS_FOOTER), null);

        assertEquals(tokenIdAndPublicUrl.tokenId, "1bcd122d87494f5ab39a185b9ec5ff73");
        assertNull(tokenIdAndPublicUrl.publicURL);
    }

    @Test
    public void noServices() throws Throwable {
        TokenIdAndPublicURL tokenIdAndPublicUrl = new KeystoneV2AccessDecoder("hpext:dns").decode(null,
                new StringReader(ACCESS_HEADER + ACCESS_FOOTER), null);

        assertEquals(tokenIdAndPublicUrl.tokenId, "1bcd122d87494f5ab39a185b9ec5ff73");
        assertNull(tokenIdAndPublicUrl.publicURL);
    }

    @Test
    public void noToken() throws Throwable {
        TokenIdAndPublicURL tokenIdAndPublicUrl = new KeystoneV2AccessDecoder("hpext:dns").decode(null,
                new StringReader("{\n" //
                        + "    \"access\": {\n" //
                        + "        \"serviceCatalog\": [{\n"//
                        + ACCESS_FOOTER), null);

        assertNull(tokenIdAndPublicUrl);
    }

    static final String TOKEN = ""//
            + "        \"token\": {\n" //
            + "            \"id\": \"1bcd122d87494f5ab39a185b9ec5ff73\",\n" //
            + "            \"expires\": \"2013-07-01T10:13:55.109-05:00\",\n" //
            + "            \"tenant\": {\n" //
            + "                \"id\": \"1234\",\n" //
            + "                \"name\": \"1234\"\n" //
            + "            }\n" //
            + "        },\n";
    static final String ACCESS_HEADER = "{\n" //
            + "    \"access\": {\n" //
            + TOKEN //
            + "        \"serviceCatalog\": [{\n";
    static final String SERVICE = ""//
            + "        }, {\n" //
            + "            \"name\": \"cloudMonitoring\",\n" //
            + "            \"endpoints\": [{\n" //
            + "                \"tenantId\": \"1234\",\n" //
            + "                \"publicURL\": \"https:\\/\\/monitoring.api.rackspacecloud.com\\/v1.0\\/1234\"\n" //
            + "            }],\n" //
            + "            \"type\": \"rax:monitor\"\n";
    static final String ACCESS_FOOTER = ""//
            + "        }]\n"//
            + "    }\n" //
            + "}";

}
