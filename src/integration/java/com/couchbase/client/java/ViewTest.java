package com.couchbase.client.java;

import com.couchbase.client.java.query.Stale;
import com.couchbase.client.java.query.ViewQuery;
import com.couchbase.client.java.query.ViewResult;
import com.couchbase.client.java.util.ClusterDependentTest;
import com.couchbase.client.java.util.TestProperties;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observer;

import java.util.concurrent.CountDownLatch;

public class ViewTest extends ClusterDependentTest {


  @Test
  public void shouldQueryView() throws Exception {
      while(true) {
          final CountDownLatch latch = new CountDownLatch(100);
          for (int i = 0; i < 100; i++) {
              bucket().query(ViewQuery.from("foo", "bar").stale(Stale.TRUE)).subscribe(new Observer<ViewResult>() {
                  @Override
                  public void onCompleted() {
                      latch.countDown();
                  }

                  @Override
                  public void onError(Throwable e) {
                      //System.out.println(e);
                  }

                  @Override
                  public void onNext(ViewResult viewRow) {
                      //System.out.println(viewRow.id());
                  }
              });
          }
          latch.await();
      }
  }
}
