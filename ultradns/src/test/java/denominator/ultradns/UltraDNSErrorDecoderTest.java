package denominator.ultradns;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collection;
import java.util.Collections;

import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

import static denominator.ultradns.UltraDNSTest.dirPoolNotFound;
import static denominator.ultradns.UltraDNSTest.dirRecordNotFound;
import static denominator.ultradns.UltraDNSTest.directionalNotEnabled;
import static denominator.ultradns.UltraDNSTest.groupNotFound;
import static denominator.ultradns.UltraDNSTest.invalidUser;
import static denominator.ultradns.UltraDNSTest.poolAlreadyExists;
import static denominator.ultradns.UltraDNSTest.poolNotFound;
import static denominator.ultradns.UltraDNSTest.recordNotFound;
import static denominator.ultradns.UltraDNSTest.rrAlreadyExists;
import static denominator.ultradns.UltraDNSTest.systemError;
import static feign.Util.UTF_8;

public class UltraDNSErrorDecoderTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  ErrorDecoder errors = new UltraDNSErrorDecoder(UltraDNSProvider.FeignModule.decoder());

  static Response errorResponse(String body) {
    return Response.create(500, "Server Error", Collections.<String, Collection<String>>emptyMap(),
                           body, UTF_8);
  }

  @Test
  public void noBody() throws Exception {
    thrown.expect(FeignException.class);
    thrown.expectMessage("status 500 reading UltraDNS#accountId()");

    throw errors.decode("UltraDNS#accountId()", errorResponse(null));
  }

  @Test
  public void systemError() throws Exception {
    thrown.expect(RetryableException.class);
    thrown.expectMessage("UltraDNS#networkStatus() failed with error 9999: System Error");

    throw errors.decode("UltraDNS#networkStatus()", errorResponse(systemError));
  }

  @Test
  public void invalidUser() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage("UltraDNS#networkStatus() failed: Invalid User");

    throw errors.decode("UltraDNS#networkStatus()", errorResponse(invalidUser));
  }

  @Test
  public void code2103() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage(
        "UltraDNS#deleteRRPool(String) failed with error 2103: No resource record with GUID found in the system AAAAAAAAAAAAAAAA");

    throw errors.decode("UltraDNS#deleteRRPool(String)", errorResponse(recordNotFound));
  }

  @Test
  public void code2111() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage(
        "UltraDNS#updateRecordInZone(Record,String) failed with error 2111: Resource Record of type 1 with these attributes already exists in the system.");

    throw errors
        .decode("UltraDNS#updateRecordInZone(Record,String)", errorResponse(rrAlreadyExists));
  }

  @Test
  public void code2911() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage(
        "UltraDNS#deleteRRPool(String) failed with error 2911: Pool does not exist in the system");

    throw errors.decode("UltraDNS#deleteRRPool(String)", errorResponse(poolNotFound));
  }

  @Test
  public void code2142() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage(
        "UltraDNS#directionalRecordsInZoneByNameAndType(String,String,int) failed with error 2142: No Pool or Multiple pools of same type exists for the PoolName : www.denominator.io.");

    throw errors.decode("UltraDNS#directionalRecordsInZoneByNameAndType(String,String,int)",
                        errorResponse(dirPoolNotFound));
  }

  @Test
  public void code2912() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage(
        "UltraDNS#createDirectionalPoolInZoneForNameAndType(String,String,String) failed with error 2912: Pool already created for this host name : www.denominator.io.");

    throw errors.decode("UltraDNS#createDirectionalPoolInZoneForNameAndType(String,String,String)",
                        errorResponse(poolAlreadyExists));
  }

  @Test
  public void code4003() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage(
        "UltraDNS#directionalRecordsInZoneAndGroupByNameAndType(String,String,String,int) failed with error 4003: Group does not exist.");

    throw errors.decode(
        "UltraDNS#directionalRecordsInZoneAndGroupByNameAndType(String,String,String,int)",
        errorResponse(groupNotFound));
  }

  @Test
  public void code4006() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage(
        "UltraDNS#directionalRecordsInZoneAndGroupByNameAndType(String,String,String,int) failed with error 4006: Directional feature not Enabled or Directional migration is not done.");

    throw errors.decode(
        "UltraDNS#directionalRecordsInZoneAndGroupByNameAndType(String,String,String,int)",
        errorResponse(directionalNotEnabled));
  }

  @Test
  public void code2705() throws Exception {
    thrown.expect(UltraDNSException.class);
    thrown.expectMessage(
        "UltraDNS#deleteDirectionalRecord(String) failed with error 2705: Directional Pool Record does not exist in the system");

    throw errors
        .decode("UltraDNS#deleteDirectionalRecord(String)", errorResponse(dirRecordNotFound));
  }
}
