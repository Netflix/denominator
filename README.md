denominator
======
denominator is a building block for portably controlling DNS mappings across cloud providers.  denominator exposes features present on a subset of DNS providers, such as GEO or Latency mapping.  denominator is a Java 7 library intended to be embedded in edge control planes.

Features
--------
* Portability
denominator works against multiple backends, including Route53, DynECT, and UltraDNS

* Feature Discovery
denominator allows you to discover the record type and features supported by the backends

* IP mapping
denominator can read edge IP addresses, for example Amazon EC2 Elastic IPs.

License
-------
Copyright (C) 2013 Netflix, Inc.

Licensed under the Apache License, Version 2.0
