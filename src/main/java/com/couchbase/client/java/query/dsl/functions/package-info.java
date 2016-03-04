/**
 * Functions are {@link com.couchbase.client.java.query.dsl.Expression Expressions} that represent predefined utility
 * functions in N1QL. The <code>xxxFunctions</code> classes each map to a N1QL category of functions. Some other
 * utility methods allow you to build constructs that are not functions per se but can also be used in many places
 * where an Expression is accepted: see {@link com.couchbase.client.java.query.dsl.functions.Collections},
 * {@link com.couchbase.client.java.query.dsl.functions.Case}.
 *
 * For more specialized Expression builders that don't apply everywhere but only on specific parts of a N1QL statement,
 * see {@link com.couchbase.client.java.query.dsl.clause clauses}.
 */
package com.couchbase.client.java.query.dsl.functions;