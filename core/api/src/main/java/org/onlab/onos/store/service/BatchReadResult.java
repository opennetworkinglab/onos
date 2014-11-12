package org.onlab.onos.store.service;

import java.util.Collections;
import java.util.List;

public class BatchReadResult {
	
	private final List<ReadResult> readResults;
	
	public BatchReadResult(List<ReadResult> readResults)  {
		this.readResults = Collections.unmodifiableList(readResults);
	}
	
	public List<ReadResult> getAsList() {
		return readResults;
	}
	
	public int batchSize() {
		return readResults.size();
	}
}