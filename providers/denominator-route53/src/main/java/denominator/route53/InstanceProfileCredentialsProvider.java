package denominator.route53;

import static com.google.common.base.Preconditions.checkNotNull;
import static denominator.CredentialsConfiguration.checkValidForProvider;

import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Provider;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import dagger.Module;
import dagger.Provides;
import denominator.Credentials;
import denominator.Credentials.MapCredentials;
import denominator.hook.InstanceMetadataHook;

/**
 * Credentials supplier implementation that loads credentials from the Amazon
 * EC2 Instance Metadata Service.
 */
@Module(injects = InvalidatableAuthenticationHeadersSupplier.class, complete = false)
public class InstanceProfileCredentialsProvider {
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

    @Provides
    Credentials get(denominator.Provider provider) {
        return checkValidForProvider(MapCredentials.from(parseJson(iipJsonProvider.get())), provider);
    }

    @Override
    public String toString() {
        return "ParseIIPJsonFrom(" + iipJsonProvider + ")";
    }

    private static final Map<String, String> keyMap = ImmutableMap.of("AccessKeyId", "accessKey", "SecretAccessKey",
            "secretKey", "Token", "sessionToken");

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
     * This impl avoids choosing a json library by parsing the simple structure
     * above directly.
     * 
     */
    static Map<String, String> parseJson(String in) {
        if (in == null)
            return ImmutableMap.of();
        String noBraces = in.replace('{', ' ').replace('}', ' ').trim();
        Builder<String, String> builder = ImmutableMap.<String, String> builder();
        for (Entry<String, String> entry : Splitter.on(',').withKeyValueSeparator(" : ").split(noBraces).entrySet()) {
            String key = keyMap.get(entry.getKey().replace('"', ' ').trim());
            if (key != null)
                builder.put(key, entry.getValue().replace('"', ' ').trim());
        }
        return builder.build();
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
         * @param baseUri
         *            uri string with trailing slash
         */
        public ReadFirstInstanceProfileCredentialsOrNull(URI baseUri) {
            this.baseUri = checkNotNull(baseUri, "baseUri");
        }

        @Override
        public String get() {
            ImmutableList<String> roles = InstanceMetadataHook.list(baseUri, "iam/security-credentials/");
            if (roles.isEmpty())
                return null;
            return InstanceMetadataHook.get(baseUri, "iam/security-credentials/" + roles.get(0)).orNull();
        }

        @Override
        public String toString() {
            return "ReadFirstInstanceProfileCredentialsOrNull(" + baseUri + ")";
        }
    }
}