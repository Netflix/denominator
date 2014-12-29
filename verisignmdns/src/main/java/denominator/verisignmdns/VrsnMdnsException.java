/**
 * 
 */
package denominator.verisignmdns;

import feign.FeignException;

/**
 * @author smahurpawar
 *
 */
public class VrsnMdnsException extends FeignException {
    private static final long serialVersionUID = 1L;
	
	private final int code;

    VrsnMdnsException(String message, int code) {
        super(message);
        this.code = code;
    }

    /**
     * The error code. ex {@code 1801}
     */
    public int code() {
        return code;
    }
    
}
