package denominator.verisigndns;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static denominator.common.Preconditions.checkNotNull;

class RecordFilter {

  public static <T> Iterable<T> filter(final Iterable<T> unfiltered,
      final Predicate<? super T> predicate) {
    checkNotNull(unfiltered, "data was null");
    checkNotNull(predicate, "predicate was null");

    final Iterator<T> it = unfiltered.iterator();
    final List<T> list = new ArrayList<T>();
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        while (it.hasNext()) {
          final T element = it.next();
          if (predicate.apply(element)) {
            list.add(element);
          }
        }
        return list.iterator();
      }
    };
  }

  interface Predicate<T> {
    boolean apply(T input);
    boolean equals(Object object);
  }

  static class InPredicate<T> implements Predicate<T>, Serializable {
    private static final long serialVersionUID = 1L;
    private final Collection<?> target;

    InPredicate(Collection<?> target) {
      this.target = checkNotNull(target, "target was null");
    }

    @Override
    public boolean apply(T element) {
      try {
        return target.contains(element);
      } catch (NullPointerException e) {
        return false;
      } catch (ClassCastException e) {
        return false;
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof InPredicate) {
        InPredicate<?> that = (InPredicate<?>) obj;
        return target.equals(that.target);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return target.hashCode();
    }
  }

  static class NotPredicate<T> implements Predicate<T>, Serializable {
    private static final long serialVersionUID = 1L;
    final Predicate<T> predicate;

    NotPredicate(Predicate<T> predicate) {
      this.predicate = checkNotNull(predicate, "predicate was null");
    }

    @Override
    public boolean apply(T t) {
      return !predicate.apply(t);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof NotPredicate) {
        NotPredicate<?> that = (NotPredicate<?>) obj;
        return predicate.equals(that.predicate);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return ~predicate.hashCode();
    }
  }

  public static <T> Predicate<T> in(Collection<? extends T> target) {
    return new InPredicate<T>(target);
  }

  public static <T> Predicate<T> not(Predicate<T> predicate) {
    return new NotPredicate<T>(predicate);
  }


  public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements) {
    return newArrayList(elements.iterator());
  }

  public static <E> ArrayList<E> newArrayList(Iterator<? extends E> elements) {
    ArrayList<E> list = new ArrayList<E>();
    while (elements.hasNext()) {
      list.add(elements.next());
    }
    return list;
  }
}
