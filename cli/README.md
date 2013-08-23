# Denominator CLI

The denominator CLI is a git-like-cli based on the [airline](https://github.com/airlift/airline) project.  It is packaged as a [really executable jar](http://skife.org/java/unix/2011/06/20/really_executable_jars.html) which means you can do `./denominator` without any of the `java -jar` stuff.

### Binaries
Here's how to get denominator-cli `4.0.1` from [bintray](https://bintray.com/pkg/show/general/netflixoss/denominator/denominator-cli)

1. [Download denominator](http://dl.bintray.com/content/netflixoss/denominator/denominator-cli/release/4.0.1/denominator?direct)
2. Place it on your `$PATH`. (ex. `~/bin`)
3. Set it to be executable. (`chmod 755 ~/bin/denominator`)

## Building
To build the cli, execute `./gradlew clean test install` from the root of your denominator clone.  The binary will end up at `cli/build/denominator`

## Configuring

You may optionally use a configuration file in YAML format to define named providers and credentials for each.  The default path to this file is `~/.denominatorconfig`.

Here's an example of a configuration file:

```
name: route53-test
provider: route53
credentials:
  accessKey: foo1
  secretKey: foo2
---
name: ultradns-prod
provider: ultradns
url: https://alternative/rest/endpoint
credentials:
  username: your_username
  password: your_password
```

Use the `-n` arg to select the named provider.

For example, `./denominator -n route53-test zone`

To use an alternate config file, specify the `-C` arg to its path.

For example, `./denominator -C /path/to/config.yml -n route53-test zone`

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
provider   url                                                 duplicateZones credentialType credentialArgs
mock       mem:mock                                            false
clouddns   https://identity.api.rackspacecloud.com/v2.0/       true           apiKey         username apiKey
designate  http://localhost:5000/v2.0                          true           password       tenantId username password
dynect     https://api2.dynect.net/REST                        false          password       customer username password
route53    https://route53.amazonaws.com                       true           accessKey      accessKey secretKey
route53    https://route53.amazonaws.com                       true           session        accessKey secretKey sessionToken
ultradns   https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01 false          password       username password
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
#### Show Supported Geo Regions
`denominator geo regions` returns all supported regions for the zone specified.

Ex.

```bash
$ denominator -p ultradns -c my_user -c my_password geo --zone netflix.com. regions
--snip--
Russian Federation          : Russian Federation
Satellite Provider (A2)     : Satellite Provider
South America               : Argentina;Bolivia;Brazil;Chile;Colombia;Ecuador;Falkland Islands;French Guiana;Guyana;Paraguay;Peru;South Georgia and the South Sandwich Islands;Suriname;Undefined South America;Uruguay;Venezuela, Bolivarian Republic of
--snip--
```

#### List Geo Record Sets
`denominator geo list` returns the records that have directional configuration in that zone, including json form of their region data.

Ex.

```bash
$ denominator -p ultradns -c my_user -c my_password geo --zone netflix.com. list
--snip--
redirects-us.geo.denominator.io.                  CNAME  US-WEST-2           300   elb-1234.us-west-2.elb.amazonaws.com. {"Antarctica":["Bouvet Island"],"Canada (CA)":["Alberta","British Columbia","Greenland","Manitoba","Northwest Territories","Nunavut","Saint Pierre and Miquelon","Saskatchewan","Undefined Canada","Yukon"],"Mexico":["Mexico"],"South America":["Brazil"],"United States (US)":["Alaska","Arizona","Arkansas","California","Colorado","Georgia","Hawaii","Idaho","Iowa","Kansas","Louisiana","Minnesota","Missouri","Montana","Nebraska","Nevada","New Mexico","North Dakota","Oklahoma","Oregon","South Dakota","Texas","Utah","Washington","Wyoming"]}
--snip--
```

#### Add Region to Geo Record Set.
`denominator geo add` adds the territories specified, if they don't already exist, to the geo group of a record set.

  * If you specify the flag `--validate-regions`, the input json will not only be checked for well-formedness, but also that the regions are valid for the provider.  Note that this may imply more remote commands against the DNS provider.
  * If you specify the flag `--dry-run`, only read-only commands will be executed.

Ex.

```bash
$ denominator -q -n ultradns-test geo -z geo.denominator.io. add -n redirects-us.geo.denominator.io. -t CNAME -g US-WEST-2 -r '{"United States (US)": ["Maryland"]}' --validate-regions --dry-run
;; in zone geo.denominator.io. adding regions {"United States (US)": ["Maryland"]} to rrset redirects-us.geo.denominator.io. CNAME US-WEST-2
;; validated regions: {"United States (US)":["Maryland"]}
;; current rrset: {"name":"redirects-us.geo.denominator.io.","type":"CNAME","qualifier":"US-WEST-2","ttl":300,"records":[{"cname":"elb-1234.us-west-2.elb.amazonaws.com."}],"profiles":[{"type":"geo","regions":{"Antarctica":["Bouvet Island"],"Canada (CA)":["Alberta","British Columbia","Greenland","Manitoba","Northwest Territories","Nunavut","Saint Pierre and Miquelon","Saskatchewan","Undefined Canada","Yukon"],"Mexico":["Mexico"],"South America":["Brazil"],"United States (US)":["Alaska","Arizona","Arkansas","California","Colorado","Georgia","Hawaii","Idaho","Iowa","Kansas","Louisiana","Minnesota","Missouri","Montana","Nebraska","Nevada","New Mexico","North Dakota","Oklahoma","Oregon","South Dakota","Texas","Utah","Washington","Wyoming"]}}]}
;; revised rrset: {"name":"redirects-us.geo.denominator.io.","type":"CNAME","qualifier":"US-WEST-2","ttl":300,"records":[{"cname":"elb-1234.us-west-2.elb.amazonaws.com."}],"profiles":[{"type":"geo","regions":{"Antarctica":["Bouvet Island"],"Canada (CA)":["Alberta","British Columbia","Greenland","Manitoba","Northwest Territories","Nunavut","Saint Pierre and Miquelon","Saskatchewan","Undefined Canada","Yukon"],"Mexico":["Mexico"],"South America":["Brazil"],"United States (US)":["Alaska","Arizona","Arkansas","California","Colorado","Georgia","Hawaii","Idaho","Iowa","Kansas","Louisiana","Minnesota","Missouri","Montana","Nebraska","Nevada","New Mexico","North Dakota","Oklahoma","Oregon","South Dakota","Texas","Utah","Washington","Wyoming","Maryland"]}}]}
;; ok
```

## Output detail
By default, denominator emits 2 lines per http request to STDERR.  This is helpful to get high-level feedback on progress.

Ex.

```bash
$ denominator -p ultradns -c USERNAME -c PASSWORD zone list
[UltraDNS#accountId] ---> POST https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01 HTTP/1.1
[UltraDNS#accountId] <--- HTTP/1.1 200 OK (1321ms)
[UltraDNS#zonesOfAccount] ---> POST https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01 HTTP/1.1
[UltraDNS#zonesOfAccount] <--- HTTP/1.1 200 OK (106ms)
geo.denominator.io.
ultradnstest.denominator.io.
```

If you wish to omit this, add the `-q` or `--quiet` flag to your options.

```bash
$ denominator -q -p ultradns -c USERNAME -c PASSWORD zone list
geo.denominator.io.
ultradnstest.denominator.io.
```

If you wish more detail, including HTTP messages sent, add the `-v` or `--verbose` flag to your options.

```bash
$ denominator -v -p ultradns -c USERNAME -c PASSWORD zone list
[UltraDNS#accountId] ---> POST https://ultra-api.ultradns.com:8443/UltraDNS_WS/v01 HTTP/1.1
[UltraDNS#accountId] Content-Length: 704
[UltraDNS#accountId] Host: ultra-api.ultradns.com
[UltraDNS#accountId] Content-Type: application/xml
[UltraDNS#accountId]
[UltraDNS#accountId] <?xml version="1.0"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:v01="http://webservice.api.ultra.neustar.com/v01/">
  <soapenv:Header>
    <wsse:Security xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" soapenv:mustUnderstand="1">
      <wsse:UsernameToken>
        <wsse:Username>USERNAME</wsse:Username>
        <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">PASSWORD</wsse:Password>
      </wsse:UsernameToken>
    </wsse:Security>
  </soapenv:Header>
  <soapenv:Body>
    <v01:getAccountsListOfUser/>
  </soapenv:Body>
</soapenv:Envelope>
[UltraDNS#accountId] ---> END HTTP (704-byte body)
[UltraDNS#accountId] <--- HTTP/1.1 200 OK (728ms)
[UltraDNS#accountId] Content-Length: 399
[UltraDNS#accountId] Content-Type: text/xml;charset=ISO-8859-1
[UltraDNS#accountId] Server: Jetty(7.6.5.v20120716)
[UltraDNS#accountId]
[UltraDNS#accountId] <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><ns1:getAccountsListOfUserResponse xmlns:ns1="http://webservice.api.ultra.neustar.com/v01/"><AccountsList xmlns:ns2="http://schema.ultraservice.neustar.com/v01/"><ns2:AccountDetailsData accountID="12345678910" accountName="USERNAME"/></AccountsList></ns1:getAccountsListOfUserResponse></soap:Body></soap:Envelope>
[UltraDNS#accountId] <--- END HTTP (399-byte body)
--snip--
geo.denominator.io.
ultradnstest.denominator.io.
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
