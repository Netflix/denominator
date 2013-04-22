<img src="https://raw.github.com/Netflix/denominator/master/denominator.jpg" alt="Denominator">

# Portable control of DNS clouds

Denominator is a portable Java library for manipulating DNS clouds.  Denominator has pluggable back-ends, initially including AWS Route53, Neustar Ultra, DynECT, and a mock for testing.  We also ship a command line version so it's easy for anyone to try it out.  Denominator currently supports basic zone and record features, but will soon include GEO (aka Directional) support and advanced usage.  See [Netflix Tech Blog](http://techblog.netflix.com/2013/03/denominator-multi-vendor-interface-for.html) for an introduction.

[![Build Status](https://netflixoss.ci.cloudbees.com/job/denominator-master/badge/icon)](https://netflixoss.ci.cloudbees.com/job/denominator-master/)

## Command line
For your convenience, the denominator cli is a [single executable file](http://skife.org/java/unix/2011/06/20/really_executable_jars.html).  Under the hood, the cli uses [airline](https://github.com/airlift/airline) to look and feel like dig or git.

### Binaries
Here's how to get denominator-cli `1.0.1` from [bintray](https://bintray.com/pkg/show/general/netflixoss/denominator/denominator-cli)

1. [Download denominator](http://dl.bintray.com/content/netflixoss/denominator/denominator-cli/release/1.0.1/denominator?direct)
2. Place it on your `$PATH`. (ex. `~/bin`)
3. Set it to be executable. (`chmod 755 ~/bin/denominator`)

### Getting Started

Advanced usage, including ec2 hooks are covered in the [readme](https://github.com/Netflix/denominator/tree/master/denominator-cli).  Here's a quick start for the impatient.

If you just want to fool around, you can use the `mock` provider.
```bash
$ denominator -p mock zone list
denominator.io.
$ denominator -p mock record -z denominator.io. list
denominator.io.                                    SOA   3600   ns1.denominator.io. admin.denominator.io. 1 3600 600 604800 60
denominator.io.                                    NS    86400  ns1.denominator.io.
```

Different providers need different credentials.  First step is to run `./denominator providers` to see how many `-c` args you need and what values they should have:

```bash
$ denominator providers
provider             credential type  credential arguments
mock                
dynect               password         customer username password
ultradns             password         username password
route53              accessKey        accessKey secretKey
route53              session          accessKey secretKey sessionToken
```

Now, you can list your zones or records.

```bash
$ denominator -p ultradns -c my_user -c my_password zone list
--snip--
netflix.com.
--snip--
$ denominator -p ultradns -c my_user -c my_password record --zone netflix.com. list
--snip--
email.netflix.com.                                 A     3600   192.0.2.1
--snip--
```

## Code

Denominator exposes a portable [model](https://github.com/Netflix/denominator/wiki/Models) implemented by pluggable `Provider`s such as `route53`, `ultradns`, `dynect`, or `mock`.  Under the covers, providers are [Dagger](http://square.github.com/dagger/) modules.  Except for the mock, all current providers bind to [jclouds](https://github.com/jclouds/jclouds) libraries.  That said, denominator has no core dependencies outside guava and dagger, so developers are free to implement providers however they choose.

### Binaries

The current version of denominator is `1.0.1`

Denominator can be resolved as maven dependencies, or equivalent in lein, gradle, etc.  Here are the coordinates, noting you only need to list the providers you use.
```xml
<dependencies>
  <dependency>
    <groupId>com.netflix.denominator</groupId>
    <artifactId>denominator-core</artifactId>
    <version>${denominator.version}</version>
  </dependency>
  <dependency>
    <groupId>com.netflix.denominator</groupId>
    <artifactId>denominator-dynect</artifactId>
    <version>${denominator.version}</version>
  </dependency>
  <dependency>
    <groupId>com.netflix.denominator</groupId>
    <artifactId>denominator-route53</artifactId>
    <version>${denominator.version}</version>
  </dependency>
  <dependency>
    <groupId>com.netflix.denominator</groupId>
    <artifactId>denominator-ultradns</artifactId>
    <version>${denominator.version}</version>
  </dependency>
</dependencies>
```

### Getting Started

Creating a connection to a provider requires that you have access to two things: the name of the provider, and as necessary, credentials for it.
```java
import static denominator.CredentialsConfiguration.credentials;
...
DNSApiManager manager = Denominator.create("ultradns", credentials(username, password)); // manager is closeable, so please close it!
```
The credentials are variable length, as certain providers require more that 2 parts. The above returns an instance of `DNSApiManager` where apis such as `ZoneApis` are found.  Here's how to list zones: 
```java
for (Iterator<String> zone = manager.getApi().getZoneApi().list(); zone.hasNext();) {
    processZone(zone.next());
}
```

If you are running unit tests, or don't have a cloud account yet, you can use mock, and skip the credentials part.
```java
DNSApiManager manager = Denominator.create("mock");
```

The Denominator [model](https://github.com/Netflix/denominator/wiki/Model) is based on the `ResourceRecordSet` concept.  A `ResourceRecordSet` is simply a group of records who share the same name and type.  For example all address (`A`) records for the name `www.netflix.com.` are aggregated into the same `ResourceRecordSet`.  The values of each record in a set are type-specific.  These data types are implemented as map-backed interfaces.  This affords both the strong typing of java and extensibility and versatility of maps.

For example, the following are identical:
```java
mxData.getPreference();
mxData.get("preference");
```

## Build

To build:

```bash
$ git clone git@github.com:Netflix/denominator.git
$ cd denominator/
$ ./gradlew clean test install
```

## Feedback and Collaboration

* For high-level updates, follow [denominatorOSS](https://twitter.com/denominatorOSS) on Twitter.
* For questions, please tag `denominator` in [StackOverflow](http://stackoverflow.com).
* For bugs and enhancements, please use [Github Issues](https://github.com/Netflix/denominator/issues).
* For email discussions, please post to the [user forum](https://groups.google.com/forum/?fromgroups#!forum/denominator-user)
* For discussions on design and internals, please join #jclouds on irc freenode or post to the [dev forum](https://groups.google.com/forum/?fromgroups#!forum/denominator-dev)
 
## LICENSE

Copyright 2013 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
