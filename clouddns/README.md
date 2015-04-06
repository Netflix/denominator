## Notable Behaviors
The following are notable when compared to different providers.
* `Zone.id()` is opaque and `Zone.name()` doesn't include a trailing dot.
* Zone lists are 1 + N requests in order to zip with the SOA's ttl.
* `Zone.ttl()` is the default for new records.
* `SOAData.refresh(),retry(),expire(), and minimum()` are invalid as they aren't exposed via the api.
* 413 errors are common as the api is chatty, yet throttled.
