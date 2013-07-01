package denominator.clouddns;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Reader;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import denominator.clouddns.RackspaceApis.TokenIdAndPublicURL;
import feign.codec.Decoder;

class KeystoneAccessDecoder extends Decoder {
    // rax:dns
    private final String type;

    KeystoneAccessDecoder(String type) {
        this.type = checkNotNull(type, "type was null");
    }

    @Override
    public TokenIdAndPublicURL decode(String methodKey, Reader reader, TypeToken<?> ignored) throws Throwable {
        JsonObject access = new JsonParser().parse(reader).getAsJsonObject().get("access").getAsJsonObject();
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