package denominator.clouddns;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;

import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.CharStreams;
import com.google.common.reflect.TypeToken;

import denominator.clouddns.RackspaceApis.TokenIdAndPublicURL;
import feign.codec.Decoder;

/**
 * Using regex as the form of the json is sparse and would otherwise require
 * fully mapping the object or ~100lines of parser code.
 */
class KeystoneAccessDecoder extends Decoder {
    private final Pattern pattern;
    private final String type;

    /**
     * @param type
     *            ex {@code rax:dns}
     */
    KeystoneAccessDecoder(String type) {
        this.pattern = compile("^.*token[\":{\\s]+id\":\"([^\"]+)\"(.*" + type
                + "\"[^\\]]+publicURL\":\\s*\"([^\"]+)\")?", DOTALL);
        this.type = type;
    }

    @Override
    public TokenIdAndPublicURL decode(String methodKey, Reader reader, TypeToken<?> type) throws Throwable {
        Matcher matcher = pattern.matcher(CharStreams.toString(reader));
        if (matcher.find()) {
            TokenIdAndPublicURL record = new TokenIdAndPublicURL();
            record.tokenId = matcher.group(1);
            record.publicURL = matcher.group(3).replace("\\", "");
            return record;
        }
        return null;
    }

    @Override
    public String toString() {
        return "KeystoneAccessDecoder(" + type + ")";
    }
};