## Notable Behaviors
The following are notable when compared to different providers.
* `Zone.id()` is opaque.
* `Zone.ttl()` is the default for new records.
* `ZoneApi.iterateByName()` is a client-side filter.
* Each zone has provider-specific NS records that aren't visible to the api.
