package denominator.verisigndns;

import static denominator.common.Preconditions.checkNotNull;
import static feign.Util.UTF_8;
import static java.lang.String.format;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import denominator.Credentials;
import denominator.Provider;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

final class VerisignDnsTarget implements Target<VerisignDns> {
  static final AtomicInteger MSGID = new AtomicInteger();
  // formatter:off
  static final String SOAP_TEMPLATE =
          "<soap:Envelope"
          + " xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\""
          + " xmlns:urn=\"urn:com:verisign:dnsa:messaging:schema:1\""
          + " xmlns:urn1=\"urn:com:verisign:dnsa:auth:schema:1\""
          + " xmlns:api1=\"urn:com:verisign:dnsa:api:schema:1\""
          + " xmlns:api2=\"urn:com:verisign:dnsa:api:schema:2\">\n"
          + "   <soap:Header>\n"
          + "      <urn:reliableMessageReq>\n"
          + "         <urn:MessageId>%d</urn:MessageId>\n"
          + "         <urn:DuplicateElimination/>\n"
          + "      </urn:reliableMessageReq>\n"
          + "      <urn1:authInfo>\n"
          + "         <urn1:userToken>\n"
          + "            <urn1:userName>%s</urn1:userName>\n"
          + "            <urn1:password>%s</urn1:password>\n"
          + "         </urn1:userToken>\n"
          + "      </urn1:authInfo>\n"
          + "   </soap:Header>\n"
          + "   <soap:Body>\n"
          + "      %s\n"
          + "   </soap:Body>\n"
          + "</soap:Envelope>";
  // formatter:on

  private final Provider provider;
  private final javax.inject.Provider<Credentials> credentials;

  @Inject
  VerisignDnsTarget(Provider provider, javax.inject.Provider<Credentials> credentials) {
    this.provider = provider;
    this.credentials = credentials;
  }

  @Override
  public Class<VerisignDns> type() {
    return VerisignDns.class;
  }

  @Override
  public String name() {
    return provider.name();
  }

  @Override
  public String url() {
    return provider.url();
  }

  @Override
  public Request apply(RequestTemplate in) {
    String username;
    String password;

    Credentials creds = credentials.get();
    if (creds instanceof List) {
      @SuppressWarnings("unchecked")
      List<Object> listCreds = (List<Object>) creds;
      username = listCreds.get(0).toString();
      password = listCreds.get(1).toString();
    } else if (creds instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> mapCreds = (Map<String, Object>) creds;
      username = checkNotNull(mapCreds.get("username"), "username").toString();
      password = checkNotNull(mapCreds.get("password"), "password").toString();
    } else {
      throw new IllegalArgumentException("Unsupported credential type: " + creds);
    }

    in.insert(0, url());
    in.body(xml(username, password, new String(in.body(), UTF_8)));
    in.header("Host", URI.create(in.url()).getHost());
    in.header("Content-Type", "application/soap+xml");
    return in.request();
  }

  private String xml(String username, String password, String body) {
    return format(SOAP_TEMPLATE, MSGID.getAndIncrement(), username, password, body);
  }
}
