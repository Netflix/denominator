# Denominator CLI

The denominator CLI is a git-like-cli based on the [airline](https://github.com/airlift/airline) project.  It is packaged as a [really executable jar](http://skife.org/java/unix/2011/06/20/really_executable_jars.html) which means you can do `./denominator` without any of the `java -jar` stuff.

### Binaries
Here's how to get denominator-cli `1.1.3` from [bintray](https://bintray.com/pkg/show/general/netflixoss/denominator/denominator-cli)

1. [Download denominator](http://dl.bintray.com/content/netflixoss/denominator/denominator-cli/release/1.1.3/denominator?direct)
2. Place it on your `$PATH`. (ex. `~/bin`)
3. Set it to be executable. (`chmod 755 ~/bin/denominator`)

## Building
To build the cli, execute `./gradlew clean test install` from the root of your denominator clone.  The binary will end up at `/denominator-cli/build/denominator`

## Running
denominator will print out a help statement, but here's the gist.

If you just want to fool around, you can use the `mock` provider.
```bash
$ denominator -p mock zone list
denominator.io.
$ denominator -p mock -z denominator.io. record list
denominator.io.                                    SOA   3600   ns1.denominator.io. admin.denominator.io. 1 3600 600 604800 60
denominator.io.                                    NS    86400  ns1.denominator.io.
```

### Providers
Different providers connect to different urls and need different credentials.  First step is to run `./denominator providers` to see how many `-c` args you need and what values they should have:

```bash
$ denominator providers
provider   url                                                  credentialType credentialArgs
mock       mem:mock
clouddns   https://identity.api.rackspacecloud.com/v2.0/        apiKey         username apiKey
dynect     https://api2.dynect.net/REST                         password       customer username password
route53    https://route53.amazonaws.com                        accessKey      accessKey secretKey
route53    https://route53.amazonaws.com                        session        accessKey secretKey sessionToken
ultradns   https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01  password       username password
```

The first field says the type, if any.  If there's no type listed, it needs no credentials.  If there is a type listed, the following fields are credential args.  Say for example, you were using `ultradns`.  

```
ultradns   https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01  password       username password
```
This says the provider `ultradns` connects by default to `https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01` and supports `password` authentication, which needs two `-c` parameters: `username` and `password`.  To put it together, you'd specify the following to do a zone list:
```bash
./denominator -p ultradns -c myusername -c mypassword zone list
```
If you need to connect to an alternate url, pass the `-u` parameter:
```bash
./denominator -p ultradns -u https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01-BETA -c myusername -c mypassword zone list
```

### Zone
`zone list` returns the zones names in your account.  Ex.
```bash
$ denominator -p ultradns -c my_user -c my_password zone list
--snip--
netflix.com.
--snip--
```

### Record
`-z zone. record list` returns the record details of that zone.  Ex.
```bash
$ denominator -p ultradns -c my_user -c my_password record --zone netflix.com. list
--snip--
email.netflix.com.                                 A     3600   69.53.237.168
--snip--
```

### Geo
`-z zone. geo list` returns the records that have directional configuration in that zone.  Ex.
```bash
$ denominator -p ultradns -c my_user -c my_password geo --zone netflix.com. list
--snip--
www.geo.denominator.io.                           CNAME  300   a.denominator.io. alazona {United States (US)=[Alaska, Arizona]}
--snip--
```

## Hooks
### IAM Instance Profile
If you are using the `route53` provider on an ec2 instance with a profile associated with it, you don't need to pass credentials. 

Ex.
```
./denominator -p route53 zone list
```
### EC2 Instance Metadata
When running on an ec2 instance, you can use shortcuts to supply `A` or `CNAME` record data from instance metadata.

Ex.
```
$ ./denominator -p route53 record -z myzone.com. add -n wonky.myzone.com. -t A --ec2-public-ipv4
;; in zone myzone.com. adding to rrset wonky.myzone.com. A values: [{address=75.101.168.33}]
;; ok
```
