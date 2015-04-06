package denominator.designate;

import java.util.List;
import java.util.Map;

import denominator.model.Zone;
import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

// http://designate.readthedocs.org/en/latest/rest.html#v1-api
public interface Designate {

  @RequestLine("GET /limits")
  Map<String, Object> limits();

  @RequestLine("GET /domains")
  List<Zone> domains();

  @RequestLine("POST /domains")
  @Body("%7B\"name\":\"{name}\",\"ttl\":{ttl},\"email\":\"{email}\"%7D")
  @Headers("Content-Type: application/json")
  Zone createDomain(@Param("name") String name, @Param("email") String email,
                    @Param("ttl") int ttl);

  @RequestLine("PUT /domains/{id}")
  @Body("%7B\"id\":\"{id}\",\"name\":\"{name}\",\"ttl\":{ttl},\"email\":\"{email}\"%7D")
  @Headers("Content-Type: application/json")
  Zone updateDomain(@Param("id") String id, @Param("name") String name,
                    @Param("email") String email, @Param("ttl") int ttl);

  @RequestLine("DELETE /domains/{domainId}")
  void deleteDomain(@Param("domainId") String domainId);

  @RequestLine("GET /domains/{domainId}/records")
  List<Record> records(@Param("domainId") String domainId);

  @RequestLine("POST /domains/{domainId}/records")
  @Headers("Content-Type: application/json")
  Record createRecord(@Param("domainId") String domainId, Record record);

  @RequestLine("PUT /domains/{domainId}/records/{recordId}")
  @Headers("Content-Type: application/json")
  Record updateRecord(@Param("domainId") String domainId, @Param("recordId") String recordId,
                      Record record);

  @RequestLine("DELETE /domains/{domainId}/records/{recordId}")
  void deleteRecord(@Param("domainId") String domainId, @Param("recordId") String recordId);

  class Record {

    String id;
    String name;
    String type;
    Integer ttl;
    String data;
    Integer priority;

    // toString ordering
    @Override
    public String toString() {
      return new StringBuilder(name).append(type).append(ttl).append(data).append(priority)
          .toString();
    }
  }
}
