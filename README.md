<img src="https://raw.github.com/Netflix/denominator/master/denominator.jpg" alt="Denominator">

# Portable control of DNS clouds

Denominator is a portable Java library for manipulating DNS clouds. Denominator has pluggable back-ends, including AWS Route53, Neustar Ultra, DynECT, Rackspace Cloud DNS, OpenStack Designate, and a mock for testing. We also ship a command line version so it's easy for anyone to try it out. Denominator currently supports basic zone and record features, as well GEO (aka Directional) profiles. See [Netflix Tech Blog](http://techblog.netflix.com/2013/03/denominator-multi-vendor-interface-for.html) for an introduction.

[![Build Status](https://netflixoss.ci.cloudbees.com/job/denominator-master/badge/icon)](https://netflixoss.ci.cloudbees.com/job/denominator-master/)

## Command line
For your convenience, the denominator cli is a [single executable file](http://skife.org/java/unix/2011/06/20/really_executable_jars.html). Under the hood, the cli uses [airline](https://github.com/airlift/airline) to look and feel like dig or git.

## Android
There's no official android client, yet. However, we do have an [Android Example](https://github.com/Netflix/denominator/tree/master/example-android) you can try out.

### Binaries
If you are using OSX, [Homebrew](http://mxcl.github.io/homebrew/) has a built-in installer for denominator.

Here's how to get denominator-cli `4.6.0` from [bintray](https://bintray.com/pkg/show/general/netflixoss/denominator/denominator-cli)

1. [Download denominator](https://bintray.com/artifact/download/netflixoss/maven/com/netflix/denominator/denominator-cli/4.6.0/denominator-cli-4.6.0-fat.jar)
2. Place it on your `$PATH`. (ex. `~/bin`)
3. Set it to be executable. (`chmod 755 ~/bin/denominator`)

### Getting Started

Advanced usage, including ec2 hooks are covered in the [readme](https://github.com/Netflix/denominator/tree/master/cli). Here's a quick start for the impatient.

If you just want to fool around, you can use the `mock` provider.
```bash
# first column is the zone id, which isn't always its name!
$ denominator -p mock zone list
denominator.io.          denominator.io.                                          admin.denominator.io.                86400
$ denominator -p mock record -z denominator.io. list
denominator.io.                                    SOA   3600   ns1.denominator.io. admin.denominator.io. 1 3600 600 604800 60
denominator.io.                                    NS    86400  ns1.denominator.io.
```

Different providers connect to different urls and need different credentials. First step is to run `./denominator providers` to see how many `-c` args you need and what values they should have:

```bash
$ denominator providers
provider   url                                                 duplicateZones credentialType credentialArgs
mock       mem:mock                                            false
clouddns   https://identity.api.rackspacecloud.com/v2.0/       true           apiKey         username apiKey
designate  http://localhost:5000/v2.0                          true           password       tenantId username password
dynect     https://api2.dynect.net/REST                        false          password       customer username password
route53    https://route53.amazonaws.com                       true           accessKey      accessKey secretKey
route53    https://route53.amazonaws.com                       true           session        accessKey secretKey sessionToken
ultradns   https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01 false          password       username password
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

Denominator exposes a portable [model](https://github.com/Netflix/denominator/wiki/Models) implemented by pluggable `Provider`s such as `route53`, `ultradns`, `dynect`, `clouddns`, or `mock`. Under the covers, providers are [Dagger](http://square.github.com/dagger/) modules. Except for the mock, all current providers bind to http requests via [feign](https://github.com/Netflix/feign), which keeps the distribution light.

### Binaries

The current version of denominator is `4.6.0`

Denominator can be resolved as maven dependencies, or equivalent in lein, gradle, etc. Here are the coordinates, noting you only need to list the providers you use.
```xml
<dependencies>
  <dependency>
    <groupId>com.netflix.denominator</groupId>
    <artifactId>denominator-core</artifactId>
    <version>${denominator.version}</version>
  </dependency>
  <dependency>
    <groupId>com.netflix.denominator</groupId>
    <artifactId>denominator-clouddns</artifactId>
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
The credentials are variable length, as certain providers require more that 2 parts. The above returns an instance of `DNSApiManager` where apis such as `ZoneApis` are found. Here's how to list zones: 
```java
for (Zone zone : manager.api().zones()) {
  for (ResourceRecordSet<?> rrs : manager.api().recordSetsInZone(zone.id())) {
    processRRS(rrs);
  }
}
```

If you are running unit tests, or don't have a cloud account yet, you can use mock, and skip the credentials part.
```java
DNSApiManager manager = Denominator.create("mock");
```

The Denominator [model](https://github.com/Netflix/denominator/wiki/Model) is based on the `ResourceRecordSet` concept. A `ResourceRecordSet` is simply a group of records who share the same name and type. For example all address (`A`) records for the name `www.netflix.com.` are aggregated into the same `ResourceRecordSet`. The values of each record in a set are type-specific. These data types are implemented as map-backed interfaces. This affords both the strong typing of java and extensibility and versatility of maps.

For example, the following are identical:
```java
mxData.getPreference();
mxData.get("preference");
```

Resource record sets live in a Zone, which is roughly analogous to a domain. Zones can be created on-demand and deleted.

### Use via Dagger
Some users may wish to use Denominator as a Dagger library. Here's one way to achieve that:
```java
import static denominator.CredentialsConfiguration.credentials;
import static denominator.Dagger.provider;
...
// this shows how to facilitate runtime url updates
Provider fromDiscovery = new UltraDNSProvider() {
  public String url() {
    return discovery.urlFor("ultradns");
  }
};
// this shows how to facilitate runtime credential updates
Supplier<Credentials> fromEncryptedStore = new Supplier<Credentials>() {
  public Credentials get() {
    return encryptedStore.getCredentialsFor("ultradns");
  }
}
DNSApiManager manager = ObjectGraph.create(provider(fromDiscovery), new UltraDNSProvider.Module(), credentials(fromEncryptedStore))
                                   .get(DNSApiManager.java);
```

## Third-Party Providers
Denominator also operates with third-party DNS providers such as [DiscoveryDNS](https://github.com/discoverydns/denominator-discoverydns).

Providers are looked up by name using [ServiceLoader](https://docs.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html). Given the provider is in the classpath, normal lookups should work.

```java
// Given the discoverydns jar is in the classpath
DNSApiManager manager = Denominator.create("discoverydns");
```

Third-party support is also explained in the [CLI readme](https://github.com/Netflix/denominator/tree/master/cli).

## Build

To build:

```bash
$ git clone git@github.com:Netflix/denominator.git
$ cd denominator/
$ ./gradlew clean test install
```

## Intellij Idea IDE

Generate the Idea project files:

```bash
$ ./gradlew idea
```

Import the project:

1. File > Import Project...
2. Preferences > Compiler > Annotation Processors > Check "Enable annotation processing"

Run the live tests:

1. Choose a live test (e.g. ```CloudDNSCheckConnectionLiveTest```)
2. Right click and select "Create CloudDNSCheckConnectionLiveTest"
3. VM options: ```-ea -Dclouddns.username=<username> -Dclouddns.password=<password>```
4. Working directory: ```/path/to/denominator/clouddns```

## Feedback and Collaboration

* For high-level updates, follow [denominatorOSS](https://twitter.com/denominatorOSS) on Twitter.
* For questions, please tag `denominator` in [StackOverflow](http://stackoverflow.com).
* For bugs and enhancements, please use [Github Issues](https://github.com/Netflix/denominator/issues).
* For email discussions, please post to the [user forum](https://groups.google.com/forum/?fromgroups#!forum/denominator-user)
* For discussions on design and internals, please join #denominator on irc freenode or post to the [dev forum](https://groups.google.com/forum/?fromgroups#!forum/denominator-dev)
 
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
