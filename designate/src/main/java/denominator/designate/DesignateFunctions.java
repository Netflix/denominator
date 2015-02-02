package denominator.designate;

import java.util.List;
import java.util.Map;

import denominator.designate.Designate.Record;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.TXTData;

import static denominator.common.Util.split;

public final class DesignateFunctions {

  private DesignateFunctions() { /* */
  }

  static Map<String, Object> toRDataMap(Record record) {
    if ("A".equals(record.type)) {
      return AData.create(record.data);
    } else if ("AAAA".equals(record.type)) {
      return AAAAData.create(record.data);
    } else if ("CNAME".equals(record.type)) {
      return CNAMEData.create(record.data);
    } else if ("MX".equals(record.type)) {
      return MXData.create(record.priority, record.data);
    } else if ("NS".equals(record.type)) {
      return NSData.create(record.data);
    } else if ("SRV".equals(record.type)) {
      List<String> rdata = split(' ', record.data);
      return SRVData.builder()
          .priority(record.priority)
          .weight(Integer.valueOf(rdata.get(0)))
          .port(Integer.valueOf(rdata.get(1)))
          .target(rdata.get(2)).build();
    } else if ("TXT".equals(record.type)) {
      return TXTData.create(record.data);
    } else {
      throw new UnsupportedOperationException("record type not yet supported" + record);
    }
  }
}
