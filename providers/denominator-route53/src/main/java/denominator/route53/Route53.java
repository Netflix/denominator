package denominator.route53;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;

interface Route53 {
    @GET
    @Path("/2012-12-12/hostedzone")
    ZoneList zones();

    @GET
    @Path("/2012-12-12/hostedzone")
    ZoneList zones(@QueryParam("marker") String marker);

    static class ZoneList extends ForwardingList<Zone> {

        List<Zone> zones = ImmutableList.of();
        String next;

        @Override
        protected List<Zone> delegate() {
            return zones;
        }
    }

    @GET
    @Path("/2012-12-12/hostedzone/{zoneId}/rrset")
    ResourceRecordSetList rrsets(@PathParam("zoneId") String zoneId);

    @GET
    @Path("/2012-12-12/hostedzone/{zoneId}/rrset")
    ResourceRecordSetList rrsetsStartingAtName(@PathParam("zoneId") String zoneId, @QueryParam("name") String name);

    @GET
    @Path("/2012-12-12/hostedzone/{zoneId}/rrset")
    ResourceRecordSetList rrsetsStartingAtNameAndType(@PathParam("zoneId") String zoneId,
            @QueryParam("name") String name, @QueryParam("type") String type);

    @GET
    @Path("/2012-12-12/hostedzone/{zoneId}/rrset")
    ResourceRecordSetList rrsetsStartingAtNameTypeAndIdentifier(@PathParam("zoneId") String zoneId,
            @QueryParam("name") String name, @QueryParam("type") String type,
            @QueryParam("identifier") String identifier);

    static class ResourceRecordSetList extends ForwardingList<ResourceRecordSet<?>> {

        List<ResourceRecordSet<?>> rrsets = ImmutableList.of();
        NextRecord next;

        static class NextRecord {
            final String name;

            NextRecord(String name) {
                this.name = name;
            }

            String type;
            String identifier;
        }

        @Override
        protected List<ResourceRecordSet<?>> delegate() {
            return rrsets;
        }
    }

    @POST
    @Path("/2012-12-12/hostedzone/{zoneId}/rrset")
    @Produces(APPLICATION_XML)
    void changeBatch(@PathParam("zoneId") String zoneId, List<ActionOnResourceRecordSet> changes)
            throws InvalidChangeBatchException;

    static class ActionOnResourceRecordSet {
        static ActionOnResourceRecordSet create(ResourceRecordSet<?> rrs) {
            return new ActionOnResourceRecordSet("CREATE", rrs);
        }

        static ActionOnResourceRecordSet delete(ResourceRecordSet<?> rrs) {
            return new ActionOnResourceRecordSet("DELETE", rrs);
        }

        final String action;
        final ResourceRecordSet<?> rrs;

        private ActionOnResourceRecordSet(String action, ResourceRecordSet<?> rrs) {
            this.action = action;
            this.rrs = rrs;
        }
    }
}
