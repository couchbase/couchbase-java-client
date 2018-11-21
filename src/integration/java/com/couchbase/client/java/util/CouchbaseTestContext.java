/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.java.util;

import static org.junit.Assume.assumeFalse;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.ServiceNotAvailableException;
import com.couchbase.client.core.logging.CouchbaseLogger;
import com.couchbase.client.core.logging.CouchbaseLoggerFactory;
import com.couchbase.client.core.message.internal.PingReport;
import com.couchbase.client.core.message.internal.PingServiceHealth;
import com.couchbase.client.deps.io.netty.util.ResourceLeakDetector;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.error.AuthenticationException;
import com.couchbase.client.java.error.BucketDoesNotExistException;
import com.couchbase.client.java.error.IndexDoesNotExistException;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import com.couchbase.client.java.repository.Repository;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.result.SearchQueryResult;
import com.couchbase.client.java.util.features.CouchbaseFeature;
import com.couchbase.client.java.util.features.Version;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import com.couchbase.mock.BucketConfiguration;
import com.couchbase.mock.CouchbaseMock;
import com.couchbase.mock.JsonUtils;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;

/**
 * An helper class for integration tests that defaults to values from {@link TestProperties}
 * but can be overridden on a case by case basis. Use the {@link #builder()} to initialize
 * the context in a JUnit {@link BeforeClass} annotated method, then get the SDK components
 * you need for your tests from this context (eg. {@link #bucket()}).
 *
 * You can have the test context create an adhoc bucket for you ({@link Builder#adhoc(boolean)},
 * in which case you should set a low quota ({@link Builder#bucketQuota(int)} of 100) and call
 * {@link #destroyBucketAndDisconnect()} in a {@link AfterClass} annotated method.
 *
 * @author Simon Baslé
 * @since 2.2
 */
public class CouchbaseTestContext {

    private static final CouchbaseLogger LOGGER = CouchbaseLoggerFactory.getInstance(CouchbaseTestContext.class);

    public static final String AD_HOC = "adHoc_";

    static {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    }

    private static Properties mockProperties = loadProperties();

    private final Bucket bucket;
    private final String bucketPassword;
    private final BucketManager bucketManager;
    private final Cluster cluster;
    private final ClusterManager clusterManager;
    private final String seedNode;
    private final String adminName;
    private final String adminPassword;
    private final CouchbaseEnvironment env;
    private final String bucketName;
    private final boolean isAdHoc;
    private final boolean isFlushEnabled;
    private final Repository repository;
    private final boolean rbacEnabled;
    public final CouchbaseMock mock;

    private CouchbaseTestContext(Bucket bucket, String bucketPassword,
            BucketManager bucketManager, Cluster cluster, ClusterManager clusterManager, String seedNode,
            String adminName, String adminPassword, CouchbaseEnvironment env, boolean isAdHoc, boolean isFlushEnabled,
        boolean rbacEnabled, CouchbaseMock mock) {
        this.bucket = bucket;
        this.bucketName = bucket.name();
        this.bucketPassword = bucketPassword;
        this.bucketManager = bucketManager;
        this.cluster = cluster;
        this.clusterManager = clusterManager;
        this.seedNode = seedNode;
        this.adminName = adminName;
        this.adminPassword = adminPassword;
        this.env = env;
        this.isAdHoc = isAdHoc;
        this.isFlushEnabled = isFlushEnabled;
        this.repository = bucket.repository();
        this.rbacEnabled = rbacEnabled;
        this.mock = mock;
    }


    /**
     * @return a {@link Builder} for a new {@link CouchbaseTestContext}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * If N1QL is available (detected or forced), this method will attempt to create a PRIMARY INDEX on the bucket.
     * It will ignore an already existing primary index. If other N1QL errors arise, a {@link CouchbaseException} will
     * be thrown (with the message containing the list of errors).
     */
    public CouchbaseTestContext ensurePrimaryIndex() throws Exception {
        //test for N1QL
        if (clusterManager.info().checkAvailable(CouchbaseFeature.N1QL)) {
            N1qlQueryResult result = bucket().query(
                    N1qlQuery.simple("CREATE PRIMARY INDEX ON `" + bucketName() + "`",
                            N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS)), 5, TimeUnit.MINUTES);

            if (!result.finalSuccess()) {
                //ignore "index already exist"
                for (JsonObject error : result.errors()) {
                    assumeFalse(error.getString("msg").contains("concurrent"));
                    assumeFalse(error.getString("msg").contains("Request timed out"));
                    if (!error.getString("msg").contains("already exist")) {
                        throw new CouchbaseException("Could not CREATE PRIMARY INDEX - " + result.errors().toString());
                    }
                }
            }
        }
        return this;
    }

    /**
     * Builder for a {@link CouchbaseTestContext} that allows you to set all the options for
     * creating a tailored integration test environment. Default values will be taken from {@link TestProperties}
     * and, if it doesn't exist, the requested bucket will be created as a {@link BucketType#COUCHBASE} with a
     * {@link #bucketType(BucketType) bucket} with {@link #bucketQuota(int) memory quota} of 256MB and
     * {@link #flushOnInit(boolean) flush} enabled.
     */
    public static final class Builder {

        private boolean createAdhocBucket;
        private boolean createIfMissing;
        private String seedNode;
        private String adminName;
        private String adminPassword;
        private DefaultCouchbaseEnvironment.Builder envBuilder;
        private String bucketName;
        private String bucketPassword;
        private DefaultBucketSettings.Builder bucketSettingsBuilder;
        private boolean flushOnInit;
        private CouchbaseMock couchbaseMock;


        public Builder() {
            seedNode = TestProperties.seedNode();
            adminName = TestProperties.adminName();
            adminPassword = TestProperties.adminPassword();
            envBuilder = DefaultCouchbaseEnvironment.builder();
            bucketName = TestProperties.bucket();
            bucketPassword = TestProperties.password();
            bucketSettingsBuilder = DefaultBucketSettings
                .builder()
                .quota(100)
                .enableFlush(true)
                .type(BucketType.COUCHBASE);
            flushOnInit = true;
            createIfMissing = true;
            this.createAdhocBucket = false;
        }

        /**
         * Set adhoc to true to force creation of a bucket for the duration of the test case (the name will be
         * prefixed by "{@value CouchbaseTestContext#AD_HOC}" and suffixed with a random number). The bucket won't be flushed as it is brand new.
         *
         * Don't forget to clean it up at the end, eg. using {@link CouchbaseTestContext#destroyBucketAndDisconnect()}.
         */
        public Builder adhoc(boolean isAdhoc) {
            this.createAdhocBucket = isAdhoc;
            this.flushOnInit = false;
            return this;
        }

        /**
         * Toggles creation of missing buckets on or off. If disabled and the bucket is actually missing, a
         * {@link BucketDoesNotExistException} will be thrown when building the context.
         *
         * @param createIfMissing should missing buckets be created using context bucket settings?
         */
        public Builder createBucketIfMissing(boolean createIfMissing) {
            this.createIfMissing = createIfMissing;
            return this;
        }

        /**
         * Changes the seed node used for {@link Cluster} creation.
         */
        public Builder seedNode(String seedNode) {
            this.seedNode = seedNode;
            return this;
        }

        /**
         * Changes the administrator name used for {@link Cluster} and {@link ClusterManager} creation.
         */
        public Builder adminName(String adminName) {
            this.adminName = adminName;
            return this;
        }

        /**
         * Changes the administrator password used for {@link Cluster} and {@link ClusterManager} creation.
         */
        public Builder adminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
            return this;
        }

        /**
         * Forces an environment configuration to be used.
         */
        public Builder withEnv(DefaultCouchbaseEnvironment.Builder envBuilder) {
            this.envBuilder = envBuilder;
            return this;
        }

        /**
         * Changes the bucket name that will be provided by this context. Note that the name could vary if
         * {@link #adhoc(boolean)} is true.
         */
        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        /**
         * Changes the bucket password used in opening the context's bucket.
         */
        public Builder bucketPassword(String bucketPassword) {
            this.bucketPassword = bucketPassword;
            return this;
        }

        /**
         * Use a sample bucket. Doing so implies that no password is used, and the
         * bucket won't be created if missing (instead leading to an error). It
         * also forces flushOnInit and adhoc to false.
         *
         * @param sampleName the name of the sample bucket to use.
         */
        public Builder sampleBucket(String sampleName) {
            bucketName(sampleName);
            bucketPassword("");
            flushOnInit(false);
            adhoc(false);
            createBucketIfMissing(false);
            return this;
        }

        /**
         * Changes the bucket RAM quota used if the bucket needs to be created (it doesn't exist or adhoc was used).
         */
        public Builder bucketQuota(int quota) {
            this.bucketSettingsBuilder.quota(quota);
            return this;
        }

        /**
         * Changes the bucket type used if the bucket needs to be created (it doesn't exist or adhoc was used).
         */
        public Builder bucketType(BucketType type) {
            this.bucketSettingsBuilder.type(type);
            return this;
        }

        /**
         * Changes the configured number of replicas if the bucket needs to be created (it doesn't exist or adhoc was used).
         */
        public Builder bucketReplicas(int replicas) {
            this.bucketSettingsBuilder.replicas(replicas);
            return this;
        }

        /**
         * Set to true to activate a flush upon building the context, unless the bucket was not previously existing
         * or flush is disabled on the bucket.
         */
        public Builder flushOnInit(boolean flushOnInit) {
            this.flushOnInit = flushOnInit;
            return this;
        }

        /**
         * Set to false deactivates flush capabilities on the bucket.
         */
        public Builder enableFlush(boolean enableFlush) {
            this.bucketSettingsBuilder.enableFlush(enableFlush);
            return this;
        }

        /**
         * Build the {@link CouchbaseTestContext}, triggering potential creation of a bucket, flush of a bucket, etc...
         * (see {@link #adhoc(boolean)}, {@link #flushOnInit(boolean)}, ...).
         */
        public CouchbaseTestContext build() {
            if (createAdhocBucket) {
                this.bucketName = AD_HOC + this.bucketName + System.nanoTime();
            }

            loadProperties();

            if (isMockEnabled()) {
                createMock();
                int httpBootstrapPort = this.couchbaseMock.getHttpPort();
                try {
                    int carrierBootstrapPort = getCarrierPortInfo(httpBootstrapPort);
                    envBuilder
                            .bootstrapHttpDirectPort(httpBootstrapPort)
                            .bootstrapCarrierDirectPort(carrierBootstrapPort)
                            .connectTimeout(30000);
                } catch (Exception ex) {
                    throw new RuntimeException("Unable to get port info" + ex.getMessage(), ex);
                }
            }
            CouchbaseEnvironment env = envBuilder.build();

            Cluster cluster = CouchbaseCluster.create(env, seedNode);
            Version min = cluster.clusterManager(adminName, adminPassword).info().getMinVersion();
            boolean authed = false;
            if (min.major() >= 5) {
                cluster.authenticate(adminName, adminPassword);
                authed = true;
            }
            return buildWithCluster(cluster, env, authed);
        }

        protected int getCarrierPortInfo(int httpPort) throws Exception {
            URIBuilder builder = new URIBuilder();
            builder.setScheme("http").setHost("localhost").setPort(httpPort).setPath("mock/get_mcports")
                    .setParameter("bucket", this.bucketName);
            HttpGet request = new HttpGet(builder.build());
            HttpClient client = HttpClientBuilder.create().build();
            HttpResponse response = client.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status < 200 || status > 300) {
                throw new ClientProtocolException("Unexpected response status: " + status);
            }
            String rawBody = EntityUtils.toString(response.getEntity());
            com.google.gson.JsonObject respObject = JsonUtils.GSON.fromJson(rawBody, com.google.gson.JsonObject.class);
            com.google.gson.JsonArray portsArray = respObject.getAsJsonArray("payload");
            return portsArray.get(0).getAsInt();
        }

        protected void createMock() {
            int nodeCount = Integer.parseInt(mockProperties.getProperty("mock.nodeCount", "1"));
            int replicaCount = Integer.parseInt(mockProperties.getProperty("mock.replicaCount", "1"));
            String bucketType = mockProperties.getProperty("mock.bucketType", "couchbase");

            BucketConfiguration bucketConfiguration = new BucketConfiguration();
            bucketConfiguration.numNodes = nodeCount;
            bucketConfiguration.numReplicas = replicaCount;
            bucketConfiguration.numVBuckets = 1024;
            bucketConfiguration.name = this.bucketName;
            bucketConfiguration.type =  bucketType.compareToIgnoreCase("couchbase") == 0 ? com.couchbase.mock.Bucket.BucketType.COUCHBASE: com.couchbase.mock.Bucket.BucketType.MEMCACHED;
            bucketConfiguration.password = this.bucketPassword;
            ArrayList<BucketConfiguration> configList = new ArrayList<BucketConfiguration>();
            configList.add(bucketConfiguration);
            try {
                this.couchbaseMock = new CouchbaseMock(0, configList);
                this.couchbaseMock.start();
                this.couchbaseMock.waitForStartup();
            } catch (Exception ex) {
                throw new RuntimeException("Unable to initialize mock" + ex.getMessage(), ex);
            }
        }

        /**
         * Build the {@link CouchbaseTestContext}, triggering potential creation of a bucket, flush of a bucket, etc...
         * (see {@link #adhoc(boolean)}, {@link #flushOnInit(boolean)}, ...), but re-using a previously existing
         * {@link Cluster} and {@link CouchbaseEnvironment}.
         */
        public CouchbaseTestContext buildWithCluster(Cluster cluster, CouchbaseEnvironment env, boolean authed) {
            if (createAdhocBucket) {
                this.bucketName = AD_HOC + this.bucketName + System.nanoTime();
            }

            this.bucketSettingsBuilder = bucketSettingsBuilder.name(this.bucketName)
                    .password(this.bucketPassword);

            boolean existing = true;
            ClusterManager clusterManager = cluster.clusterManager(adminName, adminPassword);

            Bucket bucket;

            if(!isMockEnabled()) {
                existing = clusterManager.hasBucket(bucketName);
                if (!existing) {
                    if (createIfMissing) {
                        clusterManager.insertBucket(bucketSettingsBuilder.build());
                        boolean canUseBucket = false;
                        do {
                            try {
                                bucket = authed ? cluster.openBucket(bucketName) :
                                        cluster.openBucket(bucketName, bucketPassword);
                                PingReport pingReport = bucket.ping();
                                for (PingServiceHealth health:pingReport.services()) {
                                    if (health.state() != PingServiceHealth.PingState.OK) {
                                        throw new Exception("Not healthy");
                                    }
                                }
                                bucket.upsert(JsonDocument.create(bucketName + "foo"));
                                bucket.remove(bucketName + "foo");
                                canUseBucket = true;
                            } catch (Exception e) {
                                LOGGER.info("Unable to open/use bucket " + e.toString());
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException iex) {
                                    Thread.currentThread().interrupt();
                                    throw new RuntimeException(iex);
                                }
                            }
                        } while (!canUseBucket);
                    } else {
                        throw new BucketDoesNotExistException("Bucket " + bucketName + " doesn't exist and bucket creation disabled (or a sample)");
                    }
                }
            }

            boolean isFlushEnabled = bucketSettingsBuilder.enableFlush();

            bucket = authed ? cluster.openBucket(bucketName) :
                cluster.openBucket(bucketName, bucketPassword);
            BucketManager bucketManager = bucket.bucketManager();

            if (flushOnInit && isFlushEnabled && existing) {
                for (int i = 0; i < 5; i++) {
                    try {
                        bucketManager.flush();
                        break;
                    } catch (CouchbaseException ex) {
                        // because of a server bug, retry couple times
                        if (ex.getMessage() != null && ex.getMessage().contains("Flush failed with unexpected error")) {
                            continue;
                        } else {
                            break;
                        }
                    }
                }
            }

            return new CouchbaseTestContext(bucket, bucketPassword, bucketManager, cluster, clusterManager, seedNode, adminName, adminPassword, env, createAdhocBucket, isFlushEnabled, authed, couchbaseMock);
        }
    }

    //==========================
    //== Lifecycle Management ==
    //==========================

    /**
     * Trigger a flush of the context's bucket.
     */
    public void flush() {
        if (isFlushEnabled) {
            bucketManager.flush();
        }
    }

    /**
     * If N1QL is available in this context, issue a DELETE ALL query.
     */
    public void deleteAll() {
        //test for N1QL
        if (clusterManager.info().checkAvailable(CouchbaseFeature.N1QL)) {
            N1qlQueryResult result = bucket.query(N1qlQuery.simple("DELETE FROM `" + bucketName + "`"));
            if (!result.finalSuccess()) {
                throw new CouchbaseException("Could not DELETE ALL - " + result.errors().toString());
            }
        }
    }

    /**
     * Remove the bucket (if it was adhoc).
     */
    public void destroyBucket() {
        if (isAdHoc) {
            if (!bucket.isClosed()) {
                bucket.close();
            }
            clusterManager.removeBucket(bucketName);
        }
    }

    /**
     * Remove the bucket (if it was adhoc) and disconnect from the cluster.
     */
    public void destroyBucketAndDisconnect() {
        destroyBucket();
        disconnect();
        if (mock != null) {
            this.mock.stop();
        }
    }

    /**
     * Disconnect from the cluster.
     */
    public void disconnect() {
        cluster.disconnect();
    }

    //=====================
    //== Utility Methods ==
    //=====================

    /**
     * By calling this in @BeforeClass, tests will be skipped if N1QL is unavailable and is not forced on the env.
     */
    public CouchbaseTestContext ignoreIfNoN1ql() {
        return ignoreIfMissing(CouchbaseFeature.N1QL, false);
    }

    /**
     * By calling this in @BeforeClass with a {@link CouchbaseFeature},
     * tests will be skipped if said feature is not available on the cluster.
     *
     * @param feature the feature to check for.
     */
    public CouchbaseTestContext ignoreIfMissing(CouchbaseFeature feature) {
        return ignoreIfMissing(feature, false);
    }

    /**
     * By calling this in @BeforeClass with a {@link CouchbaseFeature},
     * tests will be skipped if said feature is not available on the cluster, unless forced is set to true.
     *
     * @param feature the feature to check for.
     * @param forced if true, always consider the feature available.
     */
    public CouchbaseTestContext ignoreIfMissing(CouchbaseFeature feature, boolean forced) {
        Assume.assumeTrue("Feature " + feature + " not available and not forced", isMockEnabled() || forced || clusterManager.info().checkAvailable(feature));
        return this;
    }

    /**
     * By calling this in @BeforeClass with a {@link Version},
     * tests will be skipped is all nodes in the cluster are not above
     * or at that version.
     *
     * @param minimumVersion the required version to check for.
     */
    public CouchbaseTestContext ignoreIfClusterUnder(Version minimumVersion) {
        Assume.assumeTrue("Cluster is under " + minimumVersion, isMockEnabled() || clusterManager().info().getMinVersion().compareTo(minimumVersion) >= 0);
        return this;
    }

    /**
     * Check if search service exists in @BeforeClass
     * tests will be skipped if the service is not found
     */
    public CouchbaseTestContext ignoreIfSearchServiceNotFound() {
        try {
            this.bucket().query(new SearchQuery(this.bucketName, SearchQuery.matchPhrase("deadbeef")));
        } catch (Exception ex) {
            Assume.assumeTrue("Query service not available", (ex instanceof ServiceNotAvailableException) == false);
        }
        return this;
    }

    public CouchbaseTestContext ignoreIfSearchIndexDoesNotExist(String idxname) {
        SearchQueryResult result = bucket.query(
            new SearchQuery(idxname, SearchQuery.queryString("test")).limit(1)
        );
        if (!result.status().isSuccess()) {
            try {
                result.hitsOrFail();
            } catch (IndexDoesNotExistException ex) {
                Assume.assumeTrue("FTS Index \"" + idxname + "\" not available.", false);
            }
        }
        return this;
    }


    /**
     * Utility method to get a meaningful test fail message out of a {@link N1qlQueryResult}'s {@link N1qlQueryResult#errors()} list.
     * @param message the prefix to the message.
     * @param queryResult the query result (null will be ignored).
     * @return the message with the list of N1QL errors appended to it.
     */
    public static String errorMsg(String message, N1qlQueryResult queryResult) {
        if (message == null) {
            return (queryResult == null) ? null : queryResult.errors().toString();
        }

        if (queryResult == null) {
            return message;
        }

        return message + " - " + queryResult.errors().toString();
    }


    //=============
    //== Getters ==
    //=============

    /** @return the {@link Bucket} to be used for tests in this context. */
    public Bucket bucket() {
        return bucket;
    }

    /** @return the password used to open the {@link #bucket()}. */
    public String bucketPassword() {
        return bucketPassword;
    }

    /** @return the {@link BucketManager} to be used for tests in this context. */
    public BucketManager bucketManager() {
        return bucketManager;
    }

    /** @return the {@link Repository} associated to the {@link #bucket()} used for tests in this context. */
    public Repository repository() {
        return repository;
    }

    /** @return the {@link Cluster} to be used for tests in this context. */
    public Cluster cluster() {
        return cluster;
    }

    /** @return the {@link ClusterManager} to be used for tests in this context. */
    public ClusterManager clusterManager() {
        return clusterManager;
    }

    /** @return the administrative login for the {@link #cluster()}. */
    public String adminName() {
        return adminName;
    }

    /** @return the administrative password for the {@link #cluster()}. */
    public String adminPassword() {
        return adminPassword;
    }

    /** @return the {@link CouchbaseEnvironment} to be used for tests in this context. */
    public CouchbaseEnvironment env() {
        return env;
    }

    /** @return the name of the {@link #bucket()} to be used for tests in this context. */
    public String bucketName() {
        return bucketName;
    }

    /**
     * @return the seed node provided when connecting to the cluster.
     */
    public String seedNode() {
        return seedNode;
    }

    /**
     * Tells if the {@link #bucket()} to be used for tests in this context is ad hoc,
     * meaning that it was created for this specific context and can be destroyed at the end of the test.
     *
     * Adhoc buckets have the name initially configured in the builder prefixed with
     * "{@value CouchbaseTestContext#AD_HOC}" and suffixed with the system time in nanoseconds.
     * This makes up the name returned by {@link #bucketName()}.
     *
     * Note that such a bucket won't be flushed even if the instruction to flush was activated in the builder.
     *
     * @return true if the bucket is adhoc and can be destroyed after the tests, false otherwise.
     */
    public boolean isAdHoc() {
        return isAdHoc;
    }

    /** @return true if the {@link #bucket()} has flush capability enabled. */
    public boolean isFlushEnabled() {
        return isFlushEnabled;
    }

    public boolean rbacEnabled() {
        return rbacEnabled;
    }

    private static Properties loadProperties() {
        if (mockProperties != null) {
            return mockProperties;
        }
        mockProperties = new Properties();
        try {
            mockProperties.load(CouchbaseTestContext.class.getResourceAsStream("/mock.properties"));
        } catch (Exception ex) {
            //ignore
        }
        return mockProperties;
    }

    public static boolean isMockEnabled() {
        return Boolean.parseBoolean(mockProperties.getProperty("useMock", "true"));
    }

    public static boolean isCi() {
        return TestProperties.isCi();
    }
}
