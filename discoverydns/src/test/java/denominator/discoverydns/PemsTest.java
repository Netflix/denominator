package denominator.discoverydns;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import denominator.common.Util;

import static org.assertj.core.api.Assertions.assertThat;

public class PemsTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void readCertificate() throws Exception {
    Certificate cert = Pems.readCertificate(readPemtoString("cert.pem"));
    assertThat(cert.getType()).isEqualTo("X.509");
    assertThat(cert.toString()).contains("EMAILADDRESS=dontemailme@stuff.com, "
                                         + "CN=doesnt.matter, "
                                         + "OU=Issue stuff department, "
                                         + "O=Ministry of Pems, "
                                         + "L=Orwellville, "
                                         + "ST=Oceania, "
                                         + "C=ZZ");
  }

  @Test
  public void readPrivateKeyRsaUnencrypted() throws Exception {
    PrivateKey key = Pems.readPrivateKey(readPemtoString("key.pem"));
    assertThat(key.getAlgorithm()).isEqualTo("RSA");
    assertThat(key.getFormat()).isEqualTo("PKCS#8");
  }

  @Test
  public void readPrivateKeyRsaEncrypted() throws Exception {
    thrown.expect(IOException.class);
    thrown.expectMessage("Encrypted content is not currently supported");

    Pems.readPrivateKey(readPemtoString("key_enc.pem"));
  }

  @Test
  public void readPrivateKeyRsaUnencryptedPkcs8() throws Exception {
    PrivateKey key = Pems.readPrivateKey(readPemtoString("key_pkcs8.pem"));
    assertThat(key.getAlgorithm()).isEqualTo("RSA");
    assertThat(key.getFormat()).isEqualTo("PKCS#8");
  }

  @Test
  public void readPrivateKeyRsaEncryptedPkcs8() throws Exception {
    thrown.expect(IOException.class);
    thrown.expectMessage("Encrypted content is not currently supported");

    Pems.readPrivateKey(readPemtoString("key_pkcs8_enc.pem"));
  }

  private String readPemtoString(String file) throws Exception {
    return new String(
        Util.slurp(new InputStreamReader(PemsTest.class.getResourceAsStream("/" + file))));
  }
}
