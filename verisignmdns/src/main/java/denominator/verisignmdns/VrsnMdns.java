/**
 *
 */
package denominator.verisignmdns;


import java.util.List;
import denominator.model.Zone;

import javax.inject.Named;

import feign.Body;
import feign.RequestLine;

/**
 * This class provides interface for invoking request on Verisign MDNS provider
 * @author smahurpawar
 *
 */
public interface VrsnMdns {

	/**
	 * Get list of zone authorized for user
	 * @param aUsername String
	 * @param aPassword String
	 * @return List<Zone>
	 */
    @RequestLine("POST")
    @Body("<?xml version='1.0' encoding='UTF-8'?><S:Envelope xmlns:S='http://www.w3.org/2003/05/soap-envelope'><S:Header><ns2:authInfo xmlns='urn:com:verisign:dnsa:messaging:schema:1' xmlns:ns2='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns3='urn:com:verisign:dnsa:api:schema:1'><ns2:userToken><ns2:userName>{aUsername}</ns2:userName><ns2:password>{aPassword}</ns2:password></ns2:userToken></ns2:authInfo></S:Header><S:Body><ns3:getZoneList xmlns='urn:com:verisign:dnsa:messaging:schema:1' xmlns:ns2='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns3='urn:com:verisign:dnsa:api:schema:1'><ns3:listPagingInfo><ns3:pageNumber>1</ns3:pageNumber><ns3:pageSize>50</ns3:pageSize></ns3:listPagingInfo></ns3:getZoneList></S:Body></S:Envelope>")
    List<Zone> getZonesForUser(@Named("aUsername")String aUsername, @Named("aPassword")String  aPassword);
    
    /**
     * Get list of resource records for the zone
     *
     * @param aUsername
     * @param aPassword
     * @param aZonename
     * @return List<Record>
     */
    @RequestLine("POST")
    @Body("<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope' xmlns:urn='urn:com:verisign:dnsa:messaging:schema:1' xmlns:urn1='urn:com:verisign:dnsa:auth:schema:1' xmlns:urn2='urn:com:verisign:dnsa:api:schema:1'>"
    		+ "<soap:Header>"
    		+ "<urn1:authInfo>"
    		+ "<urn1:userToken>"
    		+ "<urn1:userName>{aUsername}</urn1:userName>"
    		+ "<urn1:password>{aPassword}</urn1:password>"
    		+ "</urn1:userToken>"
    		+ "</urn1:authInfo>"
    		+ "</soap:Header>"
    		+ "<soap:Body>"
    		+ "<urn2:getResourceRecordList>"
    		+ "<urn2:domainName>{aZonename}</urn2:domainName>"
    		+ "</urn2:getResourceRecordList>"
    		+ "</soap:Body>"
    		+ "</soap:Envelope>")
    List<Record> getResourceRecordsList(@Named("aUsername")String aUsername, @Named("aPassword")String  aPassword, @Named("aZonename") String aZonename);

    /**
     * Get resource record list for given zone, type and name
     * @param aUsername
     * @param aPassword
     * @param aZonename
     * @param aType
     * @param aName
     * @return List<Record>
     */
    @RequestLine("POST")
    @Body("<?xml version='1.0' encoding='UTF-8'?>"
    		+ "<S:Envelope xmlns:S='http://www.w3.org/2003/05/soap-envelope'>"
    		+ "<S:Header>"
    		+ "<ns2:authInfo xmlns='urn:com:verisign:dnsa:messaging:schema:1' xmlns:ns2='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns3='urn:com:verisign:dnsa:api:schema:1'>"
    		+ "<ns2:userToken>"
    		+ "<ns2:userName>{aUsername}</ns2:userName>"
    		+ "<ns2:password>{aPassword}</ns2:password>"
    		+ "</ns2:userToken>"
    		+ "</ns2:authInfo>"
    		+ "</S:Header>"
    		+ "<S:Body>"
    		+ "<ns3:getResourceRecordList xmlns='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns2='urn:com:verisign:dnsa:messaging:schema:1' xmlns:ns3='urn:com:verisign:dnsa:api:schema:1'>"
    		+ "<ns3:domainName>{aZonename}</ns3:domainName>"
    		+ "<ns3:resourceRecordType>{aType}</ns3:resourceRecordType>"
    		+ "<ns3:owner>{aName}</ns3:owner>"
    		+ "</ns3:getResourceRecordList>"
    		+ "</S:Body>"
    		+ "</S:Envelope>")
    List<Record> getResourceRecordsListForTypeAndName(@Named("aUsername")String aUsername, @Named("aPassword")String  aPassword, @Named("aZonename") String aZonename, @Named("aType") String aType, @Named("aName") String aName);
    
    /**
     * NOTE - We are using qualifier parameter as id of resource record in MDNS
     * @param aUsername String
     * @param aPassword String
     * @param aQualifier String
     * @return List<Record>
     */
    @RequestLine("POST")
	@Body("<?xml version='1.0' encoding='UTF-8'?>"
			+ "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope' xmlns:urn='urn:com:verisign:dnsa:messaging:schema:1' xmlns:urn1='urn:com:verisign:dnsa:auth:schema:1' xmlns:urn2='urn:com:verisign:dnsa:api:schema:1'>"
			+ "<soap:Header>" 
			+ "<urn1:authInfo>" 
			+ "<urn1:userToken>"
			+ "<urn1:userName>{aUsername}</urn1:userName>"
			+ "<urn1:password>{aPassword}</urn1:password>" 
			+ "</urn1:userToken>"
			+ "</urn1:authInfo>" 
			+ "</soap:Header>" 
			+ "<soap:Body>"
			+ "<urn2:getResourceRecord>"
			+ "<urn2:resourceRecordId>{anId}</urn2:resourceRecordId>"
			+ "</urn2:getResourceRecord>" 
			+ "</soap:Body>" 
			+ "</soap:Envelope>")
    List<Record> getResourceRecordByQualifier(@Named("aUsername")String aUsername, @Named("aPassword")String  aPassword, @Named("anId") String aQualifier);
    
    /**
     * Add a resource record in zone
     * @param aUsername
     * @param aPassword
     * @param aZonename
     * @param aType
     * @param aName
     * @param aTtl
     * @param aRdata
     */
    @RequestLine("POST")
    @Body("<?xml version='1.0' encoding='UTF-8'?>"
    		+ "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope' xmlns:urn='urn:com:verisign:dnsa:messaging:schema:1' xmlns:urn1='urn:com:verisign:dnsa:auth:schema:1' xmlns:urn2='urn:com:verisign:dnsa:api:schema:1'>"
    		+ "<soap:Header>"
    		+ "<urn1:authInfo>"
    		+ "<urn1:userToken>"
    		+ "<urn1:userName>{aUsername}</urn1:userName>"
    		+ "<urn1:password>{aPassword}</urn1:password>"
    		+ "</urn1:userToken>"
    		+ "</urn1:authInfo>"
    		+ "</soap:Header>"
    		+ "<soap:Body>"
    		+ "<urn2:createResourceRecords>"
    		+ "<urn2:domainName>{aZonename}</urn2:domainName>"
    		+ "<urn2:resourceRecord allowanyIP='false'>"
    		+ "<urn2:owner>{aName}</urn2:owner>"
    		+ "<urn2:type>{aType}</urn2:type>"
    		+ "<urn2:ttl>{aTtl}</urn2:ttl>"
    		+ "<urn2:rData>{aRdata}</urn2:rData>"
    		+ "</urn2:resourceRecord>"
    		+ "</urn2:createResourceRecords>"
    		+ "</soap:Body>"
    		+ "</soap:Envelope>")
    void createResourceRecord(@Named("aUsername")String aUsername, @Named("aPassword")String  aPassword
    							 , @Named("aZonename") String aZonename, @Named("aType") String aType
    							 ,@Named("aName") String aName, @Named("aTtl") String aTtl, @Named("aRdata") String aRdata); 

    /**
     * Delete resource record record from zone using record Id
     * @param aUsername
     * @param aPassword
     * @param aZonename
     * @param anId
     */
    @RequestLine("POST")
    @Body("<?xml version='1.0' encoding='UTF-8'?>"  
    + "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope' xmlns:urn='urn:com:verisign:dnsa:messaging:schema:1' xmlns:urn1='urn:com:verisign:dnsa:auth:schema:1' xmlns:urn2='urn:com:verisign:dnsa:api:schema:1'>"
    + "<soap:Header>"
    + "<urn1:authInfo>"
    + "<urn1:userToken>"
    + "<urn1:userName>{aUsername}</urn1:userName>"
    + "<urn1:password>{aPassword}</urn1:password>"
    + "</urn1:userToken>"
    + "</urn1:authInfo>"
    + "</soap:Header>"
    + "<soap:Body>"
    + "<urn2:deleteResourceRecords>"
    + "<urn2:domainName>{aZonename}</urn2:domainName>"
    + "<urn2:resourceRecordId>{anId}</urn2:resourceRecordId>"
    + "</urn2:deleteResourceRecords>"
    + "</soap:Body>"
    + "</soap:Envelope>")
    void deleteRecourceRecord(@Named("aUsername")String aUsername, @Named("aPassword")String  aPassword
			 , @Named("aZonename") String aZonename, @Named("anId") String anId);
    
    static class Record {
        String id;
        String name;
        String type;
        int ttl;
        List<String> rdata;
    }
}
