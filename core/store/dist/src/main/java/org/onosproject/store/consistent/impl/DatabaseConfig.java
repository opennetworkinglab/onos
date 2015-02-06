package org.onosproject.store.consistent.impl;

import com.typesafe.config.ConfigValueFactory;
import net.kuujo.copycat.cluster.ClusterConfig;
import net.kuujo.copycat.cluster.internal.coordinator.CoordinatedResourceConfig;
import net.kuujo.copycat.protocol.Consistency;
import net.kuujo.copycat.resource.ResourceConfig;
import net.kuujo.copycat.state.StateLogConfig;
import net.kuujo.copycat.util.internal.Assert;

import java.util.Map;

/**
 * Database configuration.
 *
 */
public class DatabaseConfig extends ResourceConfig<DatabaseConfig> {
  private static final String DATABASE_CONSISTENCY = "consistency";

  private static final String DEFAULT_CONFIGURATION = "database-defaults";
  private static final String CONFIGURATION = "database";

  public DatabaseConfig() {
    super(CONFIGURATION, DEFAULT_CONFIGURATION);
  }

  public DatabaseConfig(Map<String, Object> config) {
    super(config, CONFIGURATION, DEFAULT_CONFIGURATION);
  }

  public DatabaseConfig(String resource) {
    super(resource, CONFIGURATION, DEFAULT_CONFIGURATION);
  }

  protected DatabaseConfig(DatabaseConfig config) {
    super(config);
  }

  @Override
  public DatabaseConfig copy() {
    return new DatabaseConfig(this);
  }

  /**
   * Sets the database read consistency.
   *
   * @param consistency The database read consistency.
   * @throws java.lang.NullPointerException If the consistency is {@code null}
   */
  public void setConsistency(String consistency) {
    this.config = config.withValue(DATABASE_CONSISTENCY,
            ConfigValueFactory.fromAnyRef(
                    Consistency.parse(Assert.isNotNull(consistency, "consistency")).toString()));
  }

  /**
   * Sets the database read consistency.
   *
   * @param consistency The database read consistency.
   * @throws java.lang.NullPointerException If the consistency is {@code null}
   */
  public void setConsistency(Consistency consistency) {
    this.config = config.withValue(DATABASE_CONSISTENCY,
            ConfigValueFactory.fromAnyRef(
                    Assert.isNotNull(consistency, "consistency").toString()));
  }

  /**
   * Returns the database read consistency.
   *
   * @return The database read consistency.
   */
  public Consistency getConsistency() {
    return Consistency.parse(config.getString(DATABASE_CONSISTENCY));
  }

  /**
   * Sets the database read consistency, returning the configuration for method chaining.
   *
   * @param consistency The database read consistency.
   * @return The database configuration.
   * @throws java.lang.NullPointerException If the consistency is {@code null}
   */
  public DatabaseConfig withConsistency(String consistency) {
    setConsistency(consistency);
    return this;
  }

  /**
   * Sets the database read consistency, returning the configuration for method chaining.
   *
   * @param consistency The database read consistency.
   * @return The database configuration.
   * @throws java.lang.NullPointerException If the consistency is {@code null}
   */
  public DatabaseConfig withConsistency(Consistency consistency) {
    setConsistency(consistency);
    return this;
  }

  @Override
  public CoordinatedResourceConfig resolve(ClusterConfig cluster) {
    return new StateLogConfig(toMap())
      .resolve(cluster)
      .withResourceType(DefaultDatabase.class);
  }

}
