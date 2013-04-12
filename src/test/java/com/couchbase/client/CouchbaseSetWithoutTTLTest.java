package com.couchbase.client;

import com.couchbase.client.clustermanager.BucketType;
import net.spy.memcached.PersistTo;
import net.spy.memcached.TestConfig;
import net.spy.memcached.internal.OperationFuture;
import org.junit.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: tgrall
 * Date: 4/9/13
 * Time: 11:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class CouchbaseSetWithoutTTLTest {

    protected static TestingClient client = null;
    private static final String SERVER_URI = "http://" + TestConfig.IPV4_ADDR
            + ":8091/pools";

    public CouchbaseSetWithoutTTLTest() {
    }

    /**
     * Initialize the client connection.
     *
     * @throws Exception
     */
    protected static void initClient() throws Exception {
        List<URI> uris = new LinkedList<URI>();
        uris.add(URI.create(SERVER_URI));
        client = new TestingClient(uris, "default", "");
    }

    @Before
    public void setUp() throws Exception {
        BucketTool bucketTool = new BucketTool();
        bucketTool.deleteAllBuckets();
        bucketTool.createDefaultBucket(BucketType.COUCHBASE, 256, 0, true);

        BucketTool.FunctionCallback callback = new BucketTool.FunctionCallback() {
            @Override
            public void callback() throws Exception {
                initClient();
            }

            @Override
            public String success(long elapsedTime) {
                return "Client Initialization took " + elapsedTime + "ms";
            }
        };
        bucketTool.poll(callback);
        bucketTool.waitForWarmup(client);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSimpleSetWithNoTTLNoDurability() throws Exception {
        String jsonValue = "{\"name\":\"This is a test with no TTL\"}";
        String jsonValue2 = "NewValue";
        String key001 = "key:001";

        // test simple values
        OperationFuture op =  client.set(key001, jsonValue);
        assertTrue( op.getStatus().isSuccess() );
        assertEquals(client.get(key001), jsonValue);
        client.delete(key001);

        // test replace that should fail
        op =  client.replace(key001, jsonValue2);
        assertFalse(op.getStatus().isSuccess());

        // test add
        op =  client.add(key001, jsonValue);
        assertTrue( op.getStatus().isSuccess() );
        assertEquals(client.get(key001),jsonValue);

        // test add
        op =  client.replace(key001, jsonValue2);
        assertTrue( op.getStatus().isSuccess() );
        assertEquals(client.get(key001),jsonValue2);

        // test add
        op =  client.add(key001, jsonValue);
        assertFalse( op.getStatus().isSuccess() );

        client.delete(key001);
    }

}
