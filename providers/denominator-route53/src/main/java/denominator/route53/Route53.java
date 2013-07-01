package denominator.route53;

import java.util.List;

import javax.inject.Named;

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import feign.Headers;
import feign.RequestLine;

interface Route53 {
    @RequestLine("GET /2012-12-12/hostedzone")
    ZoneList zones();

    @RequestLine("GET /2012-12-12/hostedzone?marker={marker}")
    ZoneList zones(@Named("marker") String marker);

    static class ZoneList extends ForwardingList<Zone> {

        List<Zone> zones = ImmutableList.of();
        String next;

        @Override
        protected List<Zone> delegate() {
            return zones;
        }
    }

    @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset")
    ResourceRecordSetList rrsets(@Named("zoneId") String zoneId);

    @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset?name={name}")
    ResourceRecordSetList rrsetsStartingAtName(@Named("zoneId") String zoneId, @Named("name") String name);

    @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset?name={name}&type={type}")
    ResourceRecordSetList rrsetsStartingAtNameAndType(@Named("zoneId") String zoneId, @Named("name") String name,
            @Named("type") String type);

    @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset?name={name}&type={type}&identifier={identifier}")
    ResourceRecordSetList rrsetsStartingAtNameTypeAndIdentifier(@Named("zoneId") String zoneId,
            @Named("name") String name, @Named("type") String type, @Named("identifier") String identifier);

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

    @RequestLine("POST /2012-12-12/hostedzone/{zoneId}/rrset")
    @Headers("Content-Type: application/xml")
    void changeBatch(@Named("zoneId") String zoneId, List<ActionOnResourceRecordSet> changes)
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
