### Version 1.3.0
* Deprecated remaining naming conventions that use syntax like `getUrl` or `listByName` to `url` or `iterateByName` to support migration to denominator 2.0.
* ResourceRecordSet no longer implements `List<D>`.  Please access rdata via the `rdata()` accessor.
* Added `ResourceRecordSet.getQualifier()`, `ReadOnlyResourceRecordSetApi.getByNameTypeAndQualifier()`
* Deprecated `Geo.getGroup()` for `ResourceRecordSet.getQualifier()`
* Deprecated `GeoResourceRecordSetApi.getByNameTypeAndGroup()` for `ReadOnlyResourceRecordSetApi.getByNameTypeAndQualifier()`
* Deprecated `GeoResourceRecordSetApi.applyRegionsToNameTypeAndGroup()` for `GeoResourceRecordSetApi.applyRegionsToNameTypeAndQualifier()`
* Deprecated `GeoResourceRecordSetApi.applyTTLToNameTypeAndGroup()` for `GeoResourceRecordSetApi.applyTTLToNameTypeAndQualifier()`
* Add `-t/--type` to list, and `-q/--qualifier` to get record cli commands.

### Version 1.2.1
* remove strict zone name checks on deprecated `DNSApi.get..Api` methods as often the backend will accept zone names with or without a trailing dot.

### Version 1.2.0
* Added `Zone` type, allowing disambiguation when `Provider.supportsMultipleZoneNames`.
* Added zone id to commandline `zone list` output when present. 
* Made all Zone and RecordSet apis implement Iterable for convenience.
* Deprecated naming conventions such as `getResourceRecordSetApiForZone` in favor of shorter forms like `basicRecordSetsInZone`.
* Deprecated naming conventions in `Provider` such as `getUrl()` in favor of those matching Feign's `Target`, like `url()`.

### Version 1.1.4
* New -C/--config commandline arg specifying the path to your provider configuration.
* When changing geo groups in UltraDNS, territories are implicitly moved from other groups as necessary.
* Better performance in UltraDNS when listing directional record sets.
* Significant performance increase in DynECT list operations when zones have many records.
* Fix pagination bug in Rackspace CloudDNS.

### Version 1.1.3
* support use as a Dagger library.  ex. `ObjectGraph.create(provider(new UltraDNSProvider()), new UltraDNSProvider.Module(), credentials("username", "password"))`
* expose Provider.getUrl and support dynamic url updates

### Version 1.1.2
* update dagger dependency to 1.0.0

### Version 1.1.1
* internal refactoring in preparation for dagger 1.0 update
* update library dependencies to most current versions

### Version 1.1.0
* add ResourceRecordSet.getProfiles(): specifies server-side aspects such as visibility based on client origin, latency or server health.
* add denominator.model.profile.Geo: rrsets with this profile are visible to the regions specified.
* add GeoResourceRecordSetApi: list rrsets by geo group and change their region mappings.
* add AllProfileResourceRecordSetApi: list all rrsets with or without server-side profiles.

### Version 1.0.2
* updates to jclouds 1.6.0
* add read-only implementation of Rackspace Cloud DNS (`clouddns`)

### Version 1.0.1
* updates to jclouds 1.6.0-rc.5
* fixes session problem when ip changes during dynect session

### Version 1.0.0

* Initial open source release 
* Includes resource record set CRUD and TTL operations for DynECT, Route53, and UltraDNS
* Includes a command-line client with EC2 hooks for instance metadata
* See [Netflix Tech Blog](http://techblog.netflix.com/2013/03/denominator-multi-vendor-interface-for.html) for an introduction.
