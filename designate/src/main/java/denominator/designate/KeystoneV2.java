package denominator.designate;

import java.net.URI;

import javax.inject.Named;

import feign.Body;
import feign.Headers;
import feign.RequestLine;

interface KeystoneV2 {

  @RequestLine("POST /tokens")
  @Body("%7B\"auth\":%7B\"passwordCredentials\":%7B\"username\":\"{username}\",\"password\":\"{password}\"%7D,\"tenantId\":\"{tenantId}\"%7D%7D")
  @Headers("Content-Type: application/json")
  TokenIdAndPublicURL passwordAuth(URI endpoint, @Named("tenantId") String tenantId,
                                   @Named("username") String username,
                                   @Named("password") String password);

  static class TokenIdAndPublicURL {

    String tokenId;
    String publicURL;
  }
}
