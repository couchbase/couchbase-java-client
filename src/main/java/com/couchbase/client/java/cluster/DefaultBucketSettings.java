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
package com.couchbase.client.java.cluster;

import java.util.LinkedHashMap;
import java.util.Map;

import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.document.json.JsonObject;

public class DefaultBucketSettings implements BucketSettings {

    /**
     * Use a default quota of 100MB if not otherwise specified.
     */
    private static final int DEFAULT_QUOTA_MB = 100;

    private final String name;
    private final BucketType type;
    private final int quota;
    private final int port;
    private final String password;
    private final int replicas;
    private final boolean indexReplicas;
    private final boolean enableFlush;
    private final Map<String, Object> customSettings;
    private final JsonObject raw;
    private final CompressionMode compressionMode;
    private final EjectionMethod ejectionMethod;

    private DefaultBucketSettings(Builder builder) {
        this(builder, JsonObject.empty());
    }

    private DefaultBucketSettings(Builder builder, JsonObject raw) {
        name = builder.name();
        type = builder.type();
        quota = builder.quota();
        port = builder.port();
        password = builder.password();
        replicas = builder.replicas();
        indexReplicas = builder.indexReplicas();
        enableFlush = builder.enableFlush();
        customSettings = builder.customSettings();
        compressionMode = builder.compressionMode();
        ejectionMethod = builder.ejectionMethod();
        this.raw = raw;
    }

    /**
     * Provides a builder to build the bucket settings.
     *
     * @return the bucket settings builder.
     */
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Helper method to create bucket settings with a name and default settings.
     *
     * @param name the name of the bucket
     * @return bucket settings with defaults.
     */
    public static DefaultBucketSettings create(final String name) {
        return builder().name(name).build();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public BucketType type() {
        return type;
    }

    @Override
    public int quota() {
        return quota;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public int replicas() {
        return replicas;
    }

    @Override
    public boolean indexReplicas() {
        return indexReplicas;
    }

    @Override
    public boolean enableFlush() {
        return enableFlush;
    }

    @Override
    public CompressionMode compressionMode() {
        return compressionMode;
    }

    @Override
    public EjectionMethod ejectionMethod() {
        return ejectionMethod;
    }

    @Override
    public Map<String, Object> customSettings() {
        return customSettings;
    }

    @Override
    public JsonObject raw() {
        return raw;
    }

    public static class Builder implements BucketSettings {

        private String name = "";
        private BucketType type = BucketType.COUCHBASE;
        private int quota = DEFAULT_QUOTA_MB;
        private int port = 0;
        private String password = "";
        private int replicas = 0;
        private boolean indexReplicas = false;
        private boolean enableFlush = false;
        private CompressionMode compressionMode = null;
        private EjectionMethod ejectionMethod = null;
        private final Map<String, Object> customSettings = new LinkedHashMap<String, Object>();

        @Override
        public String name() {
            return name;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public BucketType type() {
            return type;
        }

        public Builder type(BucketType type) {
            this.type = type;
            return this;
        }

        @Override
        public int quota() {
            return quota;
        }

        public Builder quota(int quota) {
            this.quota = quota;
            return this;
        }

        @Override
        public int port() {
            return port;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        @Override
        public String password() {
            return password;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        @Override
        public int replicas() {
            return replicas;
        }

        public Builder replicas(int replicas) {
            this.replicas = replicas;
            return this;
        }

        @Override
        public boolean indexReplicas() {
            return indexReplicas;
        }

        public Builder indexReplicas(boolean indexReplicas) {
            this.indexReplicas = indexReplicas;
            return this;
        }

        @Override
        public boolean enableFlush() {
            return enableFlush;
        }

        public Builder enableFlush(boolean enableFlush) {
            this.enableFlush = enableFlush;
            return this;
        }

        @Override
        public CompressionMode compressionMode() {
            return compressionMode;
        }

        public Builder compressionMode(final CompressionMode compressionMode) {
            this.compressionMode = compressionMode;
            return this;
        }

        @Override
        public EjectionMethod ejectionMethod() {
            return ejectionMethod;
        }

        public Builder ejectionMethod(final EjectionMethod ejectionMethod) {
            this.ejectionMethod = ejectionMethod;
            return this;
        }

        @Override
        public Map<String, Object> customSettings() {
            return this.customSettings;
        }

        @Override
        public JsonObject raw() {
            return JsonObject.empty();
        }

        /**
         * Add a custom setting to the bucket settings (ie. one that is not covered by
         * a native method).
         *
         * @param key the setting's key.
         * @param value the setting's value.
         * @return the Builder for chaining.
         */
        public Builder withSetting(String key, Object value) {
            this.customSettings.put(key, value);
            return this;
        }

        /**
         * Add several custom settings to the bucket settings (ie. settings not covered by
         * a native method).
         *
         * @param customSettings the settings to add.
         * @return the Builder for chaining.
         */
        public Builder withSettings(Map<String, Object> customSettings) {
            this.customSettings.putAll(customSettings);
            return this;
        }

        public DefaultBucketSettings build() {
            return new DefaultBucketSettings(this);
        }

        /**
         * Build the {@link BucketSettings} from the data aggregated by this builder,
         * and set its {@link BucketSettings#raw()} representation as well.
         *
         * @param raw the raw representation for the settings, from the server.
         * @return the new {@link BucketSettings}.
         */
        public DefaultBucketSettings build(JsonObject raw) {
            return new DefaultBucketSettings(this, raw);
        }

    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("DefaultBucketSettings{")
            .append("name='").append(name).append('\'')
            .append(", type=").append(type)
            .append(", quota=").append(quota)
            .append(", port=").append(port)
            .append(", password='").append(password).append('\'')
            .append(", replicas=").append(replicas)
            .append(", indexReplicas=").append(indexReplicas)
            .append(", compressionMode=").append(compressionMode)
            .append(", ejectionMethod=").append(ejectionMethod)
            .append(", enableFlush=").append(enableFlush);
        if (!customSettings.isEmpty()) {
            s.append(", customSettings=").append(customSettings);
        }
        s.append('}');
        return s.toString();
    }
}
