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
package com.couchbase.client.java.query;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * The payload necessary to perform a {@link PreparedN1qlQuery}, as returned when
 * issuing a {@link PrepareStatement}.
 *
 * @author Simon Baslé
 * @since 2.2
 */
@InterfaceAudience.Private
@InterfaceStability.Uncommitted
public class PreparedPayload implements SerializableStatement {

    private static final long serialVersionUID = -5950676152745796617L;

    private final String preparedName;
    private final SerializableStatement originalStatement;
    private final String encodedPlan;

    public PreparedPayload(Statement originalStatement, String preparedName, String encodedPlan) {
        this.originalStatement = originalStatement instanceof SerializableStatement
                ? (SerializableStatement) originalStatement
                : new N1qlQuery.RawStatement(originalStatement.toString());
        this.preparedName = preparedName;
        this.encodedPlan = encodedPlan;
    }

    public String payload() {
        return preparedName;
    }

    public String encodedPlan() {
        return encodedPlan;
    }

    public String preparedName() {
        return preparedName;
    }

    public SerializableStatement originalStatement() {
        return originalStatement;
    }

    @Override
    public String toString() {
        return payload();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PreparedPayload that = (PreparedPayload) o;

        if (preparedName != null ? !preparedName.equals(that.preparedName) : that.preparedName != null)
            return false;
        if (originalStatement != null ? !originalStatement.equals(that.originalStatement) : that.originalStatement != null)
            return false;
        return !(encodedPlan != null ? !encodedPlan.equals(that.encodedPlan) : that.encodedPlan != null);

    }

    @Override
    public int hashCode() {
        int result = preparedName != null ? preparedName.hashCode() : 0;
        result = 31 * result + (originalStatement != null ? originalStatement.hashCode() : 0);
        result = 31 * result + (encodedPlan != null ? encodedPlan.hashCode() : 0);
        return result;
    }
}
