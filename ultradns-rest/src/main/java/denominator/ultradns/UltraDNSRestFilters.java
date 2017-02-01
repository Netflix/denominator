package denominator.ultradns;

import java.io.Serializable;

import denominator.common.Filter;
import denominator.ultradns.model.Record;

import static denominator.common.Preconditions.checkNotNull;

final class UltraDNSRestFilters {

  private UltraDNSRestFilters() {
  }

  public static Filter<Record> resourceTypeEqualTo(int typeValue) {
    return new ResourceTypeEqualToPredicate(typeValue);
  }

  public static Filter<Record> recordIdEqualTo(String id) {
    return new RecordIdEqualToPredicate(id);
  }

  /**
   * @see UltraDNSRestFilters#resourceTypeEqualTo(int)
   */
  private static class ResourceTypeEqualToPredicate implements Filter<Record>, Serializable {

    private static final long serialVersionUID = 0;
    private final int typeValue;

    private ResourceTypeEqualToPredicate(int typeValue) {
      this.typeValue = checkNotNull(typeValue, "typeValue");
    }

    @Override
    public boolean apply(Record in) {
      return typeValue == in.typeCode;
    }

    @Override
    public String toString() {
      return "ResourceTypeEqualTo(" + typeValue + ")";
    }
  }

  private static class RecordIdEqualToPredicate implements Filter<Record>, Serializable {

    private static final long serialVersionUID = 0;
    private final String id;

    private RecordIdEqualToPredicate(String id) {
      this.id = checkNotNull(id, "id");
    }

    @Override
    public boolean apply(Record in) {
      return id.equals(in.id);
    }

    @Override
    public String toString() {
      return "RecordIdEqualTo(" + id + ")";
    }
  }
}
