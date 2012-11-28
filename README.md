# Couchbase Java Client Library

This is the official client library for use with Couchbase Server.

## SUPPORT

If you found an issue, please file it in our [JIRA][1]. Also you are
always welcome on `#libcouchbase` channel at [freenode.net IRC servers][2].

Documentation: [http://www.couchbase.com/docs/](http://www.couchbase.com/docs/)

## INSTALL

The library may be installed either through maven or through standalone
jar files.  See the [main website][3] for details.

## USING

A simple creation of a client may be done like so:

    List<URI> baseList = Arrays.asList(
      URI.create("http://192.168.0.1:8091/pools"),
      URI.create("http://192.168.0.2:8091/pools"));
    CouchbaseClient client = new CouchbaseClient(baseList, "default", "")

See the [documentation][5] on the site for more usage details, including
a getting started guide and a tutorial.

## PREVIEWS

Note that this 1.1 preview has support for Couchbase Views which are
new to Couchbase Server 2.0. This portion of the API is still being
developed and undergoing changes. See the [preview page][4] on the
website for more information on obtaining and using the 1.1 previews
or look to the [documentation][5] on the Couchbase website.  Source
code repositories be found in the master branch [on github][6].

[1]: http://couchbase.com/issues/browse/JCBC
[2]: http://freenode.net/irc_servers.shtml
[3]: http://www.couchbase.com/develop/java/current
[4]: http://www.couchbase.com/develop/java/next
[5]: http://www.couchbase.com/docs/couchbase-sdk-java-1.0/index.html
[6]: http://github.com/couchbase/couchbase-java-client
