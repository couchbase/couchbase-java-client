package com.couchbase.client.java.repository.mapping;

import java.util.List;

/**
 * Represents the metadata for a document entity.
 *
 * @author Michael Nitschinger
 * @since 2.2.0
 */
public interface EntityMetadata {

    /**
     * Returns the metadata for all properties in this entity.
     *
     * @return the property information.
     */
    List<PropertyMetadata> properties();

    /**
     * True if it contains an id property, false otherwise.
     *
     * @return true if there is one, false otherwise.
     */
    boolean hasIdProperty();

    /**
     * Returns the metadata for the id property if set, or null otherwise.
     *
     * @return the metadata or null.
     */
    PropertyMetadata idProperty();

}
