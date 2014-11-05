package org.onlab.onos.store.service;


/**
 * Exception that indicates a precondition failure.
 * Scenarios that can cause this exception:
 * <ul>
 * <li>An operation that attempts to write a new value iff the current value is equal
 * to some specified value.</li>
 * <li>An operation that attempts to write a new value iff the current version
 * matches a specified value</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class PreconditionFailedException extends DatabaseException {
}