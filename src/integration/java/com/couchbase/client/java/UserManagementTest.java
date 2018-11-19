/*
 * Copyright (c) 2017 Couchbase, Inc.
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

package com.couchbase.client.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;


import java.util.Collections;
import java.util.List;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.java.cluster.AuthDomain;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.User;
import com.couchbase.client.java.cluster.UserRole;
import com.couchbase.client.java.cluster.UserSettings;
import com.couchbase.client.java.util.CouchbaseTestContext;
import com.couchbase.client.java.util.features.Version;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

/**
 * @author Subhashni Balakrishnan
 */
public class UserManagementTest {

    private static CouchbaseTestContext ctx;
    private static ClusterManager clusterManager;

    private static final String USER_1 = "alice";
    private static final String USER_2 = "bob";

    @BeforeClass
    public static void setup() {
        assumeFalse(CouchbaseTestContext.isMockEnabled());

        ctx = CouchbaseTestContext.builder()
                .build()
                .ignoreIfClusterUnder(Version.parseVersion("5.0.0"));
        clusterManager = ctx.clusterManager();
    }

    @AfterClass
    public static void cleanup() {
        if (ctx != null) {
            ctx.destroyBucketAndDisconnect();
        }
    }

    @Test
    public void testUpsertUser() {
        Observable
                .just(USER_1)
                .map(new Func1<String, UserSettings>() {
                    @Override
                    public UserSettings call(final String username) {
                        return UserSettings
                                .build()
                                .name(username)
                                .password("password")
                                .roles(Collections.singletonList(new UserRole("data_reader", "*")));
                    }
                }).flatMap(new Func1<UserSettings, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(final UserSettings userSettings) {
                return clusterManager.async().upsertUser(AuthDomain.LOCAL, userSettings.name(), userSettings);
            }
        }).toBlocking().single();
        clusterManager.removeUser(AuthDomain.LOCAL, USER_1);
    }

    @Test(expected = CouchbaseException.class)
    public void testUpsertUserMissingPassword() {
        Observable
                .just(USER_2)
                .map(new Func1<String, UserSettings>() {
                    @Override
                    public UserSettings call(final String username) {
                        return UserSettings
                                .build()
                                .name(username)
                                .roles(Collections.singletonList(new UserRole("data_reader", "*")));
                    }
                }).flatMap(new Func1<UserSettings, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(final UserSettings userSettings) {
                return clusterManager.async().upsertUser(AuthDomain.LOCAL, userSettings.name(), userSettings);
            }
        }).toBlocking().single();
    }

    @Test
    public void testGetUsers() {
        Observable
                .just("UserManagementTestUser1")
                .map(new Func1<String, UserSettings>() {
                    @Override
                    public UserSettings call(final String username) {
                        return UserSettings
                                .build()
                                .name(username)
                                .password("password")
                                .roles(Collections.singletonList(new UserRole("data_reader", "*")));
                    }
                }).flatMap(new Func1<UserSettings, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(final UserSettings userSettings) {
                return clusterManager.async().upsertUser(AuthDomain.LOCAL, userSettings.name(), userSettings);
            }
        }).toBlocking().single();

        Observable
                .just("UserManagementTestUser2")
                .map(new Func1<String, UserSettings>() {
                    @Override
                    public UserSettings call(final String username) {
                        return UserSettings
                                .build()
                                .name(username)
                                .password("password")
                                .roles(Collections.singletonList(new UserRole("data_reader", "*")));
                    }
                }).flatMap(new Func1<UserSettings, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(final UserSettings userSettings) {
                return clusterManager.async().upsertUser(AuthDomain.LOCAL, userSettings.name(), userSettings);
            }
        }).toBlocking().single();

        List<User> users = clusterManager.async().getUsers(AuthDomain.LOCAL).toList().toBlocking().single();
        int matchCount = 0;
        for (User user:users) {
            if (user.userId().contains("UserManagementTest")){
                matchCount += 1;
            }
        }
        assertEquals("Get user list should contain users added", 2, matchCount);
        clusterManager.removeUser(AuthDomain.LOCAL, "UserManagementTestUser1");
        clusterManager.removeUser(AuthDomain.LOCAL, "UserManagementTestUser2");
    }
}
