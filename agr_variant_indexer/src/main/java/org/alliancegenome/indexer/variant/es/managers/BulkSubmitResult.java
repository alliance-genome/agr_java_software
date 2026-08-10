package org.alliancegenome.indexer.variant.es.managers;

import java.util.List;

import lombok.Getter;

@Getter
public class BulkSubmitResult {

	public enum Status {
		SUCCESS,
		PARTIAL_FAILURE,
		FULL_FAILURE
	}

	private final Status status;
	private final List<byte[]> failedDocs;
	private final String reason;
	private final boolean reconnectNeeded;
	private final boolean sleepNeeded;

	private BulkSubmitResult(Status status, List<byte[]> failedDocs, String reason, boolean reconnectNeeded, boolean sleepNeeded) {
		this.status = status;
		this.failedDocs = failedDocs;
		this.reason = reason;
		this.reconnectNeeded = reconnectNeeded;
		this.sleepNeeded = sleepNeeded;
	}

	public static BulkSubmitResult success() {
		return new BulkSubmitResult(Status.SUCCESS, null, null, false, false);
	}

	public static BulkSubmitResult partialFailure(List<byte[]> failedDocs, String reason) {
		return new BulkSubmitResult(Status.PARTIAL_FAILURE, failedDocs, reason, false, false);
	}

	public static BulkSubmitResult fullFailure(String reason, boolean reconnectNeeded, boolean sleepNeeded) {
		return new BulkSubmitResult(Status.FULL_FAILURE, null, reason, reconnectNeeded, sleepNeeded);
	}
}
