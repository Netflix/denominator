package denominator.ultradns;

import static denominator.ultradns.UltraDNSTest.dirPoolNotFound;
import static denominator.ultradns.UltraDNSTest.dirRecordNotFound;
import static denominator.ultradns.UltraDNSTest.groupNotFound;
import static denominator.ultradns.UltraDNSTest.invalidUser;
import static denominator.ultradns.UltraDNSTest.poolAlreadyExists;
import static denominator.ultradns.UltraDNSTest.poolNotFound;
import static denominator.ultradns.UltraDNSTest.recordNotFound;
import static denominator.ultradns.UltraDNSTest.rrAlreadyExists;
import static denominator.ultradns.UltraDNSTest.systemError;

import java.util.Collection;

import javax.inject.Provider;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

import denominator.ultradns.UltraDNSErrorDecoder.UltraDNSError;
import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

@Test(singleThreaded = true)
public class UltraDNSErrorDecoderTest {

    ErrorDecoder errorDecoder = new UltraDNSErrorDecoder(new Provider<UltraDNSErrorDecoder.UltraDNSError>() {
        public UltraDNSError get() {
            return new UltraDNSError();
        }
    });

    @Test(expectedExceptions = FeignException.class, expectedExceptionsMessageRegExp = "status 500 reading UltraDNS.accountId\\(\\)")
    public void noBody() throws Throwable {
        Response response = errorResponse(null);
        throw errorDecoder.decode("UltraDNS#accountId()", response);
    }

    @Test(expectedExceptions = RetryableException.class, expectedExceptionsMessageRegExp = "UltraDNS#networkStatus\\(\\) failed with error 9999: System Error")
    public void systemError() throws Throwable {
        throw errorDecoder.decode("UltraDNS#networkStatus()", errorResponse(systemError));
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#networkStatus\\(\\) failed: Invalid User")
    public void invalidUser() throws Throwable {
        throw errorDecoder.decode("UltraDNS#networkStatus()", errorResponse(invalidUser));
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#deleteRRPool\\(String\\) failed with error 2103: No resource record with GUID found in the system AAAAAAAAAAAAAAAA")
    public void code2103() throws Throwable {
        throw errorDecoder.decode("UltraDNS#deleteRRPool(String)", errorResponse(recordNotFound));
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#updateRecordInZone\\(Record,String\\) failed with error 2111: Resource Record of type 1 with these attributes already exists in the system.")
    public void code2111() throws Throwable {
        throw errorDecoder.decode("UltraDNS#updateRecordInZone(Record,String)", errorResponse(rrAlreadyExists));
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#deleteRRPool\\(String\\) failed with error 2911: Pool does not exist in the system")
    public void code2911() throws Throwable {
        throw errorDecoder.decode("UltraDNS#deleteRRPool(String)", errorResponse(poolNotFound));
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#directionalRecordsInZoneByNameAndType\\(String,String,int\\) failed with error 2142: No Pool or Multiple pools of same type exists for the PoolName : www.denominator.io.")
    public void code2142() throws Throwable {
        throw errorDecoder.decode("UltraDNS#directionalRecordsInZoneByNameAndType(String,String,int)",
                errorResponse(dirPoolNotFound));
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#createDirectionalPoolInZoneForNameAndType\\(String,String,String\\) failed with error 2912: Pool already created for this host name : www.denominator.io.")
    public void code2912() throws Throwable {
        throw errorDecoder.decode("UltraDNS#createDirectionalPoolInZoneForNameAndType(String,String,String)",
                errorResponse(poolAlreadyExists));
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#directionalRecordsInZoneAndGroupByNameAndType\\(String,String,String,int\\) failed with error 4003: Group does not exist.")
    public void code4003() throws Throwable {
        throw errorDecoder.decode("UltraDNS#directionalRecordsInZoneAndGroupByNameAndType(String,String,String,int)",
                errorResponse(groupNotFound));
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS#deleteDirectionalRecord\\(String\\) failed with error 2705: Directional Pool Record does not exist in the system")
    public void code2705() throws Throwable {
        throw errorDecoder.decode("UltraDNS#deleteDirectionalRecord(String)", errorResponse(dirRecordNotFound));
    }

    static Response errorResponse(String body) {
        return Response.create(500, "Server Error", ImmutableMap.<String, Collection<String>> of(), body);
    }
}
