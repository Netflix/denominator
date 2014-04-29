package denominator.discoverydns.crypto;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class PemsTest {
    @Test
    public void readCertificate() throws IOException {
        Certificate cert = Pems.readCertificate(readPemtoString("cert.pem"));
        assertNotNull(cert);
        assertEquals(cert.getType(), "X.509");
        assertTrue(cert.toString().contains("EMAILADDRESS=dontemailme@stuff.com, "
                                          + "CN=doesnt.matter, "
                                          + "OU=Issue stuff department, "
                                          + "O=Ministry of Pems, "
                                          + "L=Orwellville, "
                                          + "ST=Oceania, "
                                          + "C=ZZ"));
    }

    @Test
    public void readPrivateKeyRsaUnencrypted() throws IOException {
        PrivateKey key = Pems.readPrivateKey(readPemtoString("key.pem"));
        assertNotNull(key);
        assertEquals(key.getAlgorithm(), "RSA");
        assertEquals(key.getFormat(), "PKCS#8");
        assertTrue(key.toString().contains("13027056791347954860993705134784360"
                                         + "90604656096917973695562883321428489"
                                         + "98948407504753182462366967344028665"
                                         + "37344247877657226225006174465507621"
                                         + "39437947285863531387544470171256920"
                                         + "21960122274296762437642683683205703"
                                         + "96222695213071318030195835255388795"
                                         + "63345001588010191621586917319925054"
                                         + "12412889441923662748528114071"));
    }

    @Test(expectedExceptions = IOException.class,
          expectedExceptionsMessageRegExp = "Encrypted content is not currently supported")
    public void readPrivateKeyRsaEncrypted() throws IOException {
        Pems.readPrivateKey(readPemtoString("key_enc.pem"));
    }

    @Test
    public void readPrivateKeyRsaUnencryptedPkcs8() throws IOException {
        PrivateKey key = Pems.readPrivateKey(readPemtoString("key_pkcs8.pem"));
        assertNotNull(key);
        assertEquals(key.getAlgorithm(), "RSA");
        assertEquals(key.getFormat(), "PKCS#8");
        assertTrue(key.toString().contains("13027056791347954860993705134784360"
                                         + "90604656096917973695562883321428489"
                                         + "98948407504753182462366967344028665"
                                         + "37344247877657226225006174465507621"
                                         + "39437947285863531387544470171256920"
                                         + "21960122274296762437642683683205703"
                                         + "96222695213071318030195835255388795"
                                         + "63345001588010191621586917319925054"
                                         + "12412889441923662748528114071"));
    }

    @Test(expectedExceptions = IOException.class,
          expectedExceptionsMessageRegExp = "Encrypted content is not currently supported")
    public void readPrivateKeyRsaEncryptedPkcs8() throws IOException {
        Pems.readPrivateKey(readPemtoString("key_pkcs8_enc.pem"));
    }

    private String readPemtoString(String file) throws IOException {
        return Files.toString(new File("src/test/resources/" + file), Charsets.US_ASCII);
    }
}
