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

Note that the 1.0 version does not support Couchbase Views.  Suppport
for views is in version 1.1 and later.  See the [preview page][4] on
the website for more information on obtaining the 1.1 client.  It may
also be found in the master branch (as of this writing) on github.

[1]: http://couchbase.com/issues/browse/JCBC
[2]: http://freenode.net/irc_servers.shtml
[3]: http://www.couchbase.com/develop/java/current
[4]: http://www.couchbase.com/develop/java/next
[5]: http://www.couchbase.com/docs/couchbase-sdk-java-1.0/index.html
