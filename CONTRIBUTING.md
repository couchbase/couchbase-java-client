# Contributing

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