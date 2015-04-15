## Notable Behaviors
The following are notable when compared to different providers.
* `Zone.id()` is opaque and multiple zones can exist with the same `Zone.name()`.
* Zone lists are 1 + N requests in order to zip with the SOA's ttl and rname.
* The default ttl for record sets is hard-coded to 300.
* The zone's NS record set can be altered, but not removed.
* `SPF` and `TXT` rdata are quoted when accessed from the Route53 api directly. Denominator unquotes them.
