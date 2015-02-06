package org.onosproject.store.consistent.impl;

import net.kuujo.copycat.resource.internal.ResourceContext;
import net.kuujo.copycat.state.StateMachine;
import net.kuujo.copycat.resource.internal.AbstractResource;
import net.kuujo.copycat.state.internal.DefaultStateMachine;
import net.kuujo.copycat.util.concurrent.Futures;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Default database.
 */
public class DefaultDatabase extends AbstractResource<Database> implements Database {
  private final StateMachine<DatabaseState<String, byte[]>> stateMachine;
  private DatabaseProxy<String, byte[]> proxy;

  @SuppressWarnings("unchecked")
  public DefaultDatabase(ResourceContext context) {
    super(context);
    this.stateMachine = new DefaultStateMachine(context, DatabaseState.class, DefaultDatabaseState.class);
  }

  /**
   * If the database is closed, returning a failed CompletableFuture. Otherwise, calls the given supplier to
   * return the completed future result.
   *
   * @param supplier The supplier to call if the database is open.
   * @param <T> The future result type.
   * @return A completable future that if this database is closed is immediately failed.
   */
  protected <T> CompletableFuture<T> checkOpen(Supplier<CompletableFuture<T>> supplier) {
    if (proxy == null) {
      return Futures.exceptionalFuture(new IllegalStateException("Database closed"));
    }
    return supplier.get();
  }

  @Override
  public CompletableFuture<Integer> size(String tableName) {
    return checkOpen(() -> proxy.size(tableName));
  }

  @Override
  public CompletableFuture<Boolean> isEmpty(String tableName) {
    return checkOpen(() -> proxy.isEmpty(tableName));
  }

  @Override
  public CompletableFuture<Boolean> containsKey(String tableName, String key) {
    return checkOpen(() -> proxy.containsKey(tableName, key));
  }

  @Override
  public CompletableFuture<Boolean> containsValue(String tableName, byte[] value) {
    return checkOpen(() -> proxy.containsValue(tableName, value));
  }

  @Override
  public CompletableFuture<Versioned<byte[]>> get(String tableName, String key) {
    return checkOpen(() -> proxy.get(tableName, key));
  }

  @Override
  public CompletableFuture<Versioned<byte[]>> put(String tableName, String key, byte[] value) {
    return checkOpen(() -> proxy.put(tableName, key, value));
  }

  @Override
  public CompletableFuture<Versioned<byte[]>> remove(String tableName, String key) {
    return checkOpen(() -> proxy.remove(tableName, key));
  }

  @Override
  public CompletableFuture<Void> clear(String tableName) {
    return checkOpen(() -> proxy.clear(tableName));
  }

  @Override
  public CompletableFuture<Set<String>> keySet(String tableName) {
    return checkOpen(() -> proxy.keySet(tableName));
  }

  @Override
  public CompletableFuture<Collection<Versioned<byte[]>>> values(String tableName) {
    return checkOpen(() -> proxy.values(tableName));
  }

  @Override
  public CompletableFuture<Set<Map.Entry<String, Versioned<byte[]>>>> entrySet(String tableName) {
    return checkOpen(() -> proxy.entrySet(tableName));
  }

  @Override
  public CompletableFuture<Versioned<byte[]>> putIfAbsent(String tableName, String key, byte[] value) {
    return checkOpen(() -> proxy.putIfAbsent(tableName, key, value));
  }

  @Override
  public CompletableFuture<Boolean> remove(String tableName, String key, byte[] value) {
    return checkOpen(() -> proxy.remove(tableName, key, value));
  }

  @Override
  public CompletableFuture<Boolean> remove(String tableName, String key, long version) {
    return checkOpen(() -> proxy.remove(tableName, key, version));
  }

  @Override
  public CompletableFuture<Boolean> replace(String tableName, String key, byte[] oldValue, byte[] newValue) {
    return checkOpen(() -> proxy.replace(tableName, key, oldValue, newValue));
  }

  @Override
  public CompletableFuture<Boolean> replace(String tableName, String key, long oldVersion, byte[] newValue) {
    return checkOpen(() -> proxy.replace(tableName, key, oldVersion, newValue));
  }

  @Override
  public CompletableFuture<Boolean> atomicBatchUpdate(List<UpdateOperation<String, byte[]>> updates) {
      return checkOpen(() -> proxy.atomicBatchUpdate(updates));
  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized CompletableFuture<Database> open() {
    return runStartupTasks()
      .thenCompose(v -> stateMachine.open())
      .thenRun(() -> {
        this.proxy = stateMachine.createProxy(DatabaseProxy.class);
      })
      .thenApply(v -> null);
  }

  @Override
  public synchronized CompletableFuture<Void> close() {
    proxy = null;
    return stateMachine.close()
      .thenCompose(v -> runShutdownTasks());
  }
}
