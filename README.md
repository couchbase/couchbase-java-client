# Official Couchbase Java SDK

This project is the official driver for Couchbase when working with Java (or on the JVM). It provides management, CRUD and query facilities through both asynchronous and synchronous APIs.

## Features ##

* High-Performance Key/Value and Query (N1QL, Views) operations
* Cluster-Awareness and automatic rebalance and failover handling
* Asynchronous (through [RxJava](https://github.com/ReactiveX/RxJava)) and Synchronous APIs
* Transparent Encryption Support
* Cluster and Bucket level management facilities
* Complete non-blocking stack through [RxJava](https://github.com/ReactiveX/RxJava) and [Netty](http://netty.io)

## Getting Help ##
This README, as well as the [reference documentation](http://developer.couchbase.com/documentation/server/4.0/sdks/java-2.2/java-intro.html) are the best places to get started and dig deeper into the Couchbase SDK. In addition, you might want to look at our [travel-sample application](https://github.com/couchbaselabs/try-cb-java).

The primary way to ask questions is through our official [Forums](http://forums.couchbase.com), although there is also a [stackoverflow tag](http://stackoverflow.com/questions/tagged/couchbase). You can also ask questions on `#couchbase` or `#libcouchbase` on IRC (freenode). Please file any issues you find or enhancements you want to request against our [JIRA](http://issues.couchbase.com/browse/JCBC) which we use for universal issue tracking.

## Quick Start ##
The easiest way is to download the jar as well as its transitive dependencies (only 2) through maven:


```xml
<dependency>
    <groupId>com.couchbase.client</groupId>
    <artifactId>java-client</artifactId>
    <version>2.2.4</version>
</dependency>
```

You can find information to older versions as well as alternative downloads [here](http://developer.couchbase.com/documentation/server/4.0/sdks/java-2.2/download-links.html).

The following code connects to the `Cluster`, opens a `Bucket`, stores a `Document`, retreives it and prints out parts of the content.

```java
// Create a cluster reference
CouchbaseCluster cluster = CouchbaseCluster.create("127.0.0.1");

// Connect to the bucket and open it
Bucket bucket = cluster.openBucket("default");

// Create a JSON document and store it with the ID "helloworld"
JsonObject content = JsonObject.create().put("hello", "world");
JsonDocument inserted = bucket.upsert(JsonDocument.create("helloworld", content));

// Read the document and print the "hello" field
JsonDocument found = bucket.get("helloworld");
System.out.println("Couchbase is the best database in the " + found.content().getString("hello"));

// Close all buckets and disconnect
cluster.disconnect();
```

If you want to perform a N1QL query against [Couchbase Server 4.0](http://www.couchbase.com/nosql-databases/couchbase-server) or later, you can do it like this:

```java
N1qlQueryResult query = bucket.query(N1qlQuery.simple("SELECT DISTINCT(country) FROM `travel-sample` WHERE type = 'airline' LIMIT 10"));

for (N1qlQueryRow row : query) {
    System.out.println(row.value());
}
```

This prints out the distinct countries for all airlines stored in the `travel-sample` bucket that comes with the server.

If you want to learn more, check out the [Getting Started](http://developer.couchbase.com/documentation/server/4.0/sdks/java-2.2/hello-couchbase.html) section in the official documentation.

## Contributing ##

We've decided to use "gerrit" for our code review system, making it
easier for all of us to contribute with code and comments.

  1. Visit http://review.couchbase.org and "Register" for an account
  2. Review http://review.couchbase.org/static/individual_agreement.html
  3. Agree to agreement by visiting http://review.couchbase.org/#/settings/agreements
  4. If you do not receive an email, please contact us
  5. Check out the java client area http://review.couchbase.org/#/q/status:open+project:couchbase-java-client,n,z
  6. Join us on IRC at #libcouchbase on Freenode :-)

Note that to build `SNAPSHOT` versions of the `java-client` you also need to build the `core-io` package on which it depends. Both use maven to package and install. The same process as above applies for the [core-io](https://github.com/couchbase/couchbase-jvm-core) package.

After you've checked out both projects you can build and install them as follows:

```
┌─[michael@daschlbase]─[~/code/couchbase/couchbase-jvm-core]
└──╼ mvn clean install
**SNIP**
[INFO] --- maven-install-plugin:2.4:install (default-install) @ core-io ---
[INFO] Installing /Users/michaelnitschinger/code/couchbase/couchbase-jvm-core/target/core-io-1.2.1-SNAPSHOT.jar to /Users/michaelnitschinger/.m2/repository/com/couchbase/client/core-io/1.2.1-SNAPSHOT/core-io-1.2.1-SNAPSHOT.jar
[INFO] Installing /Users/michaelnitschinger/code/couchbase/couchbase-jvm-core/dependency-reduced-pom.xml to /Users/michaelnitschinger/.m2/repository/com/couchbase/client/core-io/1.2.1-SNAPSHOT/core-io-1.2.1-SNAPSHOT.pom
[INFO] Installing /Users/michaelnitschinger/code/couchbase/couchbase-jvm-core/target/core-io-1.2.1-SNAPSHOT-sources.jar to /Users/michaelnitschinger/.m2/repository/com/couchbase/client/core-io/1.2.1-SNAPSHOT/core-io-1.2.1-SNAPSHOT-sources.jar
[INFO] Installing /Users/michaelnitschinger/code/couchbase/couchbase-jvm-core/target/core-io-1.2.1-SNAPSHOT-javadoc.jar to /Users/michaelnitschinger/.m2/repository/com/couchbase/client/core-io/1.2.1-SNAPSHOT/core-io-1.2.1-SNAPSHOT-javadoc.jar
[INFO] Installing /Users/michaelnitschinger/code/couchbase/couchbase-jvm-core/target/core-io-1.2.1-SNAPSHOT-sources.jar to /Users/michaelnitschinger/.m2/repository/com/couchbase/client/core-io/1.2.1-SNAPSHOT/core-io-1.2.1-SNAPSHOT-sources.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 52.676 s
[INFO] Finished at: 2015-10-12T07:18:50+02:00
[INFO] Final Memory: 36M/337M
[INFO] ------------------------------------------------------------------------
```

Next, the exact steps apply for the  `java-client`.

Note that installing includes running the tests, which require you to run a local Couchbase Server 4.0 or later instance. If you want to avoid building the tests over and over again, you can add the `-Dmaven.test.skip` flag to the command line. If you only want to run the unit tests (also no server required for them), use the `-Dunit` flag (recommended over skipping the tests entirely).

Finally, feel free to reach out to the maintainers over the forums, IRC or email if you have further questions on contributing or get stuck along the way. We love contributions and want to help you get your change over the finish line - and you mentioned in the release notes!
