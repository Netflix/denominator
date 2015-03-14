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

  @RequestLine("GET /zones/{zoneIdOrName}/recordsets")
  List<ResourceRecordSet<?>> recordSets(@Param("zoneIdOrName") String zoneIdOrName);

  @RequestLine("GET /zones/{zoneIdOrName}/recordsets?name={name}")
  List<ResourceRecordSet<?>> recordSetsByName(@Param("zoneIdOrName") String zoneIdOrName,
                                              @Param("name") String name);

  @RequestLine("GET /zones/{zoneIdOrName}/recordsets?name={name}&type={type}")
  List<ResourceRecordSet<?>> recordSetsByNameAndType(@Param("zoneIdOrName") String zoneIdOrName,
                                                     @Param("name") String name,
                                                     @Param("type") String type);

  @RequestLine("GET /zones/{zoneIdOrName}/recordsets?name={name}&type={type}&qualifier={qualifier}")
  List<ResourceRecordSet<?>> recordsetsByNameAndTypeAndQualifier(
      @Param("zoneIdOrName") String zoneIdOrName, @Param("name") String name,
      @Param("type") String type, @Param("qualifier") String qualifier);

  @RequestLine("PUT /zones/{zoneIdOrName}/recordsets?name={name}")
  @Headers("Content-Type: application/json")
  void putRecordSet(@Param("zoneIdOrName") String zoneIdOrName, ResourceRecordSet<?> update);

  @RequestLine("DELETE /zones/{zoneIdOrName}/recordsets?name={name}&type={type}")
  void deleteRecordSetByNameAndType(@Param("zoneIdOrName") String zoneIdOrName,
                                    @Param("name") String name,
                                    @Param("type") String type);

  @RequestLine("DELETE /zones/{zoneIdOrName}/recordsets?name={name}&type={type}&qualifier={qualifier}")
  void deleteRecordSetByNameTypeAndQualifier(@Param("zoneIdOrName") String zoneIdOrName,
                                             @Param("name") String name,
                                             @Param("type") String type,
                                             @Param("qualifier") String qualifier);
}
