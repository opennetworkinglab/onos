package org.onosproject.net;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onosproject.net.resource.ResourceId;

/**
 * Representation of a tunnel.
 */
public class TunnelId implements ResourceId {

	private long tunnelId;

	private TunnelId(long tunnelId) {
		this.tunnelId = tunnelId;
	}

	public static TunnelId tunnelId(long tunnelId) {
		return new TunnelId(tunnelId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tunnelId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof TunnelId) {
			final TunnelId other = (TunnelId) obj;
			return Objects.equals(this.tunnelId, other.tunnelId);
		}
		return false;
	}

	@Override
	public String toString() {
		return toStringHelper(this).add("tunnelId", tunnelId).toString();
	}
}
