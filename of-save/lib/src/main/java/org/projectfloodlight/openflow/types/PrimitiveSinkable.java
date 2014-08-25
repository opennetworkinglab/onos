package org.projectfloodlight.openflow.types;

import com.google.common.hash.PrimitiveSink;

public interface PrimitiveSinkable {
    public void putTo(PrimitiveSink sink);
}
