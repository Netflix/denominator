package denominator.model;

import java.util.List;
import java.util.Map;

import denominator.model.profile.Geo;
import denominator.model.profile.Weighted;

/**
 * Capable of building record sets from rdata input types expressed as {@code E}
 *
 * @param <E> input type of rdata
 * @param <D> portable type of the rdata in the {@link ResourceRecordSet}
 */
abstract class AbstractRecordSetBuilder<E, D extends Map<String, Object>, B extends AbstractRecordSetBuilder<E, D, B>> {

  private String name;
  private String type;
  private String qualifier;
  private Integer ttl;
  private Geo geo;
  private Weighted weighted;

  /**
   * @see ResourceRecordSet#name()
   */
  @SuppressWarnings("unchecked")
  public B name(String name) {
    this.name = name;
    return (B) this;
  }

  /**
   * @see ResourceRecordSet#type()
   */
  @SuppressWarnings("unchecked")
  public B type(String type) {
    this.type = type;
    return (B) this;
  }

  /**
   * @see ResourceRecordSet#qualifier()
   */
  @SuppressWarnings("unchecked")
  public B qualifier(String qualifier) {
    this.qualifier = qualifier;
    return (B) this;
  }

  /**
   * @see ResourceRecordSet#ttl()
   */
  @SuppressWarnings("unchecked")
  public B ttl(Integer ttl) {
    this.ttl = ttl;
    return (B) this;
  }

  /**
   * @see ResourceRecordSet#geo()
   */
  @SuppressWarnings("unchecked")
  public B geo(Geo geo) {
    this.geo = geo;
    return (B) this;
  }

  /**
   * @see ResourceRecordSet#weighted()
   */
  @SuppressWarnings("unchecked")
  public B weighted(Weighted weighted) {
    this.weighted = weighted;
    return (B) this;
  }

  public ResourceRecordSet<D> build() {
    return new ResourceRecordSet<D>(name, type, qualifier, ttl, records(), geo, weighted);
  }

  /**
   * aggregate collected rdata values
   */
  protected abstract List<D> records();
}
