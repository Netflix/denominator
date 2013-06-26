package denominator.ultradns;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.HOST;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;

import denominator.Credentials;
import denominator.Credentials.ListCredentials;
import denominator.Provider;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;

class UltraDNSTarget implements Target<UltraDNS> {

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
        in.insert(0, url());
        List<Object> creds = ListCredentials.asList(credentials.get());
        in.body(format(SOAP_TEMPLATE, creds.get(0).toString(), creds.get(1).toString(), in.body().get()));
        in.header(HOST, URI.create(in.url()).getHost());
        in.header(CONTENT_TYPE, APPLICATION_XML);
        return in.request();
    }

    static final String SOAP_TEMPLATE = "" //
            + "<?xml version=\"1.0\"?>\n"//
            + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v01=\"http://webservice.api.ultra.neustar.com/v01/\">\n"//
            + "  <soapenv:Header>\n"//
            + "    <wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" soapenv:mustUnderstand=\"1\">\n"//
            + "      <wsse:UsernameToken>\n"//
            + "        <wsse:Username>%s</wsse:Username>\n"//
            + "        <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">%s</wsse:Password>\n"//
            + "      </wsse:UsernameToken>\n"//
            + "    </wsse:Security>\n"//
            + "  </soapenv:Header>\n"//
            + "  <soapenv:Body>\n"//
            + "    %s\n"//
            + "  </soapenv:Body>\n"//
            + "</soapenv:Envelope>";
}
