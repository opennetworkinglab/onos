package org.onlab.onos.store.service;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Collection of read requests to be submitted as one batch.
 */
public class BatchReadRequest {

	private final List<ReadRequest> readRequests;

	/**
	 * Creates a new BatchReadRequest object from the specified list of read requests.
	 * @param readRequests read requests.
	 * @return BatchReadRequest object.
	 */
	public static BatchReadRequest create(List<ReadRequest> readRequests) {
		return new BatchReadRequest(readRequests);
	}

	private BatchReadRequest(List<ReadRequest> readRequests) {
		this.readRequests = Collections.unmodifiableList(readRequests);
	}

	/**
	 * Returns the number of requests in this batch.
	 * @return size of request batch.
	 */
	public int batchSize() {
		return readRequests.size();
	}

	/**
	 * Returns the requests in this batch as a list.
	 * @return list of read requests
	 */
	public List<ReadRequest> getAsList() {
		return readRequests;
	}

	/**
	 * Builder for BatchReadRequest.
	 */
	public static class Builder {

		private final List<ReadRequest> readRequests = Lists.newLinkedList();

		/**
		 * Append a get request.
		 * @param tableName table name
		 * @param key key to fetch.
		 * @return this Builder
		 */
		public Builder get(String tableName, String key) {
			readRequests.add(new ReadRequest(tableName, key));
			return this;
		}

		/**
		 * Builds a BatchReadRequest
		 * @return BatchReadRequest
		 */
		public BatchReadRequest build() {
			return new BatchReadRequest(readRequests);
		}
	}
}