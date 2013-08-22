package denominator.route53;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import feign.Headers;
import feign.RequestLine;

interface Route53 {
    @RequestLine("GET /2012-12-12/hostedzone")
    ZoneList listHostedZones();

    @RequestLine("GET /2012-12-12/hostedzone?marker={marker}")
    ZoneList listHostedZones(@Named("marker") String marker);

    static class ZoneList extends ArrayList<Zone> {
        String next;
        private static final long serialVersionUID = 1L;
    }

    @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset")
    ResourceRecordSetList listResourceRecordSets(@Named("zoneId") String zoneId);

    @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset?name={name}")
    ResourceRecordSetList listResourceRecordSets(@Named("zoneId") String zoneId, @Named("name") String name);

    @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset?name={name}&type={type}")
    ResourceRecordSetList listResourceRecordSets(@Named("zoneId") String zoneId, @Named("name") String name,
            @Named("type") String type);

    @RequestLine("GET /2012-12-12/hostedzone/{zoneId}/rrset?name={name}&type={type}&identifier={identifier}")
    ResourceRecordSetList listResourceRecordSets(@Named("zoneId") String zoneId, @Named("name") String name,
            @Named("type") String type, @Named("identifier") String identifier);

    static class ResourceRecordSetList extends ArrayList<ResourceRecordSet<?>> {
        NextRecord next;

        static class NextRecord {
            final String name;

            NextRecord(String name) {
                this.name = name;
            }

            String type;
            String identifier;
        }

        private static final long serialVersionUID = 1L;
    }

    @RequestLine("POST /2012-12-12/hostedzone/{zoneId}/rrset")
    @Headers("Content-Type: application/xml")
    void changeResourceRecordSets(@Named("zoneId") String zoneId, List<ActionOnResourceRecordSet> changes)
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
