package org.onlab.onos.store.service;

import java.util.Collections;
import java.util.List;

public class BatchWriteResult {
	
	private final List<WriteResult> writeResults;
	
	public BatchWriteResult(List<WriteResult> writeResults) {
		this.writeResults = Collections.unmodifiableList(writeResults);
	}
	
	public boolean isSuccessful() {
		for (WriteResult result : writeResults) {
			if (result.status() != WriteStatus.OK) {
				return false;
			}
		}
		return true;
	}
	
	public List<WriteResult> getAsList() {
		return this.writeResults;
	}
	
	public int batchSize() {
		return writeResults.size();
	}
}