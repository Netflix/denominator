package denominator.ultradns;

import static java.lang.String.format;

public class UltraDNSMockResponse {

    public static final String GET_ACCOUNTS_LIST_OF_USER = "{\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 2,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 2\n" +
            "    },\n" +
            "    \"accounts\": [\n" +
            "        {\n" +
            "            \"accountName\": \"npp-rest-test2\",\n" +
            "            \"accountHolderUserName\": \"neustarnpptest2\",\n" +
            "            \"ownerUserName\": \"nppresttest2\",\n" +
            "            \"numberOfUsers\": 1,\n" +
            "            \"numberOfGroups\": 3,\n" +
            "            \"accountType\": \"ORGANIZATION\",\n" +
            "            \"features\": [\n" +
            "                \"ADVDIRECTIONAL\",\n" +
            "                \"DNSSEC\",\n" +
            "                \"MAILFORWARD\",\n" +
            "                \"RECURSIVE\",\n" +
            "                \"REPORTING\",\n" +
            "                \"WEBFORWARD\"\n" +
            "            ]\n" +
            "        },\n" +
            "        {\n" +
            "            \"accountName\": \"npp-rest-test2a\",\n" +
            "            \"accountHolderUserName\": \"neustar eng - npp\",\n" +
            "            \"ownerUserName\": \"npp-rest-test2a\",\n" +
            "            \"numberOfUsers\": 2,\n" +
            "            \"numberOfGroups\": 3,\n" +
            "            \"accountType\": \"ORGANIZATION\",\n" +
            "            \"features\": [\n" +
            "                \"ADVDIRECTIONAL\",\n" +
            "                \"DNSSEC\",\n" +
            "                \"MAILFORWARD\",\n" +
            "                \"WEBFORWARD\"\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    public static final String GET_ACCOUNTS_LIST_OF_USER_RESPONSE = "{\n" +
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

    public static final String GET_RESOURCE_RECORDS_PRESENT = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"pool_2.denominator.io.\",\n" +
            "            \"rrtype\": \"A (1)\",\n" +
            "            \"ttl\": 86400,\n" +
            "            \"rdata\": [\n" +
            "                \"1.1.1.1\",\n" +
            "                \"2.2.2.2\",\n" +
            "                \"3.3.3.3\",\n" +
            "                \"4.4.4.4\",\n" +
            "                \"5.5.5.5\",\n" +
            "                \"6.6.6.6\",\n" +
            "                \"7.7.7.7\"\n" +
            "            ],\n" +
            "            \"profile\": {\n" +
            "                \"@context\": \"http://schemas.ultradns.com/RDPool.jsonschema\",\n" +
            "                \"order\": \"ROUND_ROBIN\",\n" +
            "                \"description\": \"1\"\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";

    public static final String GET_RESOURCE_RECORDS_ABSENT = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 0,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 0\n" +
            "    }\n" +
            "}\n";

    public static final String GET_SOA_RESOURCE_RECORDS = "{\n" +
            "    \"zoneName\": \"denominator.io.\",\n" +
            "    \"rrSets\": [\n" +
            "        {\n" +
            "            \"ownerName\": \"denominator.io.\",\n" +
            "            \"rrtype\": \"SOA (6)\",\n" +
            "            \"ttl\": 86400,\n" +
            "            \"rdata\": [\n" +
            "                \"pdns1.ultradns.net. aaaaaaa\\\\.bbbbbbb.neustar.biz. 2017012518 86400 86400 86400 86400\"\n" +
            "            ]\n" +
            "        }\n" +
            "    ],\n" +
            "    \"queryInfo\": {\n" +
            "        \"sort\": \"OWNER\",\n" +
            "        \"reverse\": false,\n" +
            "        \"limit\": 100\n" +
            "    },\n" +
            "    \"resultInfo\": {\n" +
            "        \"totalCount\": 1,\n" +
            "        \"offset\": 0,\n" +
            "        \"returnedCount\": 1\n" +
            "    }\n" +
            "}\n";


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
