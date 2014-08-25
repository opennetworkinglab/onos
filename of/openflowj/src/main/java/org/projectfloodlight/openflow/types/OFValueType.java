package org.projectfloodlight.openflow.types;




public interface OFValueType<T extends OFValueType<T>> extends Comparable<T>, PrimitiveSinkable {
    public int getLength();

    public T applyMask(T mask);

}
