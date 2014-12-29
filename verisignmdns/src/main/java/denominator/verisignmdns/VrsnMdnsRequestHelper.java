/**
 * 
 */
package denominator.verisignmdns;

import java.util.Map;

import denominator.model.ResourceRecordSet;

/**
 * @author smahurpawar
 *
 */
public class VrsnMdnsRequestHelper {
	private static final String NAPTR_KEY_ORDER = "order";
    private static final String NAPTR_KEY_PREFERNCE = "preference";
    private static final String NAPTR_KEY_FLAGS = "flags";
    private static final String NAPTR_KEY_SERVICE = "services";
    private static final String NAPTR_KEY_REGEXP = "regexp";
    private static final String NAPTR_KEY_REPLACEMENT = "replacement";
			

			
	/**
	 * This method parses ResourceRecordSet.record data to construct 
	 * rData string
	 * @param aRRSet ResourceRecordSet
	 * @return String
	 */
	public static String getNAPTRData(ResourceRecordSet aRRSet) {
		StringBuilder sb = new StringBuilder();
		if (aRRSet != null) {
			for (Object obj : aRRSet.records()) {
    			Map<String, ?> attributeMap = (Map<String, String>) obj;
				if (sb.length() > 0) {
        			sb.append(VrsnConstants.STRING_COMMA);
        		}
    			sb.append(attributeMap.get(NAPTR_KEY_ORDER).toString()).append(VrsnConstants.STRING_SPACE);
    			sb.append(attributeMap.get(NAPTR_KEY_PREFERNCE).toString()).append(VrsnConstants.STRING_SPACE);
    			String tempStr = attributeMap.get(NAPTR_KEY_FLAGS).toString();
    			sb.append(encloseInDoubleQuotes(tempStr)).append(VrsnConstants.STRING_SPACE);
    			tempStr = attributeMap.get(NAPTR_KEY_SERVICE).toString();
    			sb.append(encloseInDoubleQuotes(tempStr)).append(VrsnConstants.STRING_SPACE);
    			tempStr = attributeMap.get(NAPTR_KEY_REGEXP).toString();
    			sb.append(encloseInDoubleQuotes(tempStr)).append(VrsnConstants.STRING_SPACE);
    			sb.append(attributeMap.get(NAPTR_KEY_REPLACEMENT).toString());
			}	
		}
		return sb.toString();
	}
	
	/**
	 * Some of rData attribute required by MDNS to be enclosed in Double Quotes
	 * This method wraps string in double quotes, also removes wrapping single quote if present
	 * before applying double quotes.
	 * (single quote might be present in case of denominator-cli request)
	 * @param anInput String
	 * @return String
	 */
	private static String encloseInDoubleQuotes(String anInput) {
		StringBuilder sb = new StringBuilder();
		if (anInput != null) {
			String trimmed = anInput.trim();
			trimmed = takeOutOfSingleQuote(trimmed);
			if (trimmed.isEmpty()) {
				sb.append(VrsnConstants.STRING_DOUBLE_QUOTE).append(VrsnConstants.STRING_DOUBLE_QUOTE);
				return sb.toString();
			} else if (!trimmed.startsWith(VrsnConstants.STRING_DOUBLE_QUOTE)){
				sb.append(VrsnConstants.STRING_DOUBLE_QUOTE);
				sb.append(trimmed);
				sb.append(VrsnConstants.STRING_DOUBLE_QUOTE);
				return sb.toString();
			}
		}
		return anInput;
	}
	
	/**
	 * if string is begins and ends with single quote character
	 * these quotes will be removed in returned string.
	 * @param anInput
	 * @return String
	 */
	private static String takeOutOfSingleQuote(String anInput) {
		StringBuilder sb = new StringBuilder();
		if (anInput != null) {
			String trimmed = anInput.trim();
			if(trimmed.startsWith(VrsnConstants.STRING_SINGLE_QUOTE)
					&& trimmed.endsWith(VrsnConstants.STRING_SINGLE_QUOTE)) {
			sb.append(trimmed.substring(1, trimmed.lastIndexOf(VrsnConstants.STRING_SINGLE_QUOTE)));
		    } else {
		    	sb.append(anInput);
		    }
			return sb.toString();	
		}
		return anInput;
	}
}
