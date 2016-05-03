/**
 * Copyright (C) 2016 Couchbase, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALING
 * IN THE SOFTWARE.
 */
package com.couchbase.client.java;

import com.couchbase.client.core.message.kv.MutationToken;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.subdoc.DocumentFragment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Aggregation of one or more {@link MutationToken MutationToken} into one {@link MutationState}.
 *
 * @author Michael Nitschinger
 * @since 2.3.0
 */
public class MutationState implements Iterable<MutationToken> {

    private final List<MutationToken> tokens;

    private MutationState() {
        this.tokens = new ArrayList<MutationToken>();
    }

    /**
     * Create a {@link MutationState} from one or more {@link Document Documents}.
     *
     * @param documents the documents where the tokens are extracted from.
     * @return the initialized {@link MutationState}.
     */
    public static MutationState from(Document... documents) {
        return new MutationState().add(documents);
    }

    /**
     * Create a {@link MutationState} from one or more {@link DocumentFragment DocumentFragments}.
     *
     * @param documentFragments the document fragments where the tokens are extracted from.
     * @return the initialized {@link MutationState}.
     */
    public static MutationState from(DocumentFragment... documentFragments) {
        return new MutationState().add(documentFragments);
    }

    /**
     * Add one or more {@link Document Documents} to this {@link MutationState}.
     *
     * @param documents the documents where the tokens are extracted from.
     * @return the modified {@link MutationState}.
     */
    public MutationState add(Document... documents) {
        if (documents == null || documents.length == 0) {
            throw new IllegalArgumentException("At least one Document must be provided.");
        }
        for (Document d : documents) {
            addToken(d.mutationToken());
        }
        return this;
    }

    /**
     * Add one or more {@link DocumentFragment DocumentFragments} to this {@link MutationState}.
     *
     * @param documentFragments the fragments where the tokens are extracted from.
     * @return the modified {@link MutationState}.
     */
    public MutationState add(DocumentFragment... documentFragments) {
        if (documentFragments == null || documentFragments.length == 0) {
            throw new IllegalArgumentException("At least one DocumentFragment must be provided.");
        }
        for (DocumentFragment d : documentFragments) {
            addToken(d.mutationToken());
        }
        return this;
    }

    /**
     * Adds all the internal state from the given {@link MutationState} onto the called one.
     *
     * @param mutationState the state from which the tokens are applied from.
     * @return the modified {@link MutationState}.
     */
    public MutationState add(MutationState mutationState) {
        for(MutationToken token : mutationState) {
            addToken(token);
        }
        return this;
    }

    /**
     * Helper method to check the incoming token and store it if needed.
     *
     * Note that the token is only stored if it doesn't exist for the given vbucket already or the given sequence
     * number is higher than the one stored.
     *
     * @param token the token to check and maybe store.
     */
    private void addToken(final MutationToken token) {
        if (token != null) {
            ListIterator<MutationToken> tokenIterator = tokens.listIterator();
            while (tokenIterator.hasNext()) {
                MutationToken t = tokenIterator.next();
                if (t.vbucketID() == token.vbucketID() && t.bucket().equals(token.bucket())) {
                    if (token.sequenceNumber() > t.sequenceNumber()) {
                        tokenIterator.set(token);
                    }
                    return;
                }
            }

            tokens.add(token);
        }
    }

    @Override
    public Iterator<MutationToken> iterator() {
        return tokens.iterator();
    }

    /**
     * Exports the {@link MutationState} into a universal format, which can be used either to serialize it into
     * a N1QL query or to send it over the network to a different application/SDK.
     *
     * @return the exported {@link JsonObject}.
     */
    public JsonObject export() {
        JsonObject result = JsonObject.create();
        for (MutationToken token : tokens) {
            JsonObject bucket = result.getObject(token.bucket());
            if (bucket == null) {
                bucket = JsonObject.create();
                result.put(token.bucket(), bucket);
            }

            bucket.put(
                String.valueOf(token.vbucketID()),
                JsonArray.from(token.sequenceNumber(), String.valueOf(token.vbucketUUID()))
            );
        }
        return result;
    }
}
