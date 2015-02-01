package denominator.discoverydns.crypto;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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
    }

    @Test(expectedExceptions = IOException.class,
          expectedExceptionsMessageRegExp = "Encrypted content is not currently supported")
    public void readPrivateKeyRsaEncryptedPkcs8() throws IOException {
        Pems.readPrivateKey(readPemtoString("key_pkcs8_enc.pem"));
    }

    private String readPemtoString(String file) throws IOException {
        return new String(ByteStreams.toByteArray(PemsTest.class.getResourceAsStream("/" + file)), Charsets.US_ASCII);
    }
}
