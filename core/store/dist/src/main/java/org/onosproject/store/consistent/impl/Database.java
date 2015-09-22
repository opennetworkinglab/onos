/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.store.consistent.impl;


import java.util.function.Consumer;

import net.kuujo.copycat.cluster.ClusterConfig;
import net.kuujo.copycat.cluster.internal.coordinator.ClusterCoordinator;
import net.kuujo.copycat.cluster.internal.coordinator.CoordinatorConfig;
import net.kuujo.copycat.cluster.internal.coordinator.DefaultClusterCoordinator;
import net.kuujo.copycat.resource.Resource;

/**
 * Database.
 */
public interface Database extends DatabaseProxy<String, byte[]>, Resource<Database> {

  /**
   * Creates a new database with the default cluster configuration.<p>
   *
   * The database will be constructed with the default cluster configuration. The default cluster configuration
   * searches for two resources on the classpath - {@code cluster} and {cluster-defaults} - in that order. Configuration
   * options specified in {@code cluster.conf} will override those in {cluster-defaults.conf}.<p>
   *
   * Additionally, the database will be constructed with an database configuration that searches the classpath for
   * three configuration files - {@code name}, {@code database}, {@code database-defaults}, {@code resource}, and
   * {@code resource-defaults} - in that order. The first resource is a configuration resource with the same name
   * as the map resource. If the resource is namespaced - e.g. `databases.my-database.conf` - then resource
   * configurations will be loaded according to namespaces as well; for example, `databases.conf`.
   *
   * @param name The database name.
   * @return The database.
   */
  static Database create(String name) {
    return create(name, new ClusterConfig(), new DatabaseConfig());
  }

  /**
   * Creates a new database.<p>
   *
   * The database will be constructed with an database configuration that searches the classpath for
   * three configuration files - {@code name}, {@code database}, {@code database-defaults}, {@code resource}, and
   * {@code resource-defaults} - in that order. The first resource is a configuration resource with the same name
   * as the database resource. If the resource is namespaced - e.g. `databases.my-database.conf` - then resource
   * configurations will be loaded according to namespaces as well; for example, `databases.conf`.
   *
   * @param name The database name.
   * @param cluster The cluster configuration.
   * @return The database.
   */
  static Database create(String name, ClusterConfig cluster) {
    return create(name, cluster, new DatabaseConfig());
  }

  /**
   * Creates a new database.
   *
   * @param name The database name.
   * @param cluster The cluster configuration.
   * @param config The database configuration.

   * @return The database.
   */
  static Database create(String name, ClusterConfig cluster, DatabaseConfig config) {
    ClusterCoordinator coordinator =
            new DefaultClusterCoordinator(new CoordinatorConfig().withName(name).withClusterConfig(cluster));
    return coordinator.<Database>getResource(name, config.resolve(cluster))
      .addStartupTask(() -> coordinator.open().thenApply(v -> null))
      .addShutdownTask(coordinator::close);
  }

  /**
   * Tells whether the database supports change notifications.
   * @return true if notifications are supported; false otherwise
   */
  default boolean hasChangeNotificationSupport() {
      return true;
  }

  /**
   * Registers a new consumer of StateMachineUpdates.
   * @param consumer consumer to register
   */
  void registerConsumer(Consumer<StateMachineUpdate> consumer);

  /**
   * Unregisters a consumer of StateMachineUpdates.
   * @param consumer consumer to unregister
   */
  void unregisterConsumer(Consumer<StateMachineUpdate> consumer);
}