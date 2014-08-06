package com.couchbase.client.java.env;

import com.couchbase.client.core.env.DynamicCoreProperties;

/**
 * .
 *
 * @author Michael Nitschinger
 */
public class DynamicClusterProperties extends DynamicCoreProperties implements ClusterProperties {

    protected DynamicClusterProperties(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DynamicClusterProperties create() {
        return new DynamicClusterProperties(new Builder());
    }

    public static class Builder extends DynamicCoreProperties.Builder {
        @Override
        public Builder sslEnabled(boolean sslEnabled) {
            super.sslEnabled(sslEnabled);
            return this;
        }

        @Override
        public Builder sslKeystoreFile(String sslKeystoreFile) {
            super.sslKeystoreFile(sslKeystoreFile);
            return this;
        }

        @Override
        public Builder sslKeystorePassword(String sslKeystorePassword) {
            super.sslKeystorePassword(sslKeystorePassword);
            return this;
        }

        @Override
        public Builder queryEnabled(boolean queryEnabled) {
            super.queryEnabled(queryEnabled);
            return this;
        }

        @Override
        public Builder queryPort(int queryPort) {
            super.queryPort(queryPort);
            return this;
        }

        @Override
        public Builder bootstrapHttpEnabled(boolean bootstrapHttpEnabled) {
            super.bootstrapHttpEnabled(bootstrapHttpEnabled);
            return this;
        }

        @Override
        public Builder bootstrapCarrierEnabled(boolean bootstrapCarrierEnabled) {
            super.bootstrapCarrierEnabled(bootstrapCarrierEnabled);
            return this;
        }

        @Override
        public Builder bootstrapHttpDirectPort(int bootstrapHttpDirectPort) {
            super.bootstrapHttpDirectPort(bootstrapHttpDirectPort);
            return this;
        }

        @Override
        public Builder bootstrapHttpSslPort(int bootstrapHttpSslPort) {
            super.bootstrapHttpSslPort(bootstrapHttpSslPort);
            return this;
        }

        @Override
        public Builder bootstrapCarrierDirectPort(int bootstrapCarrierDirectPort) {
            super.bootstrapCarrierDirectPort(bootstrapCarrierDirectPort);
            return this;
        }

        @Override
        public Builder bootstrapCarrierSslPort(int bootstrapCarrierSslPort) {
            super.bootstrapCarrierSslPort(bootstrapCarrierSslPort);
            return this;
        }

        @Override
        public Builder ioPoolSize(int ioPoolSize) {
            super.ioPoolSize(ioPoolSize);
            return this;
        }

        @Override
        public Builder computationPoolSize(int computationPoolSize) {
            super.computationPoolSize(computationPoolSize);
            return this;
        }

        @Override
        public Builder requestBufferSize(int requestBufferSize) {
            super.requestBufferSize(requestBufferSize);
            return this;
        }

        @Override
        public Builder responseBufferSize(int responseBufferSize) {
            super.responseBufferSize(responseBufferSize);
            return this;
        }

        @Override
        public Builder binaryServiceEndpoints(int binaryServiceEndpoints) {
            super.binaryServiceEndpoints(binaryServiceEndpoints);
            return this;
        }

        @Override
        public Builder viewServiceEndpoints(int viewServiceEndpoints) {
            super.viewServiceEndpoints(viewServiceEndpoints);
            return this;
        }

        @Override
        public Builder queryServiceEndpoints(int queryServiceEndpoints) {
            super.queryServiceEndpoints(queryServiceEndpoints);
            return this;
        }

        @Override
        public DynamicClusterProperties build() {
            return new DynamicClusterProperties(this);
        }
    }
}
