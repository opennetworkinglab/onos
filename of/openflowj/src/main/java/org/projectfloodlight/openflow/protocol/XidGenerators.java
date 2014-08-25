package org.projectfloodlight.openflow.protocol;

import java.util.concurrent.atomic.AtomicLong;

public class XidGenerators {
    private static final XidGenerator GLOBAL_XID_GENERATOR = new StandardXidGenerator();

    public static XidGenerator create() {
        return new StandardXidGenerator();
    }

    public static XidGenerator global() {
        return GLOBAL_XID_GENERATOR;
    }
}

class StandardXidGenerator implements XidGenerator {

    private final AtomicLong xidGen = new AtomicLong();
    long MAX_XID = 0xFFffFFffL;

    @Override
    public long nextXid() {
        long xid;
        do {
            xid = xidGen.incrementAndGet();
            if(xid > MAX_XID) {
                synchronized(this) {
                    if(xidGen.get() > MAX_XID) {
                        xidGen.set(0);
                    }
                }
            }
        } while(xid > MAX_XID);
        return xid;
    }

}