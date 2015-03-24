package denominator.route53;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import denominator.AllProfileResourceRecordSetApi;
import denominator.common.Filter;
import denominator.common.PeekingIterator;
import denominator.model.ResourceRecordSet;
import denominator.route53.Route53.ActionOnResourceRecordSet;
import denominator.route53.Route53.ResourceRecordSetList;
import denominator.route53.Route53.ResourceRecordSetList.NextRecord;

import static denominator.common.Util.filter;
import static denominator.common.Util.nextOrNull;
import static denominator.common.Util.peekingIterator;
import static denominator.model.ResourceRecordSets.nameAndTypeEqualTo;
import static denominator.model.ResourceRecordSets.nameEqualTo;
import static denominator.model.ResourceRecordSets.nameTypeAndQualifierEqualTo;
import static denominator.model.ResourceRecordSets.notNull;
import static denominator.route53.Route53.ActionOnResourceRecordSet.create;
import static denominator.route53.Route53.ActionOnResourceRecordSet.delete;

public final class Route53AllProfileResourceRecordSetApi implements AllProfileResourceRecordSetApi {

  private final Route53 api;
  private final String zoneId;

  Route53AllProfileResourceRecordSetApi(Route53 api, String zoneId) {
    this.api = api;
    this.zoneId = zoneId;
  }

  private static Filter<ResourceRecordSet<?>> notAlias() {
    return new AndNotAlias(notNull());
  }

  private static Filter<ResourceRecordSet<?>> andNotAlias(Filter<ResourceRecordSet<?>> first) {
    return new AndNotAlias(first);
  }

  /**
   * lists and lazily transforms all record sets who are not aliases into denominator format.
   */
  @Override
  public Iterator<ResourceRecordSet<?>> iterator() {
    return lazyIterateRRSets(api.listResourceRecordSets(zoneId), notAlias());
  }

  /**
   * lists and lazily transforms all record sets for a name which are not aliases into denominator
   * format.
   */
  @Override
  public Iterator<ResourceRecordSet<?>> iterateByName(String name) {
    Filter<ResourceRecordSet<?>> filter = andNotAlias(nameEqualTo(name));
    return lazyIterateRRSets(api.listResourceRecordSets(zoneId, name), filter);
  }

  @Override
  public Iterator<ResourceRecordSet<?>> iterateByNameAndType(String name, String type) {
    Filter<ResourceRecordSet<?>> filter = andNotAlias(nameAndTypeEqualTo(name, type));
    return lazyIterateRRSets(api.listResourceRecordSets(zoneId, name, type), filter);
  }

  @Override
  public ResourceRecordSet<?> getByNameTypeAndQualifier(String name, String type,
                                                        String qualifier) {
    Filter<ResourceRecordSet<?>> filter = andNotAlias(
        nameTypeAndQualifierEqualTo(name, type, qualifier));
    ResourceRecordSetList first = api.listResourceRecordSets(zoneId, name, type, qualifier);
    return nextOrNull(lazyIterateRRSets(first, filter));
  }

  public ResourceRecordSet<?> getByNameAndType(String name, String type) {
    return nextOrNull(filter(iterateByNameAndType(name, type), notAlias()));
  }

  @Override
  public void put(ResourceRecordSet<?> rrset) {
    List<ActionOnResourceRecordSet> changes = new ArrayList<ActionOnResourceRecordSet>();
    ResourceRecordSet<?> oldRRS;
    if (rrset.qualifier() != null) {
      oldRRS = getByNameTypeAndQualifier(rrset.name(), rrset.type(), rrset.qualifier());
    } else {
      oldRRS = getByNameAndType(rrset.name(), rrset.type());
    }
    if (oldRRS != null) {
      if (oldRRS.equals(rrset)) {
        return;
      }
      changes.add(delete(oldRRS));
    }
    changes.add(create(rrset));
    api.changeResourceRecordSets(zoneId, changes);
  }

  @Override
  public void deleteByNameTypeAndQualifier(String name, String type, String qualifier) {
    ResourceRecordSet<?> oldRRS = getByNameTypeAndQualifier(name, type, qualifier);
    if (oldRRS == null) {
      return;
    }
    api.changeResourceRecordSets(zoneId, Arrays.asList(delete(oldRRS)));
  }

  @Override
  public void deleteByNameAndType(String name, String type) {
    List<ActionOnResourceRecordSet> changes = new ArrayList<ActionOnResourceRecordSet>();
    for (Iterator<ResourceRecordSet<?>> it = iterateByNameAndType(name, type); it.hasNext(); ) {
      changes.add(delete(it.next()));
    }
    api.changeResourceRecordSets(zoneId, changes);
  }

  Iterator<ResourceRecordSet<?>> lazyIterateRRSets(final ResourceRecordSetList first,
                                                   final Filter<ResourceRecordSet<?>> filter) {
    if (first.next == null) {
      return filter(first.iterator(), filter);
    }
    return new Iterator<ResourceRecordSet<?>>() {
      PeekingIterator<ResourceRecordSet<?>> current = peekingIterator(first.iterator());
      NextRecord next = first.next;

      @Override
      public boolean hasNext() {
        while (!current.hasNext() && next != null) {
          ResourceRecordSetList nextPage;
          if (next.identifier != null) {
            nextPage = api.listResourceRecordSets(zoneId, next.name, next.type,
                                                  next.identifier);
          } else {
            nextPage = api.listResourceRecordSets(zoneId, next.name, next.type);
          }
          current = peekingIterator(nextPage.iterator());
          next = nextPage.next;
        }
        return current.hasNext() && filter.apply(current.peek());
      }

      @Override
      public ResourceRecordSet<?> next() {
        return current.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  static final class Factory implements denominator.AllProfileResourceRecordSetApi.Factory {

    private final Route53 api;

    @Inject
    Factory(Route53 api) {
      this.api = api;
    }

    @Override
    public Route53AllProfileResourceRecordSetApi create(String id) {
      return new Route53AllProfileResourceRecordSetApi(api, id);
    }
  }

  private static class AndNotAlias implements Filter<ResourceRecordSet<?>> {

    private final Filter<ResourceRecordSet<?>> first;

    private AndNotAlias(Filter<ResourceRecordSet<?>> first) {
      this.first = first;
    }

    @Override
    public boolean apply(ResourceRecordSet<?> input) {
      if (!first.apply(input)) {
        return false;
      }
      if (input.records().isEmpty()) {
        return true;
      }
      return true;
    }
  }
}
