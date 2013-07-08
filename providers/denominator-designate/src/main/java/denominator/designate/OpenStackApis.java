package denominator.designate;

import static com.google.common.base.Objects.toStringHelper;

import java.net.URI;
import java.util.List;

import javax.inject.Named;

import denominator.model.Zone;
import feign.Body;
import feign.Headers;
import feign.RequestLine;

class OpenStackApis {
    static interface KeystoneV2 {
        @RequestLine("POST /tokens")
        @Body("%7B\"auth\":%7B\"passwordCredentials\":%7B\"username\":\"{username}\",\"password\":\"{password}\"%7D,\"tenantId\":\"{tenantId}\"%7D%7D")
        @Headers("Content-Type: application/json")
        TokenIdAndPublicURL passwordAuth(URI endpoint, @Named("tenantId") String tenantId,
                @Named("username") String username, @Named("password") String password);
    }

    static class TokenIdAndPublicURL {
        String tokenId;
        String publicURL;
    }

    // http://docs.hpcloud.com/api/dns/#4.RESTAPISpecifications
    static interface Designate {
        @RequestLine("GET /domains")
        List<Zone> domains();

        @RequestLine("GET /domains/{domainId}/records")
        List<Record> records(@Named("domainId") String domainId);

        @RequestLine("POST /domains/{domainId}/records")
        @Headers("Content-Type: application/json")
        Record createRecord(@Named("domainId") String domainId, Record record);

        @RequestLine("PUT /domains/{domainId}/records")
        @Headers("Content-Type: application/json")
        Record updateRecord(@Named("domainId") String domainId, Record record);

        @RequestLine("DELETE /domains/{domainId}/records/{recordId}")
        void deleteRecord(@Named("domainId") String domainId, @Named("recordId") String recordId);
    }

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
            return toStringHelper("").omitNullValues().add("name", name).add("type", type).add("ttl", ttl)
                    .add("data", data).add("priority", priority).toString();
        }
    }
}
