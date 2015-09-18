package denominator.ultradns;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;

import denominator.Credentials;
import denominator.CredentialsConfiguration;
import denominator.DNSApiManager;
import denominator.Denominator;
import denominator.assertj.RecordedRequestAssert;

import static denominator.Credentials.ListCredentials;
import static denominator.assertj.MockWebServerAssertions.assertThat;
import static denominator.ultradns.UltraDNSTarget.SOAP_TEMPLATE;
import static java.lang.String.format;

final class MockUltraDNSServer extends UltraDNSProvider implements TestRule {

  /**
   * param 1 is the code, 2 is the description
   */
  static final String FAULT_TEMPLATE =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "    <soap:Body>\n"
      + "            <soap:Fault>\n"
      + "                    <faultcode>soap:Server</faultcode>\n"
      + "                    <faultstring>Fault occurred while processing.</faultstring>\n"
      + "                    <detail>\n"
      + "                            <ns1:UltraWSException xmlns:ns1=\"http://webservice.api.ultra.neustar.com/v01/\">\n"
      + "                                    <errorCode xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:int\">%s</errorCode>\n"
      + "                                    <errorDescription xmlns:ns2=\"http://schema.ultraservice.neustar.com/v01/\">%s</errorDescription>\n"
      + "                            </ns1:UltraWSException>\n"
      + "                    </detail>\n"
      + "            </soap:Fault>\n"
      + "    </soap:Body>\n"
      + "</soap:Envelope>";

  private final MockWebServer delegate = new MockWebServer();
  private String username;
  private String password;
  private String soapTemplate;

  MockUltraDNSServer() {
    credentials("joe", "letmein");
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

  MockUltraDNSServer credentials(String username, String password) {
    this.username = username;
    this.password = password;
    this.soapTemplate = format(SOAP_TEMPLATE, username, password, "%s");
    return this;
  }

  void enqueueError(int code, String description) {
    delegate.enqueue(
        new MockResponse().setResponseCode(500).setBody(format(FAULT_TEMPLATE, code, description)));
  }

  void enqueue(MockResponse mockResponse) {
    delegate.enqueue(mockResponse);
  }

  RecordedRequestAssert assertSoapBody(String soapBody) throws InterruptedException {
    return assertThat(delegate.takeRequest())
        .hasMethod("POST")
        .hasPath("/")
        .hasXMLBody(format(soapTemplate, soapBody));
  }

  void shutdown() throws IOException {
    delegate.shutdown();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return delegate.apply(base, description);
  }

  @dagger.Module(injects = DNSApiManager.class, complete = false, includes =
      UltraDNSProvider.Module.class)
  static final class Module {

  }
}
