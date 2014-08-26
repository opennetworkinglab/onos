package org.onlab.onos.of.controller.impl.util;

import java.util.Collection;

/**
 * A marker interface indicating that this Collection defines a particular
 * iteration order. The details about the iteration order are specified by
 * the concrete implementation.
 *
 * @param <E>
 */
public interface OrderedCollection<E> extends Collection<E> {

}
