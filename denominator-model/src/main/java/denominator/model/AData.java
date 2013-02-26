

package denominator.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.net.Inet4Address;
import java.net.InetAddress;

import com.google.common.net.InetAddresses;

/**
 * Class that holds the RDATA for an A record.
 */
public class AData extends RData implements TypedRData {

    protected AData(String value) {
        super(value);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public int type() { 
        return 1; 
    }
    
    
    public static class Builder extends RData.Builder<AData> {
        private String address;
        
        public Builder address(String address) {
            InetAddress addr = InetAddresses.forString(address);
            checkArgument(addr instanceof Inet4Address, "Must be an IPV4 Address: %s", address);
            this.address = address;
            return this;
        }
        
        @Override
        public AData build() {
            checkState(address != null, "Must set address for an A record");
            return new AData(address);
        }
    }
}
