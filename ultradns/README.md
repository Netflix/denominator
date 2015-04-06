## Notable Behaviors
The following are notable when compared to different providers.
* `Zone.id()` is the `Zone.name()`
* Zone lists are 1 + N requests in order to zip with the SOA's ttl and rname.
* The default ttl for record sets is hard-coded to 300.
* The zone's NS record set must contain at least 2 nsdnames.
