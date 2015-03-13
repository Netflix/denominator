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

import static denominator.ultradns.MockUltraDNSServer.FAULT_TEMPLATE;
import static denominator.ultradns.UltraDNSException.SYSTEM_ERROR;
import static feign.Util.UTF_8;
import static java.lang.String.format;

/**
 * Error decode tests not implicitly tested in {@linkplain denominator.ultradns.UltraDNSTest}.
 */
public class UltraDNSErrorDecoderTest {

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  ErrorDecoder errors = new UltraDNSErrorDecoder(UltraDNSProvider.FeignModule.decoder());

  static Response errorResponse(String body) {
    return Response
        .create(500, "Server Error", Collections.<String, Collection<String>>emptyMap(), body,
                UTF_8);
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
    throw errors.decode("UltraDNS#networkStatus()",
                        errorResponse(format(FAULT_TEMPLATE, SYSTEM_ERROR, "System Error")));
  }
}
