/**
 * Copyright (C) 2009-2013 Couchbase, Inc.
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

package com.couchbase.client;

import com.couchbase.client.internal.AdaptiveThrottler;
import com.couchbase.client.internal.ThrottleManager;
import com.couchbase.client.vbucket.ConfigurationException;
import com.couchbase.client.vbucket.Reconfigurable;
import com.couchbase.client.vbucket.VBucketNodeLocator;
import com.couchbase.client.vbucket.config.Bucket;
import net.spy.memcached.BroadcastOpFactory;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import net.spy.memcached.OperationFactory;
import net.spy.memcached.ops.KeyedOperation;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationCallback;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.ReplicaGetOperation;
import net.spy.memcached.ops.ReplicaGetsOperation;
import net.spy.memcached.ops.VBucketAware;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Maintains connections to each node in a cluster of Couchbase Nodes.
 *
 */
public class CouchbaseConnection extends MemcachedConnection  implements
  Reconfigurable {

  /**
   * The amount in seconds after which a op broadcast is forced to detect
   * dead connections.
   */
  protected static final int ALLOWED_IDLE_TIME = 5;

  protected volatile boolean reconfiguring = false;
  protected final CouchbaseConnectionFactory cf;
  private final ThrottleManager throttleManager;
  private final boolean enableThrottling;

  protected volatile long lastWrite;

  public CouchbaseConnection(int bufSize, CouchbaseConnectionFactory f,
      List<InetSocketAddress> a, Collection<ConnectionObserver> obs,
      FailureMode fm, OperationFactory opfactory) throws IOException {
    super(bufSize, f, a, obs, fm, opfactory);
    this.cf = f;

    enableThrottling = Boolean.parseBoolean(
      CouchbaseProperties.getProperty("enable_throttle", false));
    if(enableThrottling) {
      this.throttleManager = new ThrottleManager<AdaptiveThrottler>(
        a, AdaptiveThrottler.class, this, opfactory);
    } else {
      this.throttleManager = null;
    }
    updateLastWrite();
  }

  public void reconfigure(Bucket bucket) {
    if(reconfiguring) {
      getLogger().debug("Suppressing attempt to reconfigure again while "
        + "reconfiguring.");
      return;
    }

    reconfiguring = true;
    try {
      // get a new collection of addresses from the received config
      List<String> servers = bucket.getConfig().getServers();
      HashSet<SocketAddress> newServerAddresses = new HashSet<SocketAddress>();
      ArrayList<InetSocketAddress> newServers =
          new ArrayList<InetSocketAddress>();
      for (String server : servers) {
        int finalColon = server.lastIndexOf(':');
        if (finalColon < 1) {
          throw new IllegalArgumentException("Invalid server ``" + server
              + "'' in vbucket's server list");
        }
        String hostPart = server.substring(0, finalColon);
        String portNum = server.substring(finalColon + 1);

        InetSocketAddress address =
            new InetSocketAddress(hostPart, Integer.parseInt(portNum));
        // add parsed address to our collections
        newServerAddresses.add(address);
        newServers.add(address);
      }

      // split current nodes to "odd nodes" and "stay nodes"
      ArrayList<MemcachedNode> oddNodes = new ArrayList<MemcachedNode>();
      ArrayList<MemcachedNode> stayNodes = new ArrayList<MemcachedNode>();
      ArrayList<InetSocketAddress> stayServers =
          new ArrayList<InetSocketAddress>();
      for (MemcachedNode current : locator.getAll()) {
        if (newServerAddresses.contains(current.getSocketAddress())) {
          stayNodes.add(current);
          stayServers.add((InetSocketAddress) current.getSocketAddress());
        } else {
          oddNodes.add(current);
        }
      }

      // prepare a collection of addresses for new nodes
      newServers.removeAll(stayServers);

      // create a collection of new nodes
      List<MemcachedNode> newNodes = createConnections(newServers);

      // merge stay nodes with new nodes
      List<MemcachedNode> mergedNodes = new ArrayList<MemcachedNode>();
      mergedNodes.addAll(stayNodes);
      mergedNodes.addAll(newNodes);

      for(MemcachedNode keepingNode : mergedNodes) {
        getLogger().debug("Node " + keepingNode.getSocketAddress()
          + " will stay in cluster config after reconfiguration.");
      }

      // call update locator with new nodes list and vbucket config
      if (locator instanceof VBucketNodeLocator) {
        ((VBucketNodeLocator)locator).updateLocator(mergedNodes,
            bucket.getConfig());
      } else {
        locator.updateLocator(mergedNodes);
      }

      if(enableThrottling) {
        for(MemcachedNode node : newNodes) {
          throttleManager.setThrottler(
            (InetSocketAddress)node.getSocketAddress());
        }
        for(MemcachedNode node : oddNodes) {
          throttleManager.removeThrottler(
            (InetSocketAddress)node.getSocketAddress());
        }
      }

      // schedule shutdown for the oddNodes
      for(MemcachedNode shutDownNode : oddNodes) {
        getLogger().info("Scheduling Node "
          + shutDownNode.getSocketAddress() + " for shutdown.");
      }
      nodesToShutdown.addAll(oddNodes);
    } catch (IOException e) {
      getLogger().error("Connection reconfiguration failed", e);
    } finally {
      reconfiguring = false;
    }
  }

  /**
   * Add an operation to the given connection.
   *
   * @param key the key the operation is operating upon
   * @param o the operation
   */
  @Override
  public void addOperation(final String key, final Operation o) {
    MemcachedNode placeIn = null;

    MemcachedNode primary;
    if(o instanceof ReplicaGetOperation
      && locator instanceof VBucketNodeLocator) {
      primary = ((VBucketNodeLocator) locator).getReplica(key,
        ((ReplicaGetOperation) o).getReplicaIndex());
    } else if(o instanceof ReplicaGetsOperation
      && locator instanceof VBucketNodeLocator) {
      primary = ((VBucketNodeLocator) locator).getReplica(key,
        ((ReplicaGetsOperation) o).getReplicaIndex());
    } else {
      primary = locator.getPrimary(key);
    }

    if (primary == null) {
      o.cancel();
      cf.checkConfigUpdate();
      return;
    }

    boolean needsRecheckConfigUpdate = false;
    if (primary.isActive() || failureMode == FailureMode.Retry) {
      placeIn = primary;
      needsRecheckConfigUpdate = !primary.isActive();
    } else if (failureMode == FailureMode.Cancel) {
      o.cancel();
      needsRecheckConfigUpdate = true;
    } else {
      // Look for another node in sequence that is ready.
      for (Iterator<MemcachedNode> i = locator.getSequence(key); placeIn == null
          && i.hasNext();) {
        MemcachedNode n = i.next();
        if (n.isActive()) {
          placeIn = n;
        }
      }

      if (placeIn == null) {
        placeIn = primary;
        needsRecheckConfigUpdate = true;
      }
    }

    // If we didn't find an active node, queue it in the primary node
    // and wait for it to come back online.
    if (needsRecheckConfigUpdate) {
        getLogger().info("Node for key \"%s\" is not active (yet). Queueing "
            + "up for retry and checking for stale configuration.", key);
      cf.checkConfigUpdate();
    }

    assert o.isCancelled() || placeIn != null : "No node found for key " + key;
    if (placeIn != null) {
      // add the vbucketIndex to the operation
      if (locator instanceof VBucketNodeLocator) {
        VBucketNodeLocator vbucketLocator = (VBucketNodeLocator) locator;
        short vbucketIndex = (short) vbucketLocator.getVBucketIndex(key);
        if (o instanceof VBucketAware) {
          VBucketAware vbucketAwareOp = (VBucketAware) o;
          vbucketAwareOp.setVBucket(key, vbucketIndex);
          Collection<MemcachedNode> notMyVbucketNodes =
            vbucketAwareOp.getNotMyVbucketNodes();
          if (!notMyVbucketNodes.isEmpty()) {
            cf.checkConfigUpdate();
            MemcachedNode alternative = vbucketLocator.getAlternative(key,
              notMyVbucketNodes);
            if (alternative == null) {
              notMyVbucketNodes.clear();
            } else {
              placeIn = alternative;
            }
          }
        }
      }
      if(enableThrottling) {
        throttleManager.getThrottler(
          (InetSocketAddress)placeIn.getSocketAddress()).throttle();
      }
      updateLastWrite();
      addOperation(placeIn, o);
    } else {
      assert o.isCancelled() : "No node found for " + key
          + " (and not immediately cancelled)";
    }
  }

  @Override
  public void addOperations(final Map<MemcachedNode, Operation> ops) {
    for (Map.Entry<MemcachedNode, Operation> me : ops.entrySet()) {
      final MemcachedNode node = me.getKey();

      if (!node.isActive()) {
        cf.checkConfigUpdate();
      }

      Operation o = me.getValue();
      // add the vbucketIndex to the operation
      if (locator instanceof VBucketNodeLocator) {
        if (o instanceof KeyedOperation && o instanceof VBucketAware) {
          Collection<String> keys = ((KeyedOperation) o).getKeys();
          VBucketNodeLocator vbucketLocator = (VBucketNodeLocator) locator;
          for (String key : keys) {
            short vbucketIndex = (short) vbucketLocator.getVBucketIndex(key);
            VBucketAware vbucketAwareOp = (VBucketAware) o;
            vbucketAwareOp.setVBucket(key, vbucketIndex);
          }
        }
      }
      o.setHandlingNode(node);
      o.initialize();
      node.addOp(o);
      addedQueue.offer(node);
    }
    updateLastWrite();
    Selector s = selector.wakeup();
    assert s == selector : "Wakeup returned the wrong selector.";
  }

  /**
   * Infinitely loop processing IO.
   */
  @Override
  public void run() {
    while (running) {
      if (!reconfiguring) {
        try {
          handleIO();
        } catch (IOException e) {
          logRunException(e);
        } catch (CancelledKeyException e) {
          logRunException(e);
        } catch (ClosedSelectorException e) {
          logRunException(e);
        } catch (IllegalStateException e) {
          logRunException(e);
        } catch (ConcurrentModificationException e) {
          logRunException(e);
        } catch (ConfigurationException e) {
          getLogger().debug("Configuration unsuccessful. Will retry.", e);
        }
      }
    }
    getLogger().info("Shut down Couchbase client");
  }

  @Override
  protected void handleRetryInformation(byte[] retryMessage) {
    String message = new String(retryMessage).trim();
    if (message.startsWith("{")) {
      cf.getConfigurationProvider().setConfig(
        replaceConfigWildcards(message)
      );
    }
  }

  /**
   * Only queue for reconnect if the given node is still part of the cluster.
   *
   * Since a node is queued to reconnect, it indicates a close socket and
   * therefore an outdated configuration. With some providers, it is important
   * to force a config reload which is also issued immediately.
   *
   * @param node the node to check.
   */
  @Override
  protected void queueReconnect(final MemcachedNode node) {
    cf.getConfigurationProvider().reloadConfig();
    if (isShutDown() || !locator.getAll().contains(node)) {
      getLogger().debug("Preventing reconnect for node " + node
        + " because it" + "is either not part of the cluster anymore or "
        + "the connection is " + "shutting down.");
      return;
    }

    super.queueReconnect(node);
  }

  /**
   * Helper method to parse config wildcards into their actual representations.
   *
   * Currently, this method parses the $HOST wildcard and finds the first node
   * in the locator to replace it with.
   *
   * @param original the raw new config string.
   * @return the potentially changed config string.
   */
  public String replaceConfigWildcards(String original) {
    if (original.contains("$HOST")) {
      ArrayList<MemcachedNode> nodes =
        new ArrayList<MemcachedNode>(getLocator().getAll());
      if (nodes.size() > 0) {
        InetSocketAddress addr = (InetSocketAddress) nodes.get(0)
          .getSocketAddress();
        return original.replaceAll("\\$HOST", addr.getHostName());
      } else {
        throw new IllegalStateException("Locator has no nodes attached, "
          + "this is not allowed.");
      }
    }

    return original;
  }

  private void logRunException(Exception e) {
    if (shutDown) {
      // There are a couple types of errors that occur during the
      // shutdown sequence that are considered OK. Log at debug.
      getLogger().debug("Exception occurred during shutdown", e);
    } else {
      getLogger().warn("Problem handling Couchbase IO", e);
    }
  }

  /**
   * Helper method to centralize updating the last write timestamp.
   */
  protected void updateLastWrite() {
    long now = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
    if (lastWrite != now) {
      lastWrite = now;
    }
  }

  /**
   * Make sure that if the selector is woken up manually for an extended period
   * of time that the sockets are still alive.
   *
   * <p>This is done by broadcasting a operation so that disconnected sockets
   * are discovered even when no load is applied.</p>
   */
  @Override
  protected void handleWokenUpSelector() {
    long now = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime());
    long diff = now - lastWrite;
    if (lastWrite > 0 && diff >= ALLOWED_IDLE_TIME) {
      updateLastWrite();
      getLogger().debug("Wakeup counter triggered, broadcasting noops.");
      final OperationFactory fact = cf.getOperationFactory();
      broadcastOperation(new BroadcastOpFactory() {
        @Override
        public Operation newOp(MemcachedNode n, final CountDownLatch latch) {
          return fact.noop(new OperationCallback() {
            @Override
            public void receivedStatus(OperationStatus status) { }

            @Override
            public void complete() {
              latch.countDown();
            }
          });
        }
      });
    }
  }
}
