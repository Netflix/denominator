package denominator.model.rdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedInteger;

/**
 * Corresponds to the binary representation of the {@code SSHFP} (SSH Fingerprint) RData
 * 
 * <h4>Example</h4>
 * 
 * <pre>
 * SSHFPData rdata = SSHFPData.builder()
 *                            .algorithm(2)
 *                            .type(1)
 *                            .fingerprint(&quot;123456789abcdef67890123456789abcdef67890&quot;).build();
 *  // or shortcut
 * SSHFPData rdata = SSHFPData.createDSA(&quot;123456789abcdef67890123456789abcdef67890&quot;);
 * </pre>
 * 
 * @see <a href="http://www.rfc-editor.org/rfc/rfc4255.txt">RFC 4255</a>
 */
public class SSHFPData extends ForwardingMap<String, Object> {

    /**
     * @param fingerprint {@code DSA} {@code SHA-1} fingerprint 
     */
    public static SSHFPData createDSA(String fingerprint) {
        return builder().algorithm(1).type(2).fingerprint(fingerprint).build();
    }

    /**
     * @param fingerprint {@code RSA} {@code SHA-1} fingerprint 
     */
    public static SSHFPData createRSA(String fingerprint) {
        return builder().algorithm(1).type(1).fingerprint(fingerprint).build();
    }

    private final UnsignedInteger algorithm;
    private final UnsignedInteger type;
    private final String fingerprint;

    @ConstructorProperties({ "algorithm", "type", "fingerprint" })
    private SSHFPData(UnsignedInteger algorithm, UnsignedInteger type, String fingerprint) {
        this.algorithm = checkNotNull(algorithm, "algorithm of %s", fingerprint);
        this.type = checkNotNull(type, "type of %s", fingerprint);
        this.fingerprint = checkNotNull(fingerprint, "fingerprint");
        this.delegate = ImmutableMap.<String, Object> builder()
                                    .put("algorithm", algorithm)
                                    .put("type", type)
                                    .put("fingerprint", fingerprint).build();
    }

    /**
     * This algorithm number octet describes the algorithm of the public key.
     * @return most often {@code 1} for {@code RSA} or {@code 2} for {@code DSA}. 
     */
    public UnsignedInteger getAlgorithm() {
        return algorithm;
    }

    /**
     * The fingerprint type octet describes the message-digest algorithm used to
     * calculate the fingerprint of the public key.
     * 
     * @return most often {@code 1} for {@code SHA-1}
     */
    public UnsignedInteger getType() {
        return type;
    }

    /**
     * The fingerprint calculated over the public key blob.
     */
    public String getFingerprint() {
        return fingerprint;
    }

    public final static class Builder {
        private UnsignedInteger algorithm;
        private UnsignedInteger type;
        private String fingerprint;

        /**
         * @see SSHFPData#getAlgorithm()
         */
        public SSHFPData.Builder algorithm(UnsignedInteger algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        /**
         * @see SSHFPData#getAlgorithm()
         */
        public SSHFPData.Builder algorithm(int algorithm) {
            return algorithm(UnsignedInteger.fromIntBits(algorithm));
        }

        /**
         * @see SSHFPData#getType()
         */
        public SSHFPData.Builder type(UnsignedInteger type) {
            this.type = type;
            return this;
        }

        /**
         * @see SSHFPData#getType()
         */
        public SSHFPData.Builder type(int type) {
            return type(UnsignedInteger.fromIntBits(type));
        }

        /**
         * @see SSHFPData#getFingerprint()
         */
        public SSHFPData.Builder fingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        public SSHFPData build() {
            return new SSHFPData(algorithm, type, fingerprint);
        }
    }

    public static SSHFPData.Builder builder() {
        return new Builder();
    }

    // transient to avoid serializing by default, for example in json
    private final transient ImmutableMap<String, Object> delegate;
    
    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
