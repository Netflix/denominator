package denominator.clouddns;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import javax.inject.Named;

import denominator.model.Zone;
import feign.Body;
import feign.FeignException;
import feign.Headers;
import feign.RequestLine;

class RackspaceApis {

  static <X> ListWithNext<X> emptyOn404(Pager<X> pagingFunction, URI nullOrNext) {
    try {
      return pagingFunction.apply(nullOrNext);
    } catch (FeignException e) {
      if (e.getMessage().indexOf("status 404") != -1) {
        return new ListWithNext<X>();
      }
      throw e;
    }
  }

  static interface CloudIdentity {

    @RequestLine("POST /tokens")
    @Body("%7B\"auth\":%7B\"RAX-KSKEY:apiKeyCredentials\":%7B\"username\":\"{username}\",\"apiKey\":\"{apiKey}\"%7D%7D%7D")
    @Headers("Content-Type: application/json")
    TokenIdAndPublicURL apiKeyAuth(URI endpoint, @Named("username") String username,
                                   @Named("apiKey") String apiKey);

    @RequestLine("POST /tokens")
    @Body("%7B\"auth\":%7B\"passwordCredentials\":%7B\"username\":\"{username}\",\"password\":\"{password}\"%7D%7D%7D")
    @Headers("Content-Type: application/json")
    TokenIdAndPublicURL passwordAuth(URI endpoint, @Named("username") String username,
                                     @Named("password") String password);
  }

  static interface CloudDNS {

    @RequestLine("GET /limits")
    Map<String, Object> limits();

    @RequestLine("GET /status/{jobId}?showDetails=true")
    JobIdAndStatus getStatus(@Named("jobId") String jobId);

    @RequestLine("GET")
    ListWithNext<Zone> domains(URI href);

    @RequestLine("GET /domains")
    ListWithNext<Zone> domains();

    @RequestLine("GET")
    ListWithNext<Record> records(URI href);

    @RequestLine("GET /domains/{domainId}/records")
    ListWithNext<Record> records(@Named("domainId") int id);

    @RequestLine("GET /domains/{domainId}/records?name={name}&type={type}")
    ListWithNext<Record> recordsByNameAndType(@Named("domainId") int id,
                                              @Named("name") String nameFilter,
                                              @Named("type") String typeFilter);

    @RequestLine("POST /domains/{domainId}/records")
    @Body("%7B\"records\":[%7B\"name\":\"{name}\",\"type\":\"{type}\",\"ttl\":\"{ttl}\",\"data\":\"{data}\"%7D]%7D")
    @Headers("Content-Type: application/json")
    JobIdAndStatus createRecord(@Named("domainId") int id, @Named("name") String name,
                                @Named("type") String type, @Named("ttl") int ttl,
                                @Named("data") String data);

    @RequestLine("POST /domains/{domainId}/records")
    @Body("%7B\"records\":[%7B\"name\":\"{name}\",\"type\":\"{type}\",\"ttl\":\"{ttl}\",\"data\":\"{data}\",\"priority\":\"{priority}\"%7D]%7D")
    @Headers("Content-Type: application/json")
    JobIdAndStatus createRecordWithPriority(@Named("domainId") int id, @Named("name") String name,
                                            @Named("type") String type, @Named("ttl") int ttl,
                                            @Named("data") String data,
                                            @Named("priority") int priority);

    @RequestLine("PUT /domains/{domainId}/records/{recordId}")
    @Body("%7B\"ttl\":\"{ttl}\",\"data\":\"{data}\"%7D")
    @Headers("Content-Type: application/json")
    JobIdAndStatus updateRecord(@Named("domainId") int domainId, @Named("recordId") String recordId,
                                @Named("ttl") int ttl, @Named("data") String data);

    @RequestLine("DELETE /domains/{domainId}/records/{recordId}")
    JobIdAndStatus deleteRecord(@Named("domainId") int domainId,
                                @Named("recordId") String recordId);
  }

  interface Pager<X> {

    ListWithNext<X> apply(URI nullOrNext);
  }

  static class TokenIdAndPublicURL {

    String tokenId;
    String publicURL;
  }

  static class JobIdAndStatus {

    String id;
    String status;
  }

  static class Record {

    String id;
    String name;
    String type;
    Integer ttl;
    Integer priority;
    private String data;

    public String data() {
      if ("AAAA".equals(type)) {
        return data.toUpperCase();
      }

      return data;
    }

    public void data(String data) {
      this.data = data;
    }

    // toString ordering
    @Override
    public String toString() {
      return new StringBuilder(name).append(type).append(ttl).append(data).append(priority)
          .toString();
    }
  }

  static class ListWithNext<X> extends ArrayList<X> {

    private static final long serialVersionUID = 1L;
    URI next;
  }
}
