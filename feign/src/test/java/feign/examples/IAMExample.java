package feign.examples;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.hash.Hashing.sha256;
import static com.google.common.io.BaseEncoding.base16;
import static com.google.common.net.HttpHeaders.HOST;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import dagger.Module;
import dagger.Provides;
import feign.Feign;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.Decoders;

public class IAMExample {

    interface IAM {
        @GET
        @Path("/?Action=GetUser&Version=2010-05-08")
        String arn();
    }

    public static void main(String... args) {

        IAM iam = Feign.create(new IAMTarget(args[0], args[1]), new IAMModule());
        System.out.println(iam.arn());
    }

    static class IAMTarget extends AWSSignatureVersion4 implements Target<IAM> {

        @Override
        public Class<IAM> type() {
            return IAM.class;
        }

        @Override
        public String name() {
            return "iam";
        }

        @Override
        public String url() {
            return "https://iam.amazonaws.com";
        }

        private IAMTarget(String accessKey, String secretKey) {
            super(accessKey, secretKey);
        }

        @Override
        public Request apply(RequestTemplate in) {
            in.insert(0, url());
            return super.apply(in);
        }
    }

    @Module(overrides = true, library = true)
    static class IAMModule {
        @Provides
        @Singleton
        Map<String, Decoder> decoders() {
            return ImmutableMap.of("IAM", Decoders.firstGroup("<Arn>([\\S&&[^<]]+)</Arn>"));
        }
    }

    // http://docs.aws.amazon.com/general/latest/gr/signature-version-4.html
    static class AWSSignatureVersion4 implements Function<RequestTemplate, Request> {

        String region = "us-east-1";
        String service = "iam";
        String accessKey;
        String secretKey;

        public AWSSignatureVersion4(String accessKey, String secretKey) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
        }

        @Override
        public Request apply(RequestTemplate input) {
            input.header(HOST, URI.create(input.url()).getHost());
            Multimap<String, String> sortedLowercaseHeaders = TreeMultimap.create();
            for (String key : input.headers().keySet()) {
                sortedLowercaseHeaders.putAll(trimToLowercase.apply(key),
                        transform(input.headers().get(key), trimToLowercase));
            }

            String timestamp = iso8601.format(new Date());
            String credentialScope = Joiner.on('/').join(timestamp.substring(0, 8), region, service, "aws4_request");

            input.query("X-Amz-Algorithm", "AWS4-HMAC-SHA256");
            input.query("X-Amz-Credential", accessKey + "/" + credentialScope);
            input.query("X-Amz-Date", timestamp);
            input.query("X-Amz-SignedHeaders", Joiner.on(';').join(sortedLowercaseHeaders.keySet()));

            String canonicalString = canonicalString(input, sortedLowercaseHeaders);
            String toSign = toSign(timestamp, credentialScope, canonicalString);

            byte[] signatureKey = signatureKey(secretKey, timestamp);
            String signature = base16().lowerCase().encode(hmacSHA256(toSign, signatureKey));

            input.query("X-Amz-Signature", signature);

            return input.request();
        }

        byte[] signatureKey(String secretKey, String timestamp) {
            byte[] kSecret = ("AWS4" + secretKey).getBytes(UTF_8);
            byte[] kDate = hmacSHA256(timestamp.substring(0, 8), kSecret);
            byte[] kRegion = hmacSHA256(region, kDate);
            byte[] kService = hmacSHA256(service, kRegion);
            byte[] kSigning = hmacSHA256("aws4_request", kService);
            return kSigning;
        }

        static byte[] hmacSHA256(String data, byte[] key) {
            try {
                String algorithm = "HmacSHA256";
                Mac mac = Mac.getInstance(algorithm);
                mac.init(new SecretKeySpec(key, algorithm));
                return mac.doFinal(data.getBytes(UTF_8));
            } catch (Exception e) {
                throw propagate(e);
            }
        }

        private static final String EMPTY_STRING_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        private String canonicalString(RequestTemplate input, Multimap<String, String> sortedLowercaseHeaders) {
            StringBuilder canonicalRequest = new StringBuilder();
            // HTTPRequestMethod + '\n' +
            canonicalRequest.append(input.method()).append('\n');

            // CanonicalURI + '\n' +
            canonicalRequest.append(URI.create(input.url()).getPath()).append('\n');

            // CanonicalQueryString + '\n' +
            canonicalRequest.append(input.queryLine().substring(1));
            canonicalRequest.append('\n');

            // CanonicalHeaders + '\n' +
            for (Entry<String, Collection<String>> entry : sortedLowercaseHeaders.asMap().entrySet()) {
                canonicalRequest.append(entry.getKey()).append(':').append(Joiner.on(',').join(entry.getValue()))
                        .append('\n');
            }
            canonicalRequest.append('\n');

            // SignedHeaders + '\n' +
            canonicalRequest.append(Joiner.on(',').join(sortedLowercaseHeaders.keySet())).append('\n');

            // HexEncode(Hash(Payload))
            if (input.body().isPresent()) {
                canonicalRequest.append(base16().lowerCase().encode(
                        sha256().hashString(input.body().or(""), UTF_8).asBytes()));
            } else {
                canonicalRequest.append(EMPTY_STRING_HASH);
            }
            return canonicalRequest.toString();
        }

        private static final Function<String, String> trimToLowercase = new Function<String, String>() {
            public String apply(String in) {
                return in.toLowerCase().trim();
            }
        };

        private String toSign(String timestamp, String credentialScope, String canonicalRequest) {
            StringBuilder toSign = new StringBuilder();
            // Algorithm + '\n' +
            toSign.append("AWS4-HMAC-SHA256").append('\n');
            // RequestDate + '\n' +
            toSign.append(timestamp).append('\n');
            // CredentialScope + '\n' +
            toSign.append(credentialScope).append('\n');
            // HexEncode(Hash(CanonicalRequest))
            toSign.append(base16().lowerCase().encode(sha256().hashString(canonicalRequest, UTF_8).asBytes()));
            return toSign.toString();
        }

        private static final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

        static {
            iso8601.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
    }
}
