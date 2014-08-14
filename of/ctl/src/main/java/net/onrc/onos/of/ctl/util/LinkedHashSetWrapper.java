package net.onrc.onos.of.ctl.util;

import java.util.Collection;
import java.util.LinkedHashSet;

import com.google.common.collect.ForwardingCollection;

/**
 * A simple wrapper / forwarder that forwards all calls to a LinkedHashSet.
 * This wrappers sole reason for existence is to implement the
 * OrderedCollection marker interface.
 *
 */
public class LinkedHashSetWrapper<E>
        extends ForwardingCollection<E> implements OrderedCollection<E> {
    private final Collection<E> delegate;

    public LinkedHashSetWrapper() {
        super();
        this.delegate = new LinkedHashSet<E>();
    }

    public LinkedHashSetWrapper(Collection<? extends E> c) {
        super();
        this.delegate = new LinkedHashSet<E>(c);
    }

    @Override
    protected Collection<E> delegate() {
        return this.delegate;
    }
}
