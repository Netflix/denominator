package denominator.clouddns;

import java.util.List;
import java.util.Map;

import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.clouddns.RackspaceApis.Job;
import denominator.clouddns.RackspaceApis.Record;
import denominator.model.rdata.AAAAData;
import denominator.model.rdata.AData;
import denominator.model.rdata.CNAMEData;
import denominator.model.rdata.MXData;
import denominator.model.rdata.NSData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.TXTData;
import feign.RetryableException;
import feign.Retryer;

import static denominator.common.Util.split;
import static java.lang.String.format;

final class CloudDNSFunctions {

  static void awaitComplete(CloudDNS api, Job job) {
    RetryableException retryableException = new RetryableException(
        format("Job %s did not complete. Check your logs.", job.id), null);
    Retryer retryer = new Retryer.Default(500, 1000, 30);

    while (true) {
      job = api.getStatus(job.id);

      if ("COMPLETED".equals(job.status)) {
        break;
      } else if ("ERROR".equals(job.status)) {
        throw retryableException;
      }

      retryer.continueOrPropagate(retryableException);
    }
  }

  static Map<String, Object> toRDataMap(Record record) {
    if ("A".equals(record.type)) {
      return AData.create(record.data());
    } else if ("AAAA".equals(record.type)) {
      return AAAAData.create(record.data());
    } else if ("CNAME".equals(record.type)) {
      return CNAMEData.create(record.data());
    } else if ("MX".equals(record.type)) {
      return MXData.create(record.priority, record.data());
    } else if ("NS".equals(record.type)) {
      return NSData.create(record.data());
    } else if ("SRV".equals(record.type)) {
      List<String> rdata = split(' ', record.data());
      return SRVData.builder()
          .priority(record.priority)
          .weight(Integer.valueOf(rdata.get(0)))
          .port(Integer.valueOf(rdata.get(1)))
          .target(rdata.get(2)).build();
    } else if ("TXT".equals(record.type)) {
      return TXTData.create(record.data());
    } else {
      throw new UnsupportedOperationException("record type not yet supported" + record);
    }
  }

  private CloudDNSFunctions() {
  }
}
