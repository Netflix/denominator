package denominator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import denominator.Credentials.MapCredentials;

import static denominator.CredentialsConfiguration.credentials;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

@RunWith(Live.class)
public class CheckConnectionLiveTest {

  @Parameterized.Parameter
  public DNSApiManager manager;

  @Test
  public void success() {
    assertTrue(manager.checkConnection());
  }

  @Test
  public void failGracefullyOnIncorrectCredentials() {
    assumeFalse("This test only applies to providers that authenticate",
                manager.provider().credentialTypeToParameterNames().isEmpty());

    Collection<String>
        parameters =
        manager.provider().credentialTypeToParameterNames().values().iterator().next();

    Map<String, String> creds = new LinkedHashMap<String, String>(parameters.size());
    for (String parameter : parameters) {
      creds.put(parameter, "foo");
    }

    DNSApiManager
        incorrectCredentials =
        Denominator.create(manager.provider(), credentials(MapCredentials.from(creds)));
    assertFalse(incorrectCredentials.checkConnection());
  }
}
