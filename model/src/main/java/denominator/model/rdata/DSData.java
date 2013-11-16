package denominator.model.rdata;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;

import denominator.model.NumbersAreUnsignedIntsLinkedHashMap;

/**
 * Corresponds to the binary representation of the {@code DS} (Delegation Signer) RData
 * 
 * <br>
 * <br>
 * <b>Example</b><br>
 * 
 * <pre>
 * DSData rdata = DSData.builder()
 *                      .keyTag(12345)
 *                      .algorithmId(1)
 *                      .digestId(1)
 *                      .digest("B33F").build()
 * </pre>
 * 
 * See <a href="http://www.ietf.org/rfc/rfc3658.txt">RFC 3658</a>
 */
public class DSData extends NumbersAreUnsignedIntsLinkedHashMap {

    @ConstructorProperties({ "keyTag", "algorithmId", "digestId", "digest" })
    DSData(int keyTag, int algorithmId, int digestId, String digest) {
        checkArgument(keyTag <= 0xFFFF, "keyTag must be 0-65535");
        checkArgument(algorithmId <= 0xFF, "algorithmId must be 0-255");
        checkArgument(digestId <= 0xFF, "digestId must be 0-255");
        checkNotNull(digest, "digest");
        put("keyTag", keyTag);
        put("algorithmId", algorithmId);
        put("digestId", digestId);
        put("digest", digest);
    }

    public int keyTag() {
        return Integer.class.cast(get("keyTag"));
    }

    public int algorithmId() {
        return Integer.class.cast(get("algorithmId"));
    }

    public int digestId() {
        return Integer.class.cast(get("digestId"));
    }

    public String digest() {
        return get("digest").toString();
    }

    public DSData.Builder toBuilder() {
        return builder().from(this);
    }

    public final static class Builder {
        private int keyTag = -1;
        private int algorithmId = -1;
        private int digestId = -1;
        private String digest;

        public DSData.Builder keyTag(int keyTag) {
            this.keyTag = keyTag;
            return this;
        }

        public DSData.Builder algorithmId(int algorithmId) {
            this.algorithmId = algorithmId;
            return this;
        }

        public DSData.Builder digestId(int digestId) {
            this.digestId = digestId;
            return this;
        }

        public DSData.Builder digest(String digest) {
            this.digest = digest;
            return this;
        }

        public DSData build() {
            return new DSData(keyTag, algorithmId, digestId, digest);
        }

        public DSData.Builder from(DSData in) {
            return this.keyTag(in.keyTag())
                       .algorithmId(in.algorithmId())
                       .digestId(in.digestId())
                       .digest(in.digest());
        }
    }

    public static DSData.Builder builder() {
        return new Builder();
    }

    private static final long serialVersionUID = 1L;
}
