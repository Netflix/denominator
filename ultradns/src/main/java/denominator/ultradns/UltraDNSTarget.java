package denominator.ultradns;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import denominator.Credentials;
import denominator.Provider;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

import static denominator.common.Preconditions.checkNotNull;
import static feign.Util.UTF_8;
import static java.lang.String.format;

class UltraDNSTarget implements Target<UltraDNS> {

    static final String SOAP_TEMPLATE = "<?xml version=\"1.0\"?>\n"
            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
            + "  <soapenv:Header>\n"
            + "    <wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" soapenv:mustUnderstand=\"1\">\n"
            + "      <wsse:UsernameToken>\n"
            + "        <wsse:Username>%s</wsse:Username>\n"
            + "        <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">%s</wsse:Password>\n"
            + "      </wsse:UsernameToken>\n"
            + "    </wsse:Security>\n"
            + "  </soapenv:Header>\n"
            + "  <soapenv:Body>\n"
            + "    %s\n"
            + "  </soapenv:Body>\n"
            + "</soapenv:Envelope>";

  private final Provider provider;
  private final javax.inject.Provider<Credentials> credentials;

  @Inject
  UltraDNSTarget(Provider provider, javax.inject.Provider<Credentials> credentials) {
    this.provider = provider;
    this.credentials = credentials;
  }

  @Override
  public Class<UltraDNS> type() {
    return UltraDNS.class;
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
    if (creds instanceof  List) {
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
      throw new IllegalArgumentException("Unsupported credential type: "+ creds);
    }

    in.insert(0, url());
    in.body(format(SOAP_TEMPLATE, username, password, new String(in.body(), UTF_8)));
    in.header("Host", URI.create(in.url()).getHost());
    return in.request();
  }
}
