package org.ctpd.closfwd;

public class MacResponse {
	final static int DROP = 0;					// Drop Packet
	final static int USE_INT_CTPD_FAKE_MAC= 1;	// Use Internal Ctpd fake Mac Address
	final static int USE_EXT_CTPD_FAKE_MAC= 2;	// Use External Ctpd fake Mac Address
	/*Pedro*/
	final static int TO_SERVICE= 3;	// Respond to service using Fake Host Mac Address
	final static int TO_VPDC= 4;	// Respond to VPDC using Specific Service Mac Address.
	/*Pedro*/
}