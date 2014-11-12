package org.onlab.onos.store.service;

import com.google.common.base.MoreObjects;


/**
 * Database write result.
 */
public class WriteResult {
	
    private final WriteStatus status;
    private final VersionedValue previousValue;
    
    public WriteResult(WriteStatus status, VersionedValue previousValue) {
    	this.status = status;
        this.previousValue = previousValue;
    }

    public VersionedValue previousValue() {
        return previousValue;
    }
    
    public WriteStatus status() {
    	return status;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
        		.add("status", status)
                .add("previousValue", previousValue)
                .toString();
    }
}
