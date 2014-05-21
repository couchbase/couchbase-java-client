package com.couchbase.client.java;

import com.couchbase.client.java.util.TestProperties;
import com.couchbase.client.java.view.Stale;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewRow;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observer;

import java.util.concurrent.CountDownLatch;

public class ViewTest {

  private static final String seedNode = TestProperties.seedNode();
  private static final String bucketName = TestProperties.bucket();
  private static final String password = TestProperties.password();

  private static Bucket bucket;

  @BeforeClass
  public static void connect() {
    CouchbaseCluster cluster = new CouchbaseCluster(seedNode);
    bucket = cluster
      .openBucket(bucketName, password)
      .toBlockingObservable()
      .single();
  }

  @Test
  public void shouldQueryView() throws Exception {
      while(true) {
          final CountDownLatch latch = new CountDownLatch(100);
          for (int i = 0; i < 100; i++) {
              bucket.query(ViewQuery.from("foo", "bar").stale(Stale.TRUE)).subscribe(new Observer<ViewRow>() {
                  @Override
                  public void onCompleted() {
                      latch.countDown();
                  }

                  @Override
                  public void onError(Throwable e) {
                      //System.out.println(e);
                  }

                  @Override
                  public void onNext(ViewRow viewRow) {
                      //System.out.println(viewRow.id());
                  }
              });
          }
          latch.await();
      }
  }
}
