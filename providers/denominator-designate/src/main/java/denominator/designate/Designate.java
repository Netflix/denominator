package denominator.designate;

import java.util.List;

import javax.inject.Named;

import denominator.model.Zone;
import feign.Headers;
import feign.RequestLine;

// http://docs.hpcloud.com/api/dns/#4.RESTAPISpecifications
public interface Designate {
    // String result as we really don't care at the moment
    @RequestLine("GET /limits")
    String limits();

    @RequestLine("GET /domains")
    List<Zone> domains();

    @RequestLine("GET /domains/{domainId}/records")
    List<Record> records(@Named("domainId") String domainId);

    @RequestLine("POST /domains/{domainId}/records")
    @Headers("Content-Type: application/json")
    Record createRecord(@Named("domainId") String domainId, Record record);

    @RequestLine("PUT /domains/{domainId}/records/{recordId}")
    @Headers("Content-Type: application/json")
    Record updateRecord(@Named("domainId") String domainId, @Named("recordId") String recordId, Record record);

    @RequestLine("DELETE /domains/{domainId}/records/{recordId}")
    void deleteRecord(@Named("domainId") String domainId, @Named("recordId") String recordId);

    static class Record {
        String id;
        String name;
        String type;
        Integer ttl;
        String data;
        Integer priority;

        // toString ordering
        @Override
        public String toString() {
            return new StringBuilder(name).append(type).append(ttl).append(data).append(priority).toString();
        }
    }
}
