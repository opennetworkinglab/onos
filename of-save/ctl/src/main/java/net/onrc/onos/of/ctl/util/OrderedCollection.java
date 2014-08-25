package net.onrc.onos.of.ctl.util;

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
