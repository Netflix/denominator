package denominator.designate;

import java.net.URI;

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

@Headers("Content-Type: application/json")
interface KeystoneV2 {

  @RequestLine("POST /tokens")
  @Body("%7B\"auth\":%7B\"passwordCredentials\":%7B\"username\":\"{username}\",\"password\":\"{password}\"%7D,\"tenantId\":\"{tenantId}\"%7D%7D")
  TokenIdAndPublicURL passwordAuth(URI endpoint, @Param("tenantId") String tenantId,
                                   @Param("username") String username,
                                   @Param("password") String password);

  static class TokenIdAndPublicURL {
    String tokenId;
    String publicURL;
  }
}
