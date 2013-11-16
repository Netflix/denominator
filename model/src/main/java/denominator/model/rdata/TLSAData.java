package denominator.model.rdata;

import static denominator.common.Preconditions.checkArgument;
import static denominator.common.Preconditions.checkNotNull;

import java.beans.ConstructorProperties;

import denominator.model.NumbersAreUnsignedIntsLinkedHashMap;

/**
 * Corresponds to the binary representation of the {@code TLSA} (Transport Layer Security Authentication) RData
 * 
 * <br>
 * <br>
 * <b>Example</b><br>
 * 
 * <pre>
 * TLSAData rdata = TLSAData.builder()
 *                          .usage(1)
 *                          .selector(1)
 *                          .matchingType(1)
 *                          .certificateAssociationData("B33F").build()
 * </pre>
 * 
 * See <a href="http://www.ietf.org/rfc/rfc6698.txt">RFC 6698</a>
 */
public class TLSAData extends NumbersAreUnsignedIntsLinkedHashMap {

    @ConstructorProperties({ "usage", "selector", "matchingType", "certificateAssociationData" })
    TLSAData(int usage, int selector, int matchingType, String certificateAssociationData) {
        checkArgument(usage <= 0xFF, "usage must be 0-255");
        checkArgument(selector <= 0xFF, "selector must be 0-255");
        checkArgument(matchingType <= 0xFF, "matchingType must be 0-255");
        checkNotNull(certificateAssociationData, "certificateAssociationData");
        put("usage", usage);
        put("selector", selector);
        put("matchingType", matchingType);
        put("certificateAssociationData", certificateAssociationData);
    }

    public int usage() {
        return Integer.class.cast(get("usage"));
    }

    public int selector() {
        return Integer.class.cast(get("selector"));
    }

    public int matchingType() {
        return Integer.class.cast(get("matchingType"));
    }

    public String certificateAssociationData() {
        return get("certificateAssociationData").toString();
    }

    public TLSAData.Builder toBuilder() {
        return builder().from(this);
    }

    public final static class Builder {
        private int usage = -1;
        private int selector = -1;
        private int matchingType = -1;
        private String certificateAssociationData;

        public TLSAData.Builder usage(int usage) {
            this.usage = usage;
            return this;
        }

        public TLSAData.Builder selector(int selector) {
            this.selector = selector;
            return this;
        }

        public TLSAData.Builder matchingType(int matchingType) {
            this.matchingType = matchingType;
            return this;
        }

        public TLSAData.Builder certificateAssociationData(String certificateAssociationData) {
            this.certificateAssociationData = certificateAssociationData;
            return this;
        }

        public TLSAData build() {
            return new TLSAData(usage, selector, matchingType, certificateAssociationData);
        }

        public TLSAData.Builder from(TLSAData in) {
        	return this.usage(in.usage())
        	           .selector(in.selector())
        	           .matchingType(in.matchingType())
        	           .certificateAssociationData(in.certificateAssociationData());
        }
    }

    public static TLSAData.Builder builder() {
        return new Builder();
    }

    private static final long serialVersionUID = 1L;
}
