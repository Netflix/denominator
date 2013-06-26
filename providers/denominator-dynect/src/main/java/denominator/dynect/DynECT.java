package denominator.dynect;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import feign.RequestTemplate.Body;

public interface DynECT {
    @GET
    @Path("/Zone")
    List<Zone> zones();

    @PUT
    @Path("/Zone/{zone}")
    @Body("{\"publish\":true}")
    void publish(@PathParam("zone") String zone);

    @GET
    @Path("/AllRecord/{zone}?detail=Y")
    Iterator<ResourceRecordSet<?>> rrsets(@PathParam("zone") String zone);

    @GET
    @Path("/Geo?detail=Y")
    Multimap<String, ResourceRecordSet<?>> geoRRSetsByZone();

    @GET
    @Path("/AllRecord/{zone}?detail=Y")
    Iterator<ResourceRecordSet<?>> rrsetsInZone(@PathParam("zone") String zone);

    @GET
    @Path("/AllRecord/{zone}/{fqdn}?detail=Y")
    Iterator<ResourceRecordSet<?>> rrsetsInZoneByName(@PathParam("zone") String zone, @PathParam("fqdn") String fqdn);

    @GET
    @Path("/{type}Record/{zone}/{fqdn}?detail=Y")
    Iterator<ResourceRecordSet<?>> rrsetsInZoneByNameAndType(@PathParam("zone") String zone,
            @PathParam("fqdn") String fqdn, @PathParam("type") String type);

    @GET
    @Path("/{type}Record/{zone}/{fqdn}")
    List<String> recordIdsInZoneByNameAndType(@PathParam("zone") String zone, @PathParam("fqdn") String fqdn,
            @PathParam("type") String type);

    @GET
    @Path("/{type}Record/{zone}/{fqdn}?detail=Y")
    Iterator<Record> recordsInZoneByNameAndType(@PathParam("zone") String zone, @PathParam("fqdn") String fqdn,
            @PathParam("type") String type);

    @POST
    @Path("/{type}Record/{zone}/{fqdn}")
    void scheduleCreateRecord(@PathParam("zone") String zone, @PathParam("fqdn") String fqdn,
            @PathParam("type") String type, @FormParam("ttl") int ttl, @FormParam("rdata") Map<String, Object> rdata);

    @DELETE
    @Path("/{recordId}")
    void scheduleDeleteRecord(@PathParam("recordId") String recordId);

    static class Record {
        long id;
        String name;
        String type;
        int ttl;
        Map<String, Object> rdata = ImmutableMap.of();
    }
}
