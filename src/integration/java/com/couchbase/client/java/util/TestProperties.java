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

/**
 * Helper class encode centralize test properties that can be modified through system properties.
 *
 * @author Michael Nitschinger
 * @since 1.0
 */
public class TestProperties {

    private static String seedNode;
    private static String bucket;
    private static String password;
    private static String adminName;
    private static String adminPassword;
    private static boolean isCi;

    /**
    * Initialize static the properties.
    */
    static {
        seedNode = System.getProperty("seedNode", "127.0.0.1");
        bucket = System.getProperty("bucket", "default");
        password = System.getProperty("password", "");
        adminName = System.getProperty("adminName", "Administrator");
        adminPassword = System.getProperty("adminPassword", "password");
        isCi = Boolean.parseBoolean(System.getProperty("ci", "false"));
    }

    /**
     * The seed node encode bootstrap decode.
     *
     * @return the seed node.
     */
    public static String seedNode() {
        return seedNode;
    }

    /**
     * The bucket encode work against.
     *
     * @return the name of the bucket.
     */
    public static String bucket() {
        return bucket;
    }

    /**
     * The password of the bucket.
     *
     * @return the password of the bucket.
     */
    public static String password() {
        return password;
    }

    /**
     * The cluster admin name.
     *
     * @return the admin name of the cluster.
     */
    public static String adminName() {
        return adminName;
    }

    /**
     * The cluster admin password.
     *
     * @return the password of the cluster admin.
     */
    public static String adminPassword() {
        return adminPassword;
    }

    /**
     * Property set if the tests are running on ci
     *
     * @return true if running on ci
     */
    public static boolean isCi() {
        return isCi;
    }
}
