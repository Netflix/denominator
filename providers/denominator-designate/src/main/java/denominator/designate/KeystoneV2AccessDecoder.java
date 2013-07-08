package denominator.designate;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Reader;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import denominator.designate.OpenStackApis.TokenIdAndPublicURL;
import feign.codec.Decoder;

class KeystoneV2AccessDecoder extends Decoder {
    private final String serviceTypeSuffix;

    @Inject
    KeystoneV2AccessDecoder(@Named("serviceTypeSuffix") String serviceTypeSuffix) {
        this.serviceTypeSuffix = checkNotNull(serviceTypeSuffix, "serviceTypeSuffix was null");
    }

    @Override
    public TokenIdAndPublicURL decode(String methodKey, Reader reader, Type ignored) throws Throwable {
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
            if (!isNull(typeField) && !isNull(endpointsField) && typeField.getAsString().endsWith(serviceTypeSuffix)) {
                for (JsonElement e : endpointsField.getAsJsonArray()) {
                    JsonObject endpoint = e.getAsJsonObject();
                    tokenUrl.publicURL = endpoint.get("publicURL").getAsString();
                    if (tokenUrl.publicURL.endsWith("/"))
                        tokenUrl.publicURL = tokenUrl.publicURL.substring(0, tokenUrl.publicURL.length() - 1);
                }
            }
        }
        return tokenUrl;
    }

    @Override
    public String toString() {
        return "KeystoneV2AccessDecoder(" + serviceTypeSuffix + ")";
    }

    static boolean isNull(JsonElement element) {
        return element == null || element.isJsonNull();
    }
};
