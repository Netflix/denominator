package denominator.route53;

import java.util.LinkedHashMap;

import denominator.model.ResourceRecordSet;

import static denominator.common.Preconditions.checkNotNull;

/**
 * Reference to the addresses in a CloudFront distribution, Amazon S3 bucket, Elastic Load Balancing
 * load balancer, or Route 53 hosted zone. <br> Only valid when {@link ResourceRecordSet#type()} is
 * {@code A} or {@code AAAA} .
 *
 * <br> <b>Example</b><br>
 *
 * <pre>
 * AliasTarget rdata = AliasTarget.create(&quot;Z3DZXE0Q79N41H&quot;,
 * &quot;nccp-cbp-frontend-12345678.us-west-2.elb.amazonaws.com.&quot;);
 * </pre>
 *
 * See <a href= "http://docs.aws.amazon.com/Route53/latest/APIReference/API_ChangeResourceRecordSets.html#API_ChangeResourceRecordSets_RequestAliasSyntax"
 * >API to create aliases</a>
 *
 * @since 4.2
 */
public final class AliasTarget extends LinkedHashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  AliasTarget(String hostedZoneId, String dnsName) {
    put("HostedZoneId", checkNotNull(hostedZoneId, "HostedZoneId"));
    put("DNSName", checkNotNull(dnsName, "DNSName"));
  }

  public static AliasTarget create(String hostedZoneId, String dnsName) {
    return new AliasTarget(hostedZoneId, dnsName);
  }

  /**
   * Hosted zone ID for your CloudFront distribution, Amazon S3 bucket, Elastic Load Balancing load
   * balancer, or Route 53 hosted zone.
   */
  public String hostedZoneId() {
    return get("HostedZoneId").toString();
  }

  /**
   * DNS domain name for your CloudFront distribution, Amazon S3 bucket, Elastic Load Balancing load
   * balancer, or another resource record set in this hosted zone.
   */
  public String dnsName() {
    return get("DNSName").toString();
  }
}
