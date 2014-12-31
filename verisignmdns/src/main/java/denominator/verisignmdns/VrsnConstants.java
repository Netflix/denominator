/**
 * 
 */
package denominator.verisignmdns;

import java.util.HashMap;
import java.util.Map;

/**
 * @author smahurpawar
 *
 */
public class VrsnConstants {
	public static final String SERVICE_QNAME_NAMESPACE = "urn:com:verisign:dnsa:api:wsdl:1";
	public static final String SERVICE_QNAME_LOCAL = "DNSA";
	
	
	// String constants 
	public static final String STRING_EMPTY =  "";
	public static final String STRING_SPACE =  " ";
	public static final String STRING_DOUBLE_QUOTE =  "\"";
	public static final String STRING_SINGLE_QUOTE =  "\'";
	public static final String STRING_COMMA = ",";
	
	// RR Type String Names
	public static final String RR_TYPE_A = "A";
	public static final String RR_TYPE_AAAA = "AAAA";
	public static final String RR_TYPE_NS = "NS";
	public static final String RR_TYPE_CNAME = "CNAME";
	public static final String RR_TYPE_PTR = "PTR";
	public static final String RR_TYPE_SRV = "SRV";
	public static final String RR_TYPE_NAPTR = "NAPTR";
	public static final String RR_TYPE_TXT = "TXT";
	public static final String RR_TYPE_MX = "MX";
	public static final String RR_TYPE_DS = "DS";
	
	public static final String CREDENITAL_USERNAME_KEY = "username";
	public static final String CREDENTIAL_PASSWORD_KEY = "password";
	
	
	public static final String MDNS_ERROR_INVALID_INPUT = "ERROR_MISSING_INVALID_INPUT";
	public static final String MDNS_ERROR_RULE_VALIDATION = "ERROR_RULE_VALIDATION";
	public static final String MDNS_ERROR_OPERATION_FAILURE = "ERROR_OPERATION_FAILURE";
	public static final String MDNS_ERROR_INTERNAL_ERROR = "ERROR_INTERNAL_ERROR";
	
	public static final Map <String, Integer> MDNS_ERROR_TO_INT_CODE_MAP 
												= new HashMap<String, Integer>();
	static {
		MDNS_ERROR_TO_INT_CODE_MAP.put(MDNS_ERROR_INVALID_INPUT, new Integer(1));
		MDNS_ERROR_TO_INT_CODE_MAP.put(MDNS_ERROR_RULE_VALIDATION, new Integer(2));
		MDNS_ERROR_TO_INT_CODE_MAP.put(MDNS_ERROR_OPERATION_FAILURE, new Integer(3));
		MDNS_ERROR_TO_INT_CODE_MAP.put(MDNS_ERROR_INTERNAL_ERROR, new Integer(4));
	}
	
	
	
}
