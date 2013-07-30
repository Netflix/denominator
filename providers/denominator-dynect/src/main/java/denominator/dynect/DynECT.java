package denominator.dynect;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import feign.Body;
import feign.RequestLine;

public interface DynECT {
    @RequestLine("GET /Zone")
    List<Zone> zones();

    @RequestLine("PUT /Zone/{zone}")
    @Body("{\"publish\":true}")
    void publish(@Named("zone") String zone);

    @RequestLine("GET /AllRecord/{zone}?detail=Y")
    Iterator<ResourceRecordSet<?>> rrsets(@Named("zone") String zone);

    @RequestLine("POST /CheckPermissionReport")
    @Body("{\"permission\":[\"GeoUpdate\",\"GeoDelete\",\"GeoGet\",\"GeoActivate\",\"GeoDeactivate\"]}")
    Boolean hasAllGeoPermissions();

    @RequestLine("GET /Geo?detail=Y")
    Map<String, Collection<ResourceRecordSet<?>>> geoRRSetsByZone();

    @RequestLine("GET /AllRecord/{zone}?detail=Y")
    Iterator<ResourceRecordSet<?>> rrsetsInZone(@Named("zone") String zone);

    @RequestLine("GET /AllRecord/{zone}/{fqdn}?detail=Y")
    Iterator<ResourceRecordSet<?>> rrsetsInZoneByName(@Named("zone") String zone, @Named("fqdn") String fqdn);

    @RequestLine("GET /{type}Record/{zone}/{fqdn}?detail=Y")
    Iterator<ResourceRecordSet<?>> rrsetsInZoneByNameAndType(@Named("zone") String zone, @Named("fqdn") String fqdn,
            @Named("type") String type);

    @RequestLine("GET /{type}Record/{zone}/{fqdn}")
    List<String> recordIdsInZoneByNameAndType(@Named("zone") String zone, @Named("fqdn") String fqdn,
            @Named("type") String type);

    @RequestLine("GET /{type}Record/{zone}/{fqdn}?detail=Y")
    Iterator<Record> recordsInZoneByNameAndType(@Named("zone") String zone, @Named("fqdn") String fqdn,
            @Named("type") String type);

    @RequestLine("POST /{type}Record/{zone}/{fqdn}")
    void scheduleCreateRecord(@Named("zone") String zone, @Named("fqdn") String fqdn, @Named("type") String type,
            @Named("ttl") int ttl, @Named("rdata") Map<String, Object> rdata);

    @RequestLine("DELETE /{recordId}")
    void scheduleDeleteRecord(@Named("recordId") String recordId);

    static class Record {
        long id;
        String name;
        String type;
        int ttl;
        Map<String, Object> rdata = new LinkedHashMap<String, Object>();
    }
}
