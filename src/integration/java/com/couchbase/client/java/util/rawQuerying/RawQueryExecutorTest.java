package com.couchbase.client.java.util.rawQuerying;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import com.couchbase.client.deps.com.fasterxml.jackson.core.JsonProcessingException;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.JsonNode;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.TranscodingException;
import com.couchbase.client.java.query.N1qlMetrics;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.couchbase.client.java.transcoder.JacksonTransformers;
import com.couchbase.client.java.transcoder.TranscoderUtils;
import com.couchbase.client.java.util.CouchbaseTestContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.functions.Func1;

public class RawQueryExecutorTest {

    private static CouchbaseTestContext ctx;
    private static RawQueryExecutor rawQueryExecutor;

    @BeforeClass
    public static void init() throws InterruptedException {
        ctx = CouchbaseTestContext.builder()
                .bucketName("RawQueryExecutor")
                .bucketPassword("foo")
                .adhoc(true)
                .bucketQuota(100)
                .build()
        .ensurePrimaryIndex();

        ctx.bucket().upsert(JsonDocument.create("test1", JsonObject.create().put("item", "value")));
        ctx.bucket().upsert(JsonDocument.create("test2", JsonObject.create().put("item", 123)));

        AsyncRawQueryExecutor asyncExecutor = new AsyncRawQueryExecutor(ctx.bucketName(), ctx.bucketPassword(), ctx.cluster().core());
        rawQueryExecutor = new RawQueryExecutor(asyncExecutor, ctx.env());
    }

    @AfterClass
    public static void cleanup() {
        ctx.destroyBucketAndDisconnect();
    }


    @Test
    public void testN1qlToJsonObject() throws Exception {
        N1qlQuery query = N1qlQuery.simple("SELECT * FROM `" + ctx.bucketName() + "`",
                N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS).withContextId("foo"));

        JsonObject result = rawQueryExecutor.n1qlToJsonObject(query);

        System.out.println(result);

        assertThat(result).isNotNull();
        assertThat(result.getNames()).containsOnly("requestID", "clientContextID", "signature", "results", "status", "metrics");
        assertThat(result.getString("requestID")).isNotEmpty();
        assertThat(result.getString("clientContextID")).isEqualTo("foo");
        assertThat(result.get("signature"))
            .isNotNull()
            .isInstanceOf(JsonObject.class);
        assertThat(result.getArray("results")).isNotEmpty().hasSize(2);
        assertThat(result.getArray("errors")).isNull();
        assertThat(result.getArray("warnings")).isNull();
        assertThat(result.getString("status")).isEqualTo("success");
        assertThat(result.getObject("metrics").toMap()).isNotEmpty();
    }

    @Test
    public void testN1qlToRawJson() throws Exception {
        N1qlQuery query = N1qlQuery.simple("SELECT * FROM `" + ctx.bucketName() + "`",
                N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS).withContextId("foo"));

        String json = rawQueryExecutor.n1qlToRawJson(query);

        assertThat(json).isNotNull().isNotEmpty();

        JsonObject result = JsonObject.fromJson(json);

        assertThat(result.getNames()).containsOnly("requestID", "clientContextID", "signature", "results", "status", "metrics");
        assertThat(result.getString("requestID")).isNotEmpty();
        assertThat(result.getString("clientContextID")).isEqualTo("foo");
        assertThat(result.get("signature"))
            .isNotNull()
            .isInstanceOf(JsonObject.class);
        assertThat(result.getArray("results")).isNotEmpty().hasSize(2);
        assertThat(result.getArray("errors")).isNull();
        assertThat(result.getArray("warnings")).isNull();
        assertThat(result.getString("status")).isEqualTo("success");
        assertThat(result.getObject("metrics").toMap()).isNotEmpty();
    }

    @Test
    public void testN1qlToRawCustom() throws Exception {
         N1qlQuery query = N1qlQuery.simple("SELECT * FROM `" + ctx.bucketName() + "`",
                N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS).withContextId("foo"));

        Map result = rawQueryExecutor.n1qlToRawCustom(query,
                new Func1<TranscoderUtils.ByteBufToArray, Map>() {
                    @Override
                    public Map call(TranscoderUtils.ByteBufToArray byteBufToArray) {
                        ByteArrayInputStream bis = new ByteArrayInputStream(byteBufToArray.byteArray, byteBufToArray.offset, byteBufToArray.length);
                        try {
                            JsonNode node = JacksonTransformers.MAPPER.readTree(bis);
                            return JacksonTransformers.MAPPER.readValue(node.at("/metrics").traverse(), Map.class);
                        } catch (JsonProcessingException e) {
                            throw new TranscodingException("", e);
                        } catch (IOException e) {
                            throw new TranscodingException("", e);
                        } finally {
                            try {
                                bis.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        assertThat(result)
                .hasSize(4)
                .containsOnlyKeys("elapsedTime", "executionTime", "resultCount", "resultSize")
                .containsEntry("resultCount", 2);
    }
}