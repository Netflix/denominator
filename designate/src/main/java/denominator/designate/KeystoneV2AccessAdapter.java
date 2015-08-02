package denominator.designate;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import denominator.designate.KeystoneV2.TokenIdAndPublicURL;

class KeystoneV2AccessAdapter extends TypeAdapter<TokenIdAndPublicURL> {

  private final String serviceTypeSuffix = ":dns";

  static boolean isNull(JsonElement element) {
    return element == null || element.isJsonNull();
  }

  @Override
  public TokenIdAndPublicURL read(JsonReader in) throws IOException {
    JsonObject access;
    try {
      access = new JsonParser().parse(in).getAsJsonObject().get("access").getAsJsonObject();
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
      if (!isNull(typeField) && !isNull(endpointsField) && typeField.getAsString()
          .endsWith(serviceTypeSuffix)) {
        for (JsonElement e : endpointsField.getAsJsonArray()) {
          JsonObject endpoint = e.getAsJsonObject();
          tokenUrl.publicURL = endpoint.get("publicURL").getAsString();
          if (tokenUrl.publicURL.endsWith("/")) {
            tokenUrl.publicURL = tokenUrl.publicURL.substring(0, tokenUrl.publicURL.length() - 1);
          }
        }
      }
    }
    return tokenUrl;
  }

  @Override
  public String toString() {
    return "KeystoneV2AccessAdapter(" + serviceTypeSuffix + ")";
  }

  @Override
  public void write(JsonWriter out, TokenIdAndPublicURL value) throws IOException {
    throw new UnsupportedOperationException();
  }
};
