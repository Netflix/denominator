package denominator.ultradns;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.io.Resources.getResource;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;

import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;

@Test(singleThreaded = true)
public class UltraDNSErrorDecoderTest {

    ErrorDecoder errorDecoder = new UltraDNSErrorDecoder();

    @Test(expectedExceptions = FeignException.class, expectedExceptionsMessageRegExp = "status 500 reading UltraDNS.accountId\\(\\)")
    public void noBody() throws Throwable {
        Response response = Response.create(INTERNAL_SERVER_ERROR.getStatusCode(), INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ImmutableListMultimap.<String, String> of(), null);
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed: Invalid User")
    public void invalidUser() throws Throwable {
        Response response = responseWithContent("errors/invalid_user.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 0")
    public void code0() throws Throwable {
        Response response = responseWithContent("errors/server_fault.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 0: Cannot find task with guid AAAAAAAAAAAAAAAA")
    public void code0ForDescriptionMatchingCannotFind() throws Throwable {
        Response response = responseWithContent("errors/task_doesnt_exist.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 2401: Account not found in the system. ID: AAAAAAAAAAAAAAAA")
    public void code2401() throws Throwable {
        Response response = responseWithContent("errors/account_doesnt_exist.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 1801: Zone does not exist in the system.")
    public void code1801() throws Throwable {
        Response response = responseWithContent("errors/zone_doesnt_exist.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 2103: No Resource Record with GUID found in the system AAAAAAAAAAAAAAAA")
    public void code2103() throws Throwable {
        Response response = responseWithContent("errors/rr_doesnt_exist.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 1802: Zone already exists in the system.")
    public void code1802() throws Throwable {
        Response response = responseWithContent("errors/zone_already_exists.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 2111: Resource Record of type 15 with these attributes already exists in the system.")
    public void code2111() throws Throwable {
        Response response = responseWithContent("errors/rr_already_exists.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 2911: Pool does not exist in the system")
    public void code2911() throws Throwable {
        Response response = responseWithContent("errors/lbpool_doesnt_exist.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 2142: No Pool or Multiple pools of same type exists for the PoolName : www.denominator.io.")
    public void code2142() throws Throwable {
        Response response = responseWithContent("errors/directionalpool_doesnt_exist.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 2912: Pool already created for this host name : www.denominator.io.")
    public void code2912() throws Throwable {
        Response response = responseWithContent("errors/lbpool_already_exists.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 3101: Pool Record does not exist.")
    public void code3101() throws Throwable {
        Response response = responseWithContent("errors/tcrecord_doesnt_exist.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 4003: Group does not exist.")
    public void code4003() throws Throwable {
        Response response = responseWithContent("errors/directionalgroup_doesnt_exist.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    @Test(expectedExceptions = UltraDNSException.class, expectedExceptionsMessageRegExp = "UltraDNS.accountId\\(\\) failed with error 2705: Directional Pool Record does not exist in the system")
    public void code2705() throws Throwable {
        Response response = responseWithContent("errors/directionalrecord_doesnt_exist.xml");
        errorDecoder.decode("UltraDNS.accountId()", response, stringToken);
    }

    static TypeToken<String> stringToken = TypeToken.of(String.class);

    static Response responseWithContent(String resourceName) {
        try {
            return Response.create(INTERNAL_SERVER_ERROR.getStatusCode(), INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    ImmutableListMultimap.<String, String> of(), Resources.toString(getResource(resourceName), UTF_8));
        } catch (Throwable e) {
            throw propagate(e);
        }
    }
}
