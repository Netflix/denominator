package denominator.route53;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Provider;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.MapCredentials;
import denominator.hook.InstanceMetadataHook;

import static denominator.CredentialsConfiguration.checkValidForProvider;
import static denominator.common.Preconditions.checkNotNull;
import static java.util.regex.Pattern.DOTALL;

/**
 * Credentials supplier implementation that loads credentials from the Amazon EC2 Instance Metadata
 * Service.
 */
@Module(injects = InstanceProfileCredentialsProvider.class, complete = false, library = true)
public class InstanceProfileCredentialsProvider {

  private static final Map<String, String> keyMap = new LinkedHashMap<String, String>();

  static {
    keyMap.put("AccessKeyId", "accessKey");
    keyMap.put("SecretAccessKey", "secretKey");
    keyMap.put("Token", "sessionToken");
  }

  private static final Pattern JSON_FIELDS = Pattern.compile("\"([^\"]+)\" *: *\"([^\"]+)", DOTALL);
  private final Provider<String> iipJsonProvider;

  public InstanceProfileCredentialsProvider() {
    this(new ReadFirstInstanceProfileCredentialsOrNull());
  }

  public InstanceProfileCredentialsProvider(URI baseUri) {
    this(new ReadFirstInstanceProfileCredentialsOrNull(baseUri));
  }

  public InstanceProfileCredentialsProvider(Provider<String> iipJsonProvider) {
    this.iipJsonProvider = checkNotNull(iipJsonProvider, "iipJsonProvider");
  }

  /**
   * IAM Instance Profile format is simple, non-nested json.
   *
   * ex.
   *
   * <pre>
   * {
   *   "Code" : "Success",
   *   "LastUpdated" : "2013-02-26T02:03:57Z",
   *   "Type" : "AWS-HMAC",
   *   "AccessKeyId" : "AAAAA",
   *   "SecretAccessKey" : "SSSSSSS",
   *   "Token" : "TTTTTTT",
   *   "Expiration" : "2013-02-26T08:12:23Z"
   * }
   *
   * </pre>
   *
   * This impl avoids choosing a json library by parsing the simple structure above directly.
   */
  static Map<String, String> parseJson(String in) {
    if (in == null) {
      return Collections.emptyMap();
    }
    String noBraces = in.replace('{', ' ').replace('}', ' ').trim();
    Map<String, String> builder = new LinkedHashMap<String, String>();
    Matcher matcher = JSON_FIELDS.matcher(noBraces);
    while (matcher.find()) {
      String key = keyMap.get(matcher.group(1));
      if (key != null) {
        builder.put(key, matcher.group(2));
      }
    }
    return builder;
  }

  @Provides
  Credentials get(denominator.Provider provider) {
    return checkValidForProvider(MapCredentials.from(parseJson(iipJsonProvider.get())), provider);
  }

  @Override
  public String toString() {
    return "ParseIIPJsonFrom(" + iipJsonProvider + ")";
  }

  /**
   * default means to grab instance credentials, or return null
   */
  static class ReadFirstInstanceProfileCredentialsOrNull implements Provider<String> {

    private final URI baseUri;

    public ReadFirstInstanceProfileCredentialsOrNull() {
      this(URI.create("http://169.254.169.254/latest/meta-data/"));
    }

    /**
     * @param baseUri uri string with trailing slash
     */
    public ReadFirstInstanceProfileCredentialsOrNull(URI baseUri) {
      this.baseUri = checkNotNull(baseUri, "baseUri");
    }

    @Override
    public String get() {
      List<String> roles = InstanceMetadataHook.list(baseUri, "iam/security-credentials/");
      if (roles.isEmpty()) {
        return null;
      }
      return InstanceMetadataHook.get(baseUri, "iam/security-credentials/" + roles.get(0));
    }

    @Override
    public String toString() {
      return "ReadFirstInstanceProfileCredentialsOrNull(" + baseUri + ")";
    }
  }
}
