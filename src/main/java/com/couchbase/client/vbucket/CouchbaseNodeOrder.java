package com.couchbase.client.vbucket;

/**
 * The {@link CouchbaseNodeOrder} helps with making sure that the streaming
 * connection is not always bound to the same node.
 *
 * If {@link #ORDERED} is chosen, the default behavior before the 1.2 release
 * is used. Nodes are used in the same order as they are passed in. On the
 * other hand, if {@link #RANDOM} is chosen, the node lists are always
 * shuffled before storing/applying them.
 */
public enum CouchbaseNodeOrder {

  /**
   * Keep the node lists as they arrive / are passed in.
   */
  ORDERED,

  /**
   * Shuffle the node lists always before applying them.
   */
  RANDOM
}
