package denominator.clouddns;

import static com.google.common.base.Objects.toStringHelper;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URI;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.google.common.base.Function;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;

import denominator.model.Zone;
import feign.FeignException;
import feign.RequestTemplate.Body;

class RackspaceApis {
    static interface CloudIdentity {
        @POST
        @Path("/tokens")
        @Body("%7B\"auth\":%7B\"RAX-KSKEY:apiKeyCredentials\":%7B\"username\":\"{username}\",\"apiKey\":\"{apiKey}\"%7D%7D%7D")
        @Produces(APPLICATION_JSON)
        TokenIdAndPublicURL apiKeyAuth(URI endpoint, @FormParam("username") String username,
                @FormParam("apiKey") String apiKey);
    }

    static class TokenIdAndPublicURL {
        String tokenId;
        String publicURL;
    }

    static interface CloudDNS {
        @GET
        ListWithNext<Zone> domains(URI href);

        @GET
        @Path("/domains")
        ListWithNext<Zone> domains();

        @GET
        ListWithNext<Record> records(URI href);

        @GET
        @Path("/domains/{domainId}/records")
        ListWithNext<Record> records(@PathParam("domainId") int id);

        @GET
        @Path("/domains/{domainId}/records")
        ListWithNext<Record> recordsByNameAndType(@PathParam("domainId") int id,
                @QueryParam("name") String nameFilter, @QueryParam("type") String typeFilter);
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
            return toStringHelper("").omitNullValues().add("name", name).add("type", type).add("ttl", ttl)
                    .add("data", data).add("priority", priority).toString();
        }
    }

    static class ListWithNext<X> extends ForwardingList<X> {
        List<X> records = ImmutableList.of();
        URI next;

        @Override
        protected List<X> delegate() {
            return records;
        }
    }

    static <X> ListWithNext<X> emptyOn404(Function<URI, ListWithNext<X>> pagingFunction, URI nullOrNext) {
        try {
            return pagingFunction.apply(nullOrNext);
        } catch (FeignException e) {
            if (e.getMessage().indexOf("status 404") != -1) {
                return new ListWithNext<X>();
            }
            throw e;
        }
    }
}
