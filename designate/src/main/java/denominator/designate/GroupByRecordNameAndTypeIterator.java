package denominator.designate;

import java.util.Iterator;
import java.util.Map;

import denominator.common.PeekingIterator;
import denominator.designate.Designate.Record;
import denominator.model.ResourceRecordSet;
import denominator.model.ResourceRecordSet.Builder;

import static denominator.common.Util.peekingIterator;
import static denominator.designate.DesignateFunctions.toRDataMap;

class GroupByRecordNameAndTypeIterator implements Iterator<ResourceRecordSet<?>> {

  private final PeekingIterator<Record> peekingIterator;

  public GroupByRecordNameAndTypeIterator(Iterator<Record> sortedIterator) {
    this.peekingIterator = peekingIterator(sortedIterator);
  }

  private static boolean nameAndTypeEquals(Record actual, Record expected) {
    return actual.name.equals(expected.name) && actual.type.equals(expected.type);
  }

  @Override
  public boolean hasNext() {
    return peekingIterator.hasNext();
  }

  @Override
  public ResourceRecordSet<?> next() {
    Record recordDetail = peekingIterator.next();
    Builder<Map<String, Object>> builder = ResourceRecordSet.builder()
        .name(recordDetail.name)
        .type(recordDetail.type)
        .ttl(recordDetail.ttl)
        .add(toRDataMap(recordDetail));
    while (hasNext()) {
      Record next = peekingIterator.peek();
      if (next == null) {
        continue;
      }
      if (nameAndTypeEquals(next, recordDetail)) {
        builder.add(toRDataMap(peekingIterator.next()));
      } else {
        break;
      }
    }
    return builder.build();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
