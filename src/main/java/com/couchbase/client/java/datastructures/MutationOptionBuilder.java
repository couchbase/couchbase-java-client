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
package com.couchbase.client.java.datastructures;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;
import com.couchbase.client.java.PersistTo;
import com.couchbase.client.java.ReplicateTo;

/**
 * MutationOptionBuilder allows to set following constraints on data structure mutation operations
 *
 * - cas
 * - expiry
 * - persistence
 * - replication
 * - create Document
 *
 * @author Subhashni Balakrishnan
 * @since 2.3.5
 */

@InterfaceStability.Committed
@InterfaceAudience.Public
public class MutationOptionBuilder {

    private int expiry;
    private long cas;
    private PersistTo persistTo;
    private ReplicateTo replicateTo;
    private boolean createDocument;

    private MutationOptionBuilder() {
        this.expiry = 0;
        this.cas = 0L;
        this.persistTo = PersistTo.NONE;
        this.replicateTo = ReplicateTo.NONE;
        this.createDocument = false;
    }

    public static MutationOptionBuilder builder() {
        return new MutationOptionBuilder();
    }

    /**
     * Set expiration on option builder
     *
     * @param expiry expiration time, 0 means no expiry
     */
    public MutationOptionBuilder expiry(int expiry) {
        this.expiry = expiry;
        return this;
    }

    /**
     * Get expiration stored in option builder
     *
     * Returns expiration time
     */

    public int expiry() {
        return this.expiry;
    }

    /**
     * Set cas for optimistic locking on option builder
     *
     * @param cas the CAS to compare
     */
    public MutationOptionBuilder cas(long cas) {
        this.cas = cas;
        return this;
    }

    /**
     * Get cas stored in option builder
     *
     * Returns cas
     */
    public long cas() {
        return cas;
    }

    /**
     * Set persistence durability constraints on option builder
     *
     * @param persistTo persistence constraint
     */
    public MutationOptionBuilder persistTo(PersistTo persistTo) {
        this.persistTo = persistTo;
        return this;
    }

    /**
     * Get persistence durability constraints stored in option builder
     *
     * Returns persistence constraint
     */
    public PersistTo persistTo() {
        return this.persistTo;
    }

    /**
     * Set replication durability constraints on option builder
     *
     * @param replicateTo replication constraint
     */
    public MutationOptionBuilder replicateTo(ReplicateTo replicateTo) {
        this.replicateTo = replicateTo;
        return this;
    }


    /**
     * Get replication durability constraints stored in option builder
     *
     * Returns replication constraint
     */
    public ReplicateTo replicateTo() {
        return this.replicateTo;
    }

    /**
     * createDocument On true, creates the document if it does not exist
     */
    public MutationOptionBuilder createDocument(boolean createDoc) {
        this.createDocument = createDoc;
        return this;
    }

    /**
     * Return createDocument option set in mutation option builder
     *
     * Returns createDocument boolean
     */
    public  boolean createDocument() {
        return this.createDocument;
    }
}