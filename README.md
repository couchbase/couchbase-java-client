# Official Couchbase Java SDK

Welcome to the official Couchbase Java client library. If you want to use Couchbase
from the Java programming language, this is your one-stop-shop.

## Getting Started

### Getting the Dependencies
The package is available from Maven Central:

```xml
<dependencies>
    <dependency>
        <groupId>com.couchbase.client</groupId>
        <artifactId>java-client</artifactId>
        <version>2.1.1</version>
    </dependency>
</dependencies>
```

You can also [go here](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.couchbase.client%22) and download
the jars directly.

## Staying Updated, Asking Questions
Come by the [forums](https://forums.couchbase.com/c/java-sdk)! And be sure to check out our
[blog](https://blog.couchbase.com).

### Usage
Both synchronous and asynchronous (reactive, through [RxJava](https://github.com/ReactiveX/RxJava)) are supported.
The following code connects to `localhost`, opens the `default` bucket, stores a document, loads it again and prints
its content.

Here is the sync variation:

```java
// Create a cluster reference
CouchbaseCluster cluster = CouchbaseCluster.create("127.0.0.1");

// Connect to the bucket and open it
Bucket bucket = cluster.openBucket("default");

// Create a JSON document and store it with the ID "helloworld"
JsonObject content = JsonObject.create().put("hello", "world");
JsonDocument inserted = bucket.insert(JsonDocument.create("helloworld", content));

// Read the document and print the "hello" field
JsonDocument found = bucket.get("helloworld");
System.out.println("Couchbase is the best database in the " + found.content().getString("hello"));

// Close all buckets and disconnect
cluster.disconnect();
```


And here (one of the possible) async ones:

```java
JsonObject content = JsonObject.create().put("hello", "world");
bucket
    .async()
    .insert(JsonDocument.create("helloworld", content))
    .flatMap(new Func1<JsonDocument, Observable<JsonDocument>>() {
        @Override
        public Observable<JsonDocument> call(JsonDocument document) {
            return bucket.async().get(document);
        }
    })
    .map(new Func1<JsonDocument, String>() {
        @Override
        public String call(JsonDocument doc) {
            return doc.content().getString("hello");
        }
    })
    .subscribe(new Action1<String>() {
        @Override
        public void call(String s) {
            System.out.println("Couchbase is the best database in the " + s);
        }
    });
```

If you can already use Java 8, things get much nicer:

```java
JsonObject content = JsonObject.create().put("hello", "world");
bucket
    .async()
    .insert(JsonDocument.create("helloworld", content))
    .flatMap(document -> bucket.async().get(document))
    .map(doc -> doc.content().getString("hello"))
    .subscribe(s -> System.out.println("Couchbase is the best database in the " + s));

```

You can read more in the [documentation](http://docs.couchbase.com/).

## Contributing

### Building
We are utilizing the `publish` gradle plugin:

```
~/couchbase-java-client $ ./gradlew publishToMavenLocal
```

Make sure to do the same with [core-io](https://github.com/couchbase/couchbase-jvm-core) first!

### Running the Tests
The test suite is separated into unit, integration and performance tests. Each of those sets can and should be run
individually, depending on the type of testing needed. While unit and integration tests can be run from both the
command line and the IDE, it is recommend to run the performance tests from the command line only.

### Unit Tests
Unit tests do not need a Couchbase Server reachable, and they should complete within a very short time. They are
located under the `src/test` namespace and can be run directly from the IDE or through the `gradle test` command line:

```
~/couchbase-java-client $ ./gradlew test
...
:test

BUILD SUCCESSFUL
```

## Integration Tests
Those tests interact with Couchbase Server instances and therefore need to be configured as such. If you do not want
to change anything special, make sure you at least have one running on `localhost`. Then use the `gradle integrationTest`
command:

```
~/couchbase-java-client $ ./gradlew integrationTest
...
:integrationTest

BUILD SUCCESSFUL
```