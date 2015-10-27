package denominator.verisigndns;

import static denominator.assertj.MockWebServerAssertions.assertThat;
import static denominator.verisigndns.VerisignDnsTarget.SOAP_TEMPLATE;
import static java.lang.String.format;

import java.io.IOException;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import denominator.Credentials;
import denominator.CredentialsConfiguration;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.Credentials.ListCredentials;
import denominator.assertj.RecordedRequestAssert;

public class MockVerisignDnsServer extends VerisignDnsProvider implements TestRule {

  private final MockWebServer delegate = new MockWebServer();
  private String username;
  private String password;
  private String soapTemplate;

  MockVerisignDnsServer() {
    credentials("testuser", "password");
  }

  @Override
  public String url() {
    return "http://localhost:" + delegate.getPort();
  }

  DNSApiManager connect() {
    return Denominator.create(this, CredentialsConfiguration.credentials(credentials()));
  }

  Credentials credentials() {
    return ListCredentials.from(username, password);
  }

  MockVerisignDnsServer credentials(String username, String password) {
    this.username = username;
    this.password = password;
    this.soapTemplate = format(SOAP_TEMPLATE, System.currentTimeMillis(), username, password, "%s");
    return this;
  }

  void enqueue(MockResponse mockResponse) {
    delegate.enqueue(mockResponse);
  }

  void enqueueError(String code, String description) {
    delegate.enqueue(new MockResponse().setResponseCode(500).setBody(
        format(FAULT_TEMPLATE, description, code, description)));
  }
  
  RecordedRequestAssert assertRequest() throws InterruptedException {
    return assertThat(delegate.takeRequest());
  }

  RecordedRequestAssert assertSoapBody(String soapBody) throws InterruptedException {
    return assertThat(delegate.takeRequest()).hasMethod("POST").hasPath("/")
        .hasBody(format(soapTemplate, soapBody));
  }

  void shutdown() throws IOException {
    delegate.shutdown();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return delegate.apply(base, description);
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false,
      includes = VerisignDnsProvider.Module.class)
  static final class Module {

  }
  
  static final String FAULT_TEMPLATE = 
      "<ns3:Fault xmlns:ns2=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns3=\"http://www.w3.org/2003/05/soap-envelope\">"
      + "   <ns3:Code>"
      + "       <ns3:Value>ns3:Receiver</ns3:Value>"
      + "   </ns3:Code>"
      + "   <ns3:Reason>"
      + "       <ns3:Text xml:lang=\"en\">%s</ns3:Text>"
      + "   </ns3:Reason>"
      + "   <ns3:Detail>"
      + "       <ns3:dnsaWSRes xmlns=\"urn:com:verisign:dnsa:messaging:schema:1\" xmlns:ns2=\"urn:com:verisign:dnsa:auth:schema:1\" xmlns:ns3=\"urn:com:verisign:dnsa:api:schema:2\" xmlns:ns4=\"urn:com:verisign:dnsa:api:schema:1\">"
      + "              <ns3:callSuccess>false</ns3:callSuccess>"
      + "              <ns3:reason code=\"%s\" description=\"%s\"/>"
      + "       </ns3:dnsaWSRes>"
      + "   </ns3:Detail>"
      + "</ns3:Fault>";
}
