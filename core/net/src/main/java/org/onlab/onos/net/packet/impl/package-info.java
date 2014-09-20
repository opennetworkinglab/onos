/**
 * Core subsystem for processing inbound packets and emitting outbound packets.
 * Processing of inbound packets is always in the local context only, but
 * emitting outbound packets allows for cluster-wide operation.
 */
package org.onlab.onos.net.packet.impl;
