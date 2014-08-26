package org.onlab.onos.of.controller.impl.registry;

public class IdBlock {
    private final long start;
    private final long end;
    private final long size;

    public IdBlock(long start, long end, long size) {
        this.start = start;
        this.end = end;
        this.size = size;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "IdBlock [start=" + start + ", end=" + end + ", size=" + size
                + "]";
    }
}

