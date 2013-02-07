

package denominator.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;

/**
 * Class that holds the RDATA for an CNAME record.
 */
public class CnameData extends RData implements TypedRData {

    protected CnameData(String value) {
        super(value);
    }
    
    @Override
    public int type() {
        return 5;
    }

    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder extends RData.Builder<CnameData> {
        private String target;
        
        public Builder target(String target) {
            checkArgument(!Strings.isNullOrEmpty(target), "Target must specify a domain name.");
            this.target = target;
            return this;
        }

        @Override
        public CnameData build() {
            checkState(target != null, "Must set target for an CNAME record");
            return new CnameData(target);
        }
    }
}
