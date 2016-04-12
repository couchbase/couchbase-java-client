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