package denominator.denominatord;

import java.util.List;

import denominator.model.ResourceRecordSet;
import denominator.model.Zone;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Response;

/**
 * Defines the interface of {@link DenominatorD}, where all responses are in json. <p/> All
 * responses throw a 400 if there is a problem with the request data. <p/> 404 would be unexpected
 * as that implies a malformed request.
 */
public interface DenominatorDApi {

  @RequestLine("GET /healthcheck")
  Response healthcheck();

  @RequestLine("GET /zones")
  List<Zone> zones();

  @RequestLine("GET /zones?name={name}")
  List<Zone> zonesByName(@Param("name") String name);

  @RequestLine("PUT /zones")
  @Headers("Content-Type: application/json")
  Response putZone(Zone update);

  @RequestLine("DELETE /zones/{zoneId}")
  void deleteZone(@Param("zoneId") String zoneId);

  @RequestLine("GET /zones/{zoneId}/recordsets")
  List<ResourceRecordSet<?>> recordSets(@Param("zoneId") String zoneId);

  @RequestLine("GET /zones/{zoneId}/recordsets?name={name}")
  List<ResourceRecordSet<?>> recordSetsByName(@Param("zoneId") String zoneId,
                                              @Param("name") String name);

  @RequestLine("GET /zones/{zoneId}/recordsets?name={name}&type={type}")
  List<ResourceRecordSet<?>> recordSetsByNameAndType(@Param("zoneId") String zoneId,
                                                     @Param("name") String name,
                                                     @Param("type") String type);

  @RequestLine("GET /zones/{zoneId}/recordsets?name={name}&type={type}&qualifier={qualifier}")
  List<ResourceRecordSet<?>> recordsetsByNameAndTypeAndQualifier(
      @Param("zoneId") String zoneId, @Param("name") String name,
      @Param("type") String type, @Param("qualifier") String qualifier);

  @RequestLine("PUT /zones/{zoneId}/recordsets?name={name}")
  @Headers("Content-Type: application/json")
  void putRecordSet(@Param("zoneId") String zoneId, ResourceRecordSet<?> update);

  @RequestLine("DELETE /zones/{zoneId}/recordsets?name={name}&type={type}")
  void deleteRecordSetByNameAndType(@Param("zoneId") String zoneId,
                                    @Param("name") String name,
                                    @Param("type") String type);

  @RequestLine("DELETE /zones/{zoneId}/recordsets?name={name}&type={type}&qualifier={qualifier}")
  void deleteRecordSetByNameTypeAndQualifier(@Param("zoneId") String zoneId,
                                             @Param("name") String name,
                                             @Param("type") String type,
                                             @Param("qualifier") String qualifier);
}
