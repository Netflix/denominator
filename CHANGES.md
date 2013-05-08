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
* Includes resource record set CRUD operations for DynECT, Route53, and UltraDNS
* Includes a command-line client with EC2 hooks for instance metadata
* See [Netflix Tech Blog](http://techblog.netflix.com/2013/03/denominator-multi-vendor-interface-for.html) for an introduction.
