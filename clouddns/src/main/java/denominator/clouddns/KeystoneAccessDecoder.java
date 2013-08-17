package denominator.clouddns;

import static denominator.common.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import denominator.clouddns.RackspaceApis.TokenIdAndPublicURL;
import feign.codec.Decoder;

class KeystoneAccessDecoder implements Decoder.TextStream<TokenIdAndPublicURL> {
    // rax:dns
    private final String type;

    KeystoneAccessDecoder(String type) {
        this.type = checkNotNull(type, "type was null");
    }

    @Override
    public TokenIdAndPublicURL decode(Reader reader, Type ignored) throws IOException {
        JsonObject access = null;
        try {
            access = new JsonParser().parse(reader).getAsJsonObject().get("access").getAsJsonObject();
        } catch (JsonIOException e) {
            if (e.getCause() != null && e.getCause() instanceof IOException) {
                throw IOException.class.cast(e.getCause());
            }
            throw e;
        }
        JsonElement tokenField = access.get("token");
        if (isNull(tokenField)) {
            return null;
        }
        JsonElement idField = tokenField.getAsJsonObject().get("id");
        if (isNull(idField)) {
            return null;
        }

        TokenIdAndPublicURL tokenUrl = new TokenIdAndPublicURL();
        tokenUrl.tokenId = idField.getAsString();

        for (JsonElement s : access.get("serviceCatalog").getAsJsonArray()) {
            JsonObject service = s.getAsJsonObject();
            JsonElement typeField = service.get("type");
            JsonElement endpointsField = service.get("endpoints");
            if (!isNull(typeField) && !isNull(endpointsField) && type.equals(typeField.getAsString())) {
                for (JsonElement e : endpointsField.getAsJsonArray()) {
                    JsonObject endpoint = e.getAsJsonObject();
                    tokenUrl.publicURL = endpoint.get("publicURL").getAsString();
                }
            }
        }
        return tokenUrl;
    }

    @Override
    public String toString() {
        return "KeystoneAccessDecoder(" + type + ")";
    }

    static boolean isNull(JsonElement element) {
        return element == null || element.isJsonNull();
    }
};