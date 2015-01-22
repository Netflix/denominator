/**
 * 
 */
package denominator.verisignmdns;

import static denominator.CredentialsConfiguration.credentials;
import static feign.Util.UTF_8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import dagger.ObjectGraph;
import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.Denominator;
import denominator.ResourceRecordSetApi;
import denominator.ZoneApi;
import denominator.verisignmdns.VerisignMDNSAllProfileResourceRecordSetApi;
import denominator.verisignmdns.VerisignMDNSProvider;
import denominator.verisignmdns.VerisignMDNSProvider.XMLCodec;
import denominator.verisignmdns.VrsnMdns.Record;
import feign.Response;
import feign.codec.ErrorDecoder;

public class VrsnMDNSTest {
    public static final String VALID_TTL1 = "86000";
    public static final String VALID_RR_TYPE2 = "TXT";
    public static final String VALID_RR_TYPE1 = "CNAME";
    public static final String TEST_USER_NAME = "testUser";
    public static final String TEST_PASSWORD = "testPass";
    public static final String VALID_RR_TYPE3 = "NAPTR";
    public static final String METHODKEY = "testMethodKey";
    public static final String VALID_RDATA1 = "dummy_rdata1";
    public static final String VALID_RDATA2 = "dummy_rdata2";
    public static final String RESOURCE_RECORD_ID = "19049261";
    public static final String VALID_QUALIFIER = "testqualifier";
    public static final String VALID_ZONE_NAME1 = "dummyDomain1.com";
    public static final String VALID_ZONE_NAME2 = "dummyDomain2.co.cc";
    public static final String VALID_OWNER1 = "test1." + VALID_ZONE_NAME1;
    public static final String VALID_OWNER2 = "test2." + VALID_ZONE_NAME2;
    public static final String VALID_URL = "https://api.dns-tool.com/dnsa-ws/V2.0/dnsaapi";
    public static final String VALID_RData_NAPTR = "100 50 \"a\" \"z3950+n2l+n2c\" \"\" cidserver.example.com.";

    static final String TEMPLATE_HEAD = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<S:Envelope xmlns:S='http://www.w3.org/2003/05/soap-envelope' xmlns:urn='urn:com:verisign:dnsa:messaging:schema:1' xmlns:urn1='urn:com:verisign:dnsa:auth:schema:1' xmlns:urn2='urn:com:verisign:dnsa:api:schema:1'>"
            + "<S:Header>" + "<urn1:authInfo>" + "<urn1:userToken>" + "<urn1:userName>%s</urn1:userName>"
            + "<urn1:password>%s</urn1:password>" + "</urn1:userToken>" + "</urn1:authInfo>" + "</S:Header>"
            + "<S:Body>";
    static final String TEMPLATE_TAIL = "</S:Body>" + "</S:Envelope>";

    public static final String createRequestARecordTemplete = TEMPLATE_HEAD + "<urn2:createResourceRecords>"
            + "<urn2:domainName>" + VALID_ZONE_NAME1 + "</urn2:domainName>"
            + "<urn2:resourceRecord allowanyIP='false'>" + "<urn2:owner>" + VALID_OWNER1 + "</urn2:owner>"
            + "<urn2:type>" + VALID_RR_TYPE1 + "</urn2:type>" + "<urn2:ttl>" + VALID_TTL1 + "</urn2:ttl>"
            + "<urn2:rData>" + VALID_RDATA1 + "</urn2:rData>" + "</urn2:resourceRecord>"
            + "</urn2:createResourceRecords>" + TEMPLATE_TAIL;

    public static final String createRequestARecordResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<S:Envelope xmlns:S=\"http://www.w3.org/2003/05/soap-envelope\">"
            + "<S:Body>"
            + "<ns2:dnsaWSRes xmlns=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns2=\"urn:com:verisign:dnsa:api:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns4=\"urn:com:verisign:dnsa:messaging:schema:1\">"
            + "<ns2:callSuccess>true</ns2:callSuccess>" + "</ns2:dnsaWSRes>" + "</S:Body>" + "</S:Envelope>";

    public static final String rrDeleteTemplete = TEMPLATE_HEAD + "<urn2:deleteResourceRecords>" + "<urn2:domainName>"
            + VALID_ZONE_NAME1 + "</urn2:domainName>" + "<urn2:resourceRecordId>" + RESOURCE_RECORD_ID
            + "</urn2:resourceRecordId>" + "</urn2:deleteResourceRecords>" + TEMPLATE_TAIL;

    public static final String rrDeleteResponse = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<S:Envelope xmlns:S='http://www.w3.org/2003/05/soap-envelope'>"
            + "<S:Body>"
            + "<ns2:dnsaWSRes xmlns='urn:com:verisign:dnsa:api:schema:2' xmlns:ns2='urn:com:verisign:dnsa:api:schema:1' xmlns:ns3='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns4='urn:com:verisign:dnsa:messaging:schema:1'>"
            + "<ns2:callSuccess>true</ns2:callSuccess>" + "</ns2:dnsaWSRes>" + "</S:Body>" + "</S:Envelope>";

    public static final String rrListCNAMETypesResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<S:Envelope xmlns:S=\"http://www.w3.org/2003/05/soap-envelope\">"
            + "<S:Body>"
            + "<ns2:getResourceRecordListRes "
            + "xmlns=\"urn:com:verisign:dnsa:api:schema:2\" "
            + "xmlns:ns2=\"urn:com:verisign:dnsa:api:schema:1\" "
            + "xmlns:ns3=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns4=\"urn:com:verisign:dnsa:messaging:schema:1\">"
            + "<ns2:callSuccess>true</ns2:callSuccess>" + "<ns2:totalCount>2</ns2:totalCount>" + "<ns2:resourceRecord>"
            + "<ns2:resourceRecordId>" + RESOURCE_RECORD_ID + "</ns2:resourceRecordId>"
            + "<ns2:owner>mbvdemo.mbv-demo.cc.</ns2:owner>" + "<ns2:type>CNAME</ns2:type>" + "<ns2:ttl>86400</ns2:ttl>"
            + "<ns2:rData>myhost.mbv-demo.cc.</ns2:rData>" + "</ns2:resourceRecord>"
            + "</ns2:getResourceRecordListRes>" + "</S:Body>" + "</S:Envelope>";

    public static final String rrListCNAMETypesTemplete = TEMPLATE_HEAD + "<urn2:getResourceRecord>"
            + "<urn2:resourceRecordId>" + RESOURCE_RECORD_ID + "</urn2:resourceRecordId>" + "</urn2:getResourceRecord>"
            + TEMPLATE_TAIL;

    public static final String getrrListCNAMETypesTemplete = TEMPLATE_HEAD
            + "<ns3:getResourceRecordList xmlns=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:1\">"
            + "<ns3:domainName>" + VALID_ZONE_NAME1 + "</ns3:domainName>" + "<ns3:resourceRecordType>" + VALID_RR_TYPE1
            + "</ns3:resourceRecordType>" + "</ns3:getResourceRecordList>" + TEMPLATE_TAIL;

    public static final String nAPTRDataResponse = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<S:Envelope xmlns:S='http://www.w3.org/2003/05/soap-envelope'>"
            + "<S:Body>"
            + "<ns2:getResourceRecordListRes xmlns='urn:com:verisign:dnsa:api:schema:2' xmlns:ns2='urn:com:verisign:dnsa:api:schema:1' xmlns:ns3='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns4='urn:com:verisign:dnsa:messaging:schema:1'>"
            + "<ns2:callSuccess>true</ns2:callSuccess>" + "<ns2:totalCount>2</ns2:totalCount>" + "<ns2:resourceRecord>"
            + "<ns2:resourceRecordId>19076156</ns2:resourceRecordId>" + "<ns2:owner>"
            + VALID_OWNER1
            + "</ns2:owner>"
            + "<ns2:type>"
            + VALID_RR_TYPE3
            + "</ns2:type>"
            + "<ns2:ttl>"
            + VALID_TTL1
            + "</ns2:ttl>"
            + "<ns2:rData>"
            + VALID_RData_NAPTR
            + "</ns2:rData>"
            + "</ns2:resourceRecord>"
            + "<ns2:resourceRecord>"
            + "<ns2:resourceRecordId>19049261</ns2:resourceRecordId>"
            + "<ns2:owner>"
            + VALID_OWNER2
            + "</ns2:owner>"
            + "<ns2:type>"
            + VALID_RR_TYPE3
            + "</ns2:type>"
            + "<ns2:ttl>"
            + VALID_TTL1
            + "</ns2:ttl>"
            + "<ns2:rData>"
            + VALID_RData_NAPTR
            + "</ns2:rData>"
            + "</ns2:resourceRecord>"
            + "</ns2:getResourceRecordListRes>" + "</S:Body>" + "</S:Envelope>";

    public static final String zoneListRequestTemplate = TEMPLATE_HEAD
            + "<ns3:getZoneList xmlns='urn:com:verisign:dnsa:messaging:schema:1' xmlns:ns2='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns3='urn:com:verisign:dnsa:api:schema:1'>"
            + "<ns3:listPagingInfo><ns3:pageNumber>1</ns3:pageNumber><ns3:pageSize>500</ns3:pageSize>"
            + "</ns3:listPagingInfo>" + "</ns3:getZoneList>" + TEMPLATE_TAIL;

    public static final String zoneListResponse = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<S:Envelope xmlns:S='http://www.w3.org/2003/05/soap-envelope'>"
            + "<S:Header>"
            + "<ns2:reliableMessageRes xmlns:ns2='urn:com:verisign:dnsa:messaging:schema:1'>"
            + "<ns2:MessageId>1111011</ns2:MessageId>"
            + "</ns2:reliableMessageRes>"
            + "</S:Header>"
            + "<S:Body>"
            + "<ns2:getZoneListRes xmlns='urn:com:verisign:dnsa:api:schema:2' xmlns:ns2='urn:com:verisign:dnsa:api:schema:1' xmlns:ns3='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns4='urn:com:verisign:dnsa:messaging:schema:1'>"
            + "<ns2:callSuccess>true</ns2:callSuccess>" + "<ns2:totalCount>2</ns2:totalCount>" + "<ns2:zoneInfo>"
            + "<ns2:domainName>dummyDomain1.com</ns2:domainName>" + "<ns2:type>DNS Hosting</ns2:type>"
            + "<ns2:status>ACTIVE</ns2:status>" + "<ns2:createTimestamp>2014-05-15T16:21:33.000Z</ns2:createTimestamp>"
            + "<ns2:updateTimestamp>2014-08-07T20:57:49.000Z</ns2:updateTimestamp>"
            + "<ns2:geoLocationEnabled>No</ns2:geoLocationEnabled>" + "</ns2:zoneInfo>" + "<ns2:zoneInfo>"
            + "<ns2:domainName>dummyDomain2.co.cc</ns2:domainName>" + "<ns2:type>DNS Hosting</ns2:type>"
            + "<ns2:status>ACTIVE</ns2:status>" + "<ns2:createTimestamp>2010-04-05T06:29:33.000Z</ns2:createTimestamp>"
            + "<ns2:updateTimestamp>2014-07-01T08:29:20.000Z</ns2:updateTimestamp>"
            + "<ns2:geoLocationEnabled>No</ns2:geoLocationEnabled>" + "</ns2:zoneInfo>" + "</ns2:getZoneListRes>"
            + "</S:Body>" + "</S:Envelope>";

    public static final String authFailureResponse = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<S:Envelope xmlns:S='http://www.w3.org/2003/05/soap-envelope'>"
            + "<S:Header>"
            + "<ns2:reliableMessageRes xmlns:ns2='urn:com:verisign:dnsa:messaging:schema:1'><ns2:MessageId>1111011</ns2:MessageId>"
            + "</ns2:reliableMessageRes>"
            + "</S:Header>"
            + "<S:Body>"
            + "<ns3:Fault xmlns:ns2='http://schemas.xmlsoap.org/soap/envelope/' xmlns:ns3='http://www.w3.org/2003/05/soap-envelope'>"
            + "<ns3:Code><ns3:Value>ns3:Receiver</ns3:Value></ns3:Code>"
            + "<ns3:Reason><ns3:Text xml:lang='en'>ERROR_OPERATION_FAILURE</ns3:Text></ns3:Reason>"
            + "<ns3:Detail>"
            + "<ns2:dnsaWSRes xmlns:ns2='urn:com:verisign:dnsa:api:schema:1' xmlns:ns3='urn:com:verisign:dnsa:api:schema:2' xmlns:ns4='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns5='urn:com:verisign:dnsa:messaging:schema:1'>"
            + "<ns2:callSuccess>false</ns2:callSuccess>"
            + "<ns2:reason code='ERROR_OPERATION_FAILURE' description='Authentication Failed. Please verify your Username and Password.'/>"
            + "</ns2:dnsaWSRes>" + "</ns3:Detail>" + "</ns3:Fault>" + "</S:Body>" + "</S:Envelope>";

    public static final String rrListRequestTemplate = TEMPLATE_HEAD + "<urn2:getResourceRecordList>"
            + "<urn2:domainName>%s</urn2:domainName>" + "</urn2:getResourceRecordList>" + TEMPLATE_TAIL;

    public static final String rrByNameAndTypeTemplate = TEMPLATE_HEAD
            + "<ns3:getResourceRecordList xmlns='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns2='urn:com:verisign:dnsa:messaging:schema:1' xmlns:ns3='urn:com:verisign:dnsa:api:schema:1'>"
            + "<ns3:domainName>%s</ns3:domainName>" + "<ns3:resourceRecordType>%s</ns3:resourceRecordType>"
            + "<ns3:owner>%s</ns3:owner>" + "</ns3:getResourceRecordList>" + TEMPLATE_TAIL;

    public static final String rrListValildResponse = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<S:Envelope xmlns:S='http://www.w3.org/2003/05/soap-envelope'>"
            + "<S:Body>"
            + "<ns2:getResourceRecordListRes xmlns='urn:com:verisign:dnsa:api:schema:2' xmlns:ns2='urn:com:verisign:dnsa:api:schema:1' xmlns:ns3='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns4='urn:com:verisign:dnsa:messaging:schema:1'>"
            + "<ns2:callSuccess>true</ns2:callSuccess>" + "<ns2:totalCount>2</ns2:totalCount>" + "<ns2:resourceRecord>"
            + "<ns2:resourceRecordId>19076156</ns2:resourceRecordId>" + "<ns2:owner>"
            + VALID_OWNER1
            + "</ns2:owner>"
            + "<ns2:type>"
            + VALID_RR_TYPE1
            + "</ns2:type>"
            + "<ns2:ttl>"
            + VALID_TTL1
            + "</ns2:ttl>"
            + "<ns2:rData>"
            + VALID_RDATA1
            + "</ns2:rData>"
            + "</ns2:resourceRecord>"
            + "<ns2:resourceRecord>"
            + "<ns2:resourceRecordId>19049261</ns2:resourceRecordId>"
            + "<ns2:owner>"
            + VALID_OWNER2
            + "</ns2:owner>"
            + "<ns2:type>"
            + VALID_RR_TYPE2
            + "</ns2:type>"
            + "<ns2:ttl>"
            + VALID_TTL1
            + "</ns2:ttl>"
            + "<ns2:rData>"
            + VALID_RDATA2
            + "</ns2:rData>"
            + "</ns2:resourceRecord>"
            + "</ns2:getResourceRecordListRes>" + "</S:Body>" + "</S:Envelope>";

    public static final String rrListValidResponseNoRecords = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<S:Envelope xmlns:S='http://www.w3.org/2003/05/soap-envelope'>"
            + "<S:Body>"
            + "<ns2:getResourceRecordListRes xmlns='urn:com:verisign:dnsa:api:schema:2' xmlns:ns2='urn:com:verisign:dnsa:api:schema:1' xmlns:ns3='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns4='urn:com:verisign:dnsa:messaging:schema:1'>"
            + "<ns2:callSuccess>true</ns2:callSuccess>" + "<ns2:totalCount>0</ns2:totalCount>"
            + "</ns2:getResourceRecordListRes>" + "</S:Body>" + "</S:Envelope>";

    public static final String rrListInvalidZoneResponse = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<S:Envelope xmlns:S='http://www.w3.org/2003/05/soap-envelope'>"
            + "<S:Body>"
            + "<S:Fault xmlns:ns4='http://schemas.xmlsoap.org/soap/envelope/'>"
            + "<S:Code>"
            + "<S:Value>S:Receiver</S:Value>"
            + "</S:Code>"
            + "<S:Reason>"
            + "<S:Text xml:lang='en'>ERROR_OPERATION_FAILURE</S:Text>"
            + "</S:Reason>"
            + "<S:Detail>"
            + "<ns2:dnsaWSRes xmlns='urn:com:verisign:dnsa:api:schema:2' xmlns:ns2='urn:com:verisign:dnsa:api:schema:1' xmlns:ns3='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns4='urn:com:verisign:dnsa:messaging:schema:1'>"
            + "<ns2:callSuccess>false</ns2:callSuccess>"
            + "<ns2:reason code='ERROR_OPERATION_FAILURE' description='The domain name could not be found.'/>"
            + "</ns2:dnsaWSRes>" + "</S:Detail>" + "</S:Fault>" + "</S:Body>" + "</S:Envelope>";

    public static ZoneApi mockZoneApi(final int port) {
        return Denominator.create(new VerisignMDNSProvider() {
            @Override
            public String url() {
                return "http://localhost:" + port + "/";
            }
        }, credentials(TEST_USER_NAME, TEST_PASSWORD)).api().zones();
    }

    public static ResourceRecordSetApi mockResourceRecordSetApi(final int port) {
        return Denominator.create(new VerisignMDNSProvider() {
            @Override
            public String url() {
                return "http://localhost:" + port + "/";
            }
        }, credentials(TEST_USER_NAME, TEST_PASSWORD)).api().basicRecordSetsInZone(VALID_ZONE_NAME1);
    }

    public static VerisignMDNSAllProfileResourceRecordSetApi mockAllProfileResourceRecordSetApi(final int port) {
        return (VerisignMDNSAllProfileResourceRecordSetApi) Denominator.create(new VerisignMDNSProvider() {
            @Override
            public String url() {
                return "http://localhost:" + port + "/";
            }
        }, credentials(TEST_USER_NAME, TEST_PASSWORD)).api().recordSetsInZone(VALID_ZONE_NAME1);
    }

    public static javax.inject.Provider<Credentials> mockProviderCredentials() {
        return new javax.inject.Provider<Credentials>() {
            @Override
            public Credentials get() {
                List<String> tempList = new ArrayList<String>();
                tempList.add(TEST_USER_NAME);
                tempList.add(TEST_PASSWORD);
                return (ListCredentials) new TestListCredentials(tempList);
            }
        };
    }

    public static ErrorDecoder mockErrorDecoder() {
        ErrorDecoder errorDecoder = ObjectGraph.create(new XMLCodec()).get(ErrorDecoder.class);
        return errorDecoder;
    }

    public static String[] mockArrayList() {
        String[] result = { "testItem1", "testItem2", "testItem3" };
        return result;
    }

    static Response mockResponse(String body) {
        return Response.create(200, "OK", ImmutableMap.<String, Collection<String>> of(), body, UTF_8);
    }

    public static Map<String, Collection<String>> MockcredentialTypeToParameterNamesResponse() {
        Map<String, Collection<String>> options = new LinkedHashMap<String, Collection<String>>();
        options.put("password", Arrays.asList(TEST_USER_NAME, TEST_PASSWORD));
        return options;
    }

    public static Map<String, Collection<String>> mockProfileToRecordTypesResponse() {
        Map<String, Collection<String>> profileToRecordTypes = new LinkedHashMap<String, Collection<String>>();
        return profileToRecordTypes;
    }

    public static Record mockRecord() {
        List<String> rData = new ArrayList<String>();
        rData.add(VALID_RData_NAPTR);
        Record record = new Record();
        record.id = VALID_TTL1;
        record.name = VALID_OWNER1;
        record.type = VALID_RR_TYPE3;
        record.ttl = Integer.parseInt(VALID_TTL1);
        record.rdata = rData;

        return record;
    }

    public static Record mockCNameRecord() {
        List<String> rData = new ArrayList<String>();
        rData.add(VALID_RDATA1);
        Record record = new Record();
        record.id = VALID_TTL1;
        record.name = VALID_OWNER1;
        record.type = VALID_RR_TYPE1;
        record.ttl = Integer.parseInt(VALID_TTL1);
        record.rdata = rData;

        return record;
    }

    @SuppressWarnings("serial")
    public static class TestListCredentials extends denominator.Credentials.ListCredentials {
        protected TestListCredentials(Collection<?> args) {
            super(args);
        }
    }
}
