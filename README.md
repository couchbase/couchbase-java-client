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
This README, as well as the [reference documentation](https://developer.couchbase.com/documentation/server/5.0/sdk/java/start-using-sdk.html) are the best places to get started and dig deeper into the Couchbase SDK. In addition, you might want to look at our [travel-sample application](https://github.com/couchbaselabs/try-cb-java).

The primary way to ask questions is through our official [Forums](http://forums.couchbase.com), although there is also a [stackoverflow tag](http://stackoverflow.com/questions/tagged/couchbase). You can also ask questions on `#couchbase` or `#libcouchbase` on IRC (freenode). Please file any issues you find or enhancements you want to request against our [JIRA](http://issues.couchbase.com/browse/JCBC) which we use for universal issue tracking.

## Quick Start ##
The easiest way is to download the jar as well as its transitive dependencies (only 2) through maven:


```xml
<dependency>
    <groupId>com.couchbase.client</groupId>
    <artifactId>java-client</artifactId>
    <version>2.7.2</version>
</dependency>
```

You can find information to older versions as well as alternative downloads [here](http://developer.couchbase.com/server/other-products/release-notes-archives/java-sdk).

The following code connects to the `Cluster`, opens a `Bucket`, stores a `Document`, retrieves it and prints out parts of the content.

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
N1qlQueryResult result = bucket.query(N1qlQuery.simple("SELECT DISTINCT(country) FROM `travel-sample` WHERE type = 'airline' LIMIT 10"));

for (N1qlQueryRow row : result) {
    System.out.println(row.value());
}
```

This prints out the distinct countries for all airlines stored in the `travel-sample` bucket that comes with the server.

If you want to learn more, check out the [Start Using the SDK](https://developer.couchbase.com/documentation/server/5.0/sdk/java/start-using-sdk.html) section in the official documentation.

## Contributing ##

We use Gerrit for our code review system. Please have a look at the extensive [`CONTRIBUTING.md`](CONTRIBUTING.md) for more details.

Feel free to reach out to the maintainers over the forums, IRC or email if you have further questions on contributing or get stuck along the way. We love contributions and want to help you get your change over the finish line - and you mentioned in the release notes!
