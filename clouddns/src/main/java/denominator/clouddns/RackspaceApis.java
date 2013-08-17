package denominator.clouddns;

import java.net.URI;
import java.util.ArrayList;

import javax.inject.Named;

import denominator.model.Zone;
import feign.Body;
import feign.FeignException;
import feign.Headers;
import feign.RequestLine;

class RackspaceApis {
    static interface CloudIdentity {
        @RequestLine("POST /tokens")
        @Body("%7B\"auth\":%7B\"RAX-KSKEY:apiKeyCredentials\":%7B\"username\":\"{username}\",\"apiKey\":\"{apiKey}\"%7D%7D%7D")
        @Headers("Content-Type: application/json")
        TokenIdAndPublicURL apiKeyAuth(URI endpoint, @Named("username") String username, @Named("apiKey") String apiKey);

        @RequestLine("POST /tokens")
        @Body("%7B\"auth\":%7B\"passwordCredentials\":%7B\"username\":\"{username}\",\"password\":\"{password}\"%7D%7D%7D")
        @Headers("Content-Type: application/json")
        TokenIdAndPublicURL passwordAuth(URI endpoint, @Named("username") String username,
                @Named("password") String password);
    }

    static class TokenIdAndPublicURL {
        String tokenId;
        String publicURL;
    }

    static interface CloudDNS {

        // String result as we really don't care at the moment
        @RequestLine("GET /limits")
        String limits();

        @RequestLine("GET")
        ListWithNext<Zone> domains(URI href);

        @RequestLine("GET /domains")
        ListWithNext<Zone> domains();

        @RequestLine("GET")
        ListWithNext<Record> records(URI href);

        @RequestLine("GET /domains/{domainId}/records")
        ListWithNext<Record> records(@Named("domainId") int id);

        @RequestLine("GET /domains/{domainId}/records?name={name}&type={type}")
        ListWithNext<Record> recordsByNameAndType(@Named("domainId") int id, @Named("name") String nameFilter,
                @Named("type") String typeFilter);
    }

    static class Record {
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

    static class ListWithNext<X> extends ArrayList<X> {
        URI next;
        private static final long serialVersionUID = 1L;
    }

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

    interface Pager<X> {
        ListWithNext<X> apply(URI nullOrNext);
    }
}
