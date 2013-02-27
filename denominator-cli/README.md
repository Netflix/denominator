# Denominator CLI

The denominator CLI is a git-like-cli based on the [airline](https://github.com/airlift/airline) project.  It is packaged as a [really executable jar](http://skife.org/java/unix/2011/06/20/really_executable_jars.html) which means you can do `./denominator` without any of the `java -jar` stuff.

## Building
To build the cli, execute `./gradlew clean test install` from the root of your denominator clone.  The binary will end up at `/denominator-cli/build/denominator`

## Running
denominator will print out a help statement, but here's the gist.

### Providers
Execute `./denominator providers`  The output will tell you what credentials are needed for the provider.  Here's an example.

```
provider	credential type	credential parameters
mock	
dynect	password	customer username password
ultradns	password	username password
route53	accessKey	accessKey secretKey
route53	session	accessKey secretKey sessionToken
```

The first field says the type, if any.  If there's no type listed, it needs no credentials.  If there is a type listed, the following fields are credential args.  Say for example, you were using `ultradns`.  

```
ultradns        password        username password
```
This says the provider `ultradns` supports `password` authentication, which needs two `-c` parameters: `username` and `password`.  To put it together, you'd specify the following to do a zone list:
```
./denominator -p ultradns -c myusername -c mypassword zone list
```
### Zones
The only command yet implemented is zone list, and this returns the zones in your account.  Ex.
```
./denominator -p route53 -c access -c secret zone list
```
## Hooks
### IAM Instance Profile
If you are using the `route53` provider on an ec2 instance with a profile associated with it, you don't need to pass credentials.  The syntax would end up like this.
```
./denominator -p route53 zone list
```
