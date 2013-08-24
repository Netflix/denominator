package denominator.denominatord;

import java.net.URI;
import java.util.List;

import denominator.common.Util;
import denominator.model.ResourceRecordSet;

import static denominator.common.Preconditions.checkArgument;

class Query {

  static Query from(String path) {
    String decoded = URI.create(path).getQuery();
    if (decoded == null) {
      return new Query(null, null, null);
    }
    String name = null;
    String type = null;
    String qualifier = null;
    for (String nameValueString : Util.split('&', decoded)) {
      List<String> nameValue = Util.split('=', nameValueString);
      String queryName = nameValue.get(0);
      String queryValue = nameValue.size() > 1 ? nameValue.get(1) : null;
      if (queryName.equals("name")) {
        name = queryValue;
      } else if (queryName.equals("type")) {
        type = queryValue;
      } else if (queryName.equals("qualifier")) {
        qualifier = queryValue;
      }
    }
    return new Query(name, type, qualifier);
  }

  static Query from(ResourceRecordSet<?> recordSet) {
    return new Query(recordSet.name(), recordSet.type(), recordSet.qualifier());
  }

  final String name;
  final String type;
  final String qualifier;

  private Query(String name, String type, String qualifier) {
    this.name = name;
    this.type = type;
    this.qualifier = qualifier;
    if (qualifier != null) {
      checkArgument(type != null && name != null, "name and type query required with qualifier");
    } else if (type != null) {
      checkArgument(name != null, "name query required with type");
    }
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append("name=").append(name)
        .append(", type=").append(type)
        .append(", qualifier=").append(qualifier).toString();
  }
}
