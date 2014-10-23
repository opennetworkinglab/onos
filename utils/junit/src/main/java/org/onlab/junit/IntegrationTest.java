package org.onlab.junit;

/**
 * Marker interface used to separate unit tests from integration tests. All
 * integration tests should be marked with:
 * {@literal @Category}(IntegrationTest.class)
 * so that they can be run separately.
 */
public interface IntegrationTest {
}
