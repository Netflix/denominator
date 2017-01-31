package denominator.ultradns;

import static java.lang.String.format;

public class UltraDNSMockResponse {

    public static String getNeustarNetworkStatusResponse ="{\n" +
            "    \"message\": \"Good\"\n"
            + "}\n";

    public static String getNeustarNetworkStatusFailedResponse ="";

    public static final String getAccountsListOfUserResponse = "{\n" +
            "    \"resultInfo\": {\n"
            + "        \"totalCount\": 1,\n"
            + "        \"offset\": 0,\n"
            + "        \"returnedCount\": 1\n"
            + "    },\n"
            + "    \"accounts\": [\n"
            + "        {\n"
            + "            \"accountName\": \"npp-rest-test1\",\n"
            + "            \"accountHolderUserName\": \"neustarnpptest1\",\n"
            + "            \"ownerUserName\": \"nppresttest1\",\n"
            + "            \"numberOfUsers\": 1,\n"
            + "            \"numberOfGroups\": 3,\n"
            + "            \"accountType\": \"ORGANIZATION\",\n"
            + "            \"features\": [\n"
            + "                \"ADVDIRECTIONAL\",\n"
            + "                \"DNSSEC\",\n"
            + "                \"MAILFORWARD\",\n"
            + "                \"MDDI\",\n"
            + "                \"RECURSIVE\",\n"
            + "                \"WEBFORWARD\"\n"
            + "            ]\n"
            + "        }\n"
            + "    ]\n"
            + "}";


    public static String getMockErrorResponse(String errorCode, String errorMessage){
        return
                "[\n"
                        + "    {\n"
                        + format("        \"errorCode\": %s,\n", errorCode)
                        + format("        \"errorMessage\": \"%s\"\n", errorMessage)
                        + "    }\n"
                        + "]\n";
    }
}
