package denominator.clouddns;

import java.util.List;
import java.util.Map;

import denominator.clouddns.RackspaceApis.CloudDNS;
import denominator.clouddns.RackspaceApis.Job;
import denominator.clouddns.RackspaceApis.Record;
import denominator.common.Util;
import denominator.model.rdata.MXData;
import denominator.model.rdata.SOAData;
import denominator.model.rdata.SRVData;
import denominator.model.rdata.TXTData;
import feign.RetryableException;
import feign.Retryer;

import static denominator.common.Util.split;
import static java.lang.String.format;

final class CloudDNSFunctions {

  /**
   * Returns the ID of the object created or null.
   */
  static String awaitComplete(CloudDNS api, Job job) {
    RetryableException retryableException = new RetryableException(
        format("Job %s did not complete. Check your logs.", job.id), null);
    Retryer retryer = new Retryer.Default(500, 1000, 30);

    while (true) {
      job = api.getStatus(job.id);

      if ("COMPLETED".equals(job.status)) {
        return job.resultId;
      } else if ("ERROR".equals(job.status)) {
        throw new IllegalStateException(
            format("Job %s failed with error: %s", job.id, job.errorDetails));
      }

      retryer.continueOrPropagate(retryableException);
    }
  }

  /**
   * Special-cases priority field and the strange and incomplete SOA record.
   */
  static Map<String, Object> toRDataMap(Record record) {
    if ("MX".equals(record.type)) {
      return MXData.create(record.priority, record.data());
    } else if ("TXT".equals(record.type)) {
      return TXTData.create(record.data());
    } else if ("SRV".equals(record.type)) {
      List<String> rdata = split(' ', record.data());
      return SRVData.builder()
          .priority(record.priority)
          .weight(Integer.valueOf(rdata.get(0)))
          .port(Integer.valueOf(rdata.get(1)))
          .target(rdata.get(2)).build();
    } else if ("SOA".equals(record.type)) {
      List<String> threeParts = split(' ', record.data());
      return SOAData.builder()
          .mname(threeParts.get(0))
          .rname(threeParts.get(1))
          .serial(Integer.valueOf(threeParts.get(2)))
          .refresh(record.ttl)
          .retry(record.ttl)
          .expire(record.ttl).minimum(record.ttl).build();
    } else {
      return Util.toMap(record.type, record.data());
    }
  }

  private CloudDNSFunctions() {
  }
}
