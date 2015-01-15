package denominator.verisignmdns;

import java.util.List;
import denominator.model.Zone;

import javax.inject.Named;

import feign.Body;
import feign.RequestLine;

interface VrsnMdns {

	@RequestLine("POST")
	@Body("<ns3:getZoneList xmlns='urn:com:verisign:dnsa:messaging:schema:1' xmlns:ns2='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns3='urn:com:verisign:dnsa:api:schema:1'>"
			+ "<ns3:listPagingInfo>"
			+ "<ns3:pageNumber>1</ns3:pageNumber>"
			+ "<ns3:pageSize>500</ns3:pageSize>"
			+ "</ns3:listPagingInfo>"
			+ "</ns3:getZoneList>")
	List<Zone> getZonesForUser();

	@RequestLine("POST")
	@Body("<urn2:getResourceRecordList>"
			+ "<urn2:domainName>{zonename}</urn2:domainName>"
			+ "</urn2:getResourceRecordList>")
	List<Record> getResourceRecordsList(@Named("zonename") String zonename);

	@RequestLine("POST")
	@Body("<ns3:getResourceRecordList xmlns='urn:com:verisign:dnsa:auth:schema:1' xmlns:ns2='urn:com:verisign:dnsa:messaging:schema:1' xmlns:ns3='urn:com:verisign:dnsa:api:schema:1'>"
			+ "<ns3:domainName>{zonename}</ns3:domainName>"
			+ "<ns3:resourceRecordType>{type}</ns3:resourceRecordType>"
			+ "<ns3:owner>{name}</ns3:owner>" + "</ns3:getResourceRecordList>")
	List<Record> getResourceRecordsListForTypeAndName(
			@Named("zonename") String zonename, @Named("type") String type,
			@Named("name") String name);

	@RequestLine("POST")
	@Body("<urn2:getResourceRecord>"
			+ "<urn2:resourceRecordId>{id}</urn2:resourceRecordId>"
			+ "</urn2:getResourceRecord>")
	List<Record> getResourceRecordByQualifier(@Named("id") String id);

	@RequestLine("POST")
	@Body("<urn2:createResourceRecords>"
			+ "<urn2:domainName>{zonename}</urn2:domainName>"
			+ "<urn2:resourceRecord allowanyIP='false'>"
			+ "<urn2:owner>{name}</urn2:owner>"
			+ "<urn2:type>{type}</urn2:type>" + "<urn2:ttl>{ttl}</urn2:ttl>"
			+ "<urn2:rData>{rdata}</urn2:rData>" + "</urn2:resourceRecord>"
			+ "</urn2:createResourceRecords>")
	void createResourceRecord(@Named("zonename") String zonename,
			@Named("type") String type, @Named("name") String name,
			@Named("ttl") String ttl, @Named("rdata") String rdata);

	@RequestLine("POST")
	@Body("<urn2:deleteResourceRecords>"
			+ "<urn2:domainName>{zonename}</urn2:domainName>"
			+ "<urn2:resourceRecordId>{id}</urn2:resourceRecordId>"
			+ "</urn2:deleteResourceRecords>")
	void deleteRecourceRecord(@Named("zonename") String zonename,
			@Named("id") String id);

	static class Record {
		String id;
		String name;
		String type;
		int ttl;
		List<String> rdata;
	}
}
