package org.projectfloodlight.openflow.util;

import java.util.List;

import org.projectfloodlight.openflow.types.PrimitiveSinkable;

import com.google.common.hash.PrimitiveSink;

public class FunnelUtils {
    public static void putList(List<? extends PrimitiveSinkable> sinkables, PrimitiveSink sink) {
        for(PrimitiveSinkable p: sinkables)
            p.putTo(sink);
    }
}
