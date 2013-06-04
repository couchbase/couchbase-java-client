# Couchbase Java Client Library

This is the official Java Client Library for [Couchbase Server](http://www.couchbase.com/).


## Install

The library may be installed either through Apache Maven or through standalone Jar files (download the JAR from the [main website](http://www.couchbase.com/develop/java/current) ).

If you are using Apache Maven, add the following repository and depencendy in to your *pom.xml* file.

	<repositories>
    	<repository>
      		<id>couchbase</id>
      		<name>Couchbase Maven Repository</name>
      		<layout>default</layout>
      		<url>http://files.couchbase.com/maven2/</url>
      		<snapshots>
        		<enabled>false</enabled>
      		</snapshots>
    	</repository>
  	</repositories>
  	
  	<dependencies>
  		<dependency>
  			<groupId>couchbase</groupId>
			<artifactId>couchbase-client</artifactId>
			<version>1.1.7</version>
			<type>jar</type>
		</dependency>
    </dependencies>



See the [main website](http://www.couchbase.com/develop/java/current) for details.

## Using

A simple creation of a client may be done like so:

    List<URI> baseList = Arrays.asList(
      URI.create("http://192.168.0.1:8091/pools"),
      URI.create("http://192.168.0.2:8091/pools"));

    CouchbaseClient client = new CouchbaseClient(baseList, "default", "");
    
    OperationFuture<Boolean> setOp = client.set("key", "{\"name\":\"Couchbase\"}");
    
    client.shutdown(3, TimeUnit.SECONDS);


See the [documentation](http://www.couchbase.com/docs/couchbase-sdk-java-1.0/index.html) on the site for more usage details, including a getting started guide and a tutorial.

## Getting Help

For help with the Couchbase Java Client Library see the [Couchbase SDK Forums](http://www.couchbase.com/forums/sdks/sdks). Also you are
always welcome on `#libcouchbase` channel at [freenode.net IRC servers](http://freenode.net/irc_servers.shtml).

If you found an issue, please file it in our [JIRA](http://couchbase.com/issues/browse/JCBC).


Documentation: [http://www.couchbase.com/docs/](http://www.couchbase.com/docs/)

## Contributing
Contributions are welcome, see the [contributor guide](http://www.couchbase.com/wiki/display/couchbase/Contributing+Changes).
