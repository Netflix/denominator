package denominator.verisignmdns;

import java.util.Map;

import denominator.model.ResourceRecordSet;

class VrsnMdnsRequestHelper {

    /**
     * This method parses ResourceRecordSet.record data to construct rData
     * string
     */
    static String getNAPTRData(ResourceRecordSet rRSet) {
        StringBuilder sb = new StringBuilder();
        if (rRSet != null) {
            for (Object obj : rRSet.records()) {
                Map<String, ?> attributeMap = (Map<String, String>) obj;
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(attributeMap.get("order").toString()).append(" ");
                sb.append(attributeMap.get("preference").toString()).append(" ");
                String tempStr = attributeMap.get("flags").toString();
                sb.append(encloseInDoubleQuotes(tempStr)).append(" ");
                tempStr = attributeMap.get("services").toString();
                sb.append(encloseInDoubleQuotes(tempStr)).append(" ");
                tempStr = attributeMap.get("regexp").toString();
                sb.append(encloseInDoubleQuotes(tempStr)).append(" ");
                sb.append(attributeMap.get("replacement").toString());
            }
        }
        return sb.toString();
    }

    /**
     * Some of rData attribute required by MDNS to be enclosed in Double Quotes
     * This method wraps string in double quotes, also removes wrapping single
     * quote if present before applying double quotes. (single quote might be
     * present in case of denominator-cli request)
     */
    private static String encloseInDoubleQuotes(String input) {
        StringBuilder sb = new StringBuilder();
        if (input != null) {
            String trimmed = input.trim();
            trimmed = takeOutOfSingleQuote(trimmed);
            if (trimmed.isEmpty()) {
                sb.append("\"").append("\"");
                return sb.toString();
            } else if (!trimmed.startsWith("\"")) {
                sb.append("\"");
                sb.append(trimmed);
                sb.append("\"");
                return sb.toString();
            }
        }
        return input;
    }

    /**
     * if string is begins and ends with single quote character these quotes
     * will be removed in returned string.
     */
    private static String takeOutOfSingleQuote(String input) {
        StringBuilder sb = new StringBuilder();
        if (input != null) {
            String trimmed = input.trim();
            if (trimmed.startsWith("\'") && trimmed.endsWith("\'")) {
                sb.append(trimmed.substring(1, trimmed.lastIndexOf("\'")));
            } else {
                sb.append(input);
            }
            return sb.toString();
        }
        return input;
    }
}
