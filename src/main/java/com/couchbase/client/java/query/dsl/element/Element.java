package com.couchbase.client.java.query.dsl.element;

import com.couchbase.client.core.annotations.InterfaceAudience;
import com.couchbase.client.core.annotations.InterfaceStability;

/**
 * The Element interface describes keywords in the N1QL DSL.
 *
 * @author Michael Nitschinger
 */
@InterfaceStability.Experimental
@InterfaceAudience.Public
public interface Element {

    public static final String ESCAPE_CHAR = "`";

    String export();

}
