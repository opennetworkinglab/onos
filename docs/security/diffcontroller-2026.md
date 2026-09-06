## Purpose

This PR adds an external disclosure record for the ONOS candidates in an
academic differential-testing study. The disclosure set contains 24 report
records overall: 23 historical candidate IDs plus a current replay addendum.
The historical label records a two-author classification at the 2023 discovery
cutoff; it is not maintainer confirmation, a CVE, or a claim that every row is
a current vulnerability.

Each row below is a request for validation. `L1` means that the stated
differential signal was reproduced in the stated local harness. `unvalidated`
means that the current run did not establish the original field-specific
effect. Raw OpenFlow payloads and mutation automation are intentionally not
included in this PR.

## Tested target

- Project: `opennetworkinglab/onos`
- Historical target: ONOS 2.5.0, OpenFlow 1.3, isolated Mininet/OVS harness
- Current supplemental target for DC-026: `onosproject/onos:latest`, content
  digest `sha256:c3d8941030a6bc8a375a49270dc3179cf1f62840b5f037c2d5e09d6dfbe55937`
- Artifact and methodology: <https://github.com/Drone-Lab/DiffController>

## Candidate records

| ID | Finding | Message/action and parameter | Current replay | Current boundary |
|---|---|---|---|---|
| DC-001 | Undefined `port_no` in PortStatus | `of_port_status`; BUILD; `port_no=0xFFFFFFF7` | L1 | robustness signal; validate scope |
| DC-002 | Undefined `port_no` in PacketIn | `of_packet_in`; MOD; `in_port=0xFFFFFFF7` | L1 | robustness signal; validate scope |
| DC-003 | Missing `in_port` in PacketIn | `of_packet_in`; DEL; remove match | L1, narrowed | ONOS topology/list effect reproduced; validate intended behavior |
| DC-005 | Undefined role in RoleReply | `of_role_reply`; MOD; `role=0xaa1da1d5` | L1 | robustness signal; validate scope |
| DC-006 | Undefined group ID | group-desc stats; BUILD; `group_id=0xFFFFFFFD` | L1, constructed | constructed signal; validate scope |
| DC-007 | Undefined group type | group-desc stats; BUILD; `group_type=0x04` | L1, constructed | constructed signal; validate scope |
| DC-008 | Undefined PacketIn reason | `of_packet_in`; MOD; `reason=0x72` | L1 | robustness signal; validate scope |
| DC-009 | Undefined FlowRemoved reason | `of_flow_removed`; BUILD; `reason=0x3C` | L1, constructed | constructed signal; validate scope |
| DC-010 | Undefined PortStatus reason | `of_port_status`; BUILD; `reason=0x3A` | L1 | exact historical ONOS result; validate scope |
| DC-011 | Wrong-direction FlowStatsRequest after RoleReply | `of_flow_stats_request`; BUILD; `vlan_vid=0x6b2e` | unvalidated | field-specific effect not established |
| DC-012 | Unsolicited FlowStatsReply after FeaturesReply | `of_flow_stats_reply`; BUILD; `icmpv6_code=0xe1` | unvalidated | field-specific effect not established |
| DC-013 | Undefined `vlan_pcp` in flow stats match | `of_flow_stats_reply`; BUILD; `vlan_pcp=0xec` | L1, constructed | robustness signal; validate scope |
| DC-014 | Undefined `ip_dscp` in flow stats match | `of_flow_stats_reply`; BUILD; `ip_dscp=0xb2` | L1, constructed | robustness signal; validate scope |
| DC-015 | Controller-to-switch message sent in reverse | `of_desc_stats_request`; BUILD | L1 | robustness signal; validate scope |
| DC-016 | RoleReply without RoleRequest | `of_role_reply`; BUILD | L1, narrowed | ONOS/PacketIn scope reproduced; validate intended recovery |
| DC-018 | GetConfigReply without GetConfigRequest | `of_get_config_reply`; BUILD | L1, narrowed | robustness signal; validate scope |
| DC-020 | PortDesc multipart reply without request accepted | `of_port_desc_stats_reply`; BUILD | L1 | robustness signal; validate scope |
| DC-022 | Packet truncation / `total_len` mismatch | `of_packet_in`; MOD; `total_len=0x22` | unvalidated | no reproducible current security impact |
| DC-024 | FlowRemoved removes controller-stored flow entries | `of_flow_removed`; BUILD | L1 | robustness signal; validate scope |
| DC-026 | RoleRequest/RoleReply role mismatch | `of_role_reply`; MOD; `role=0x03` | L1 | attached-switch availability candidate; no whole-controller outage claim |

## DC-026 current replay

Against the digest-pinned `onosproject/onos:latest` image, ten independently
reset paired local runs used an identity relay as control and a relay that
changed only a genuine same-XID RoleReply from MASTER to SLAVE after the
corresponding RoleRequest. The two-host ping showed 0% loss in 10/10 controls
and 100% loss in 10/10 mutation arms. The REST device endpoint returned HTTP
200 after every arm and the relay observed no post-target connection close.

This establishes a bounded attached-switch data-plane availability effect in
the tested setup. It does not establish a controller-process crash, REST
outage, cross-switch impact, full-deployment impact, an affected-version range,
a CVE, or maintainer acknowledgement. Please determine whether ONOS should
reject or recover safely from a same-transaction RoleReply that contradicts a
single-controller MASTER request and the switch's original reply.

## Requested action

Please review the rows, confirm the intended protocol/state-machine behavior,
and point to any existing issue, advisory, fix, or preferred private security
channel. We can provide minimal redacted reproducer material, paired controls,
and artifact hashes through a maintainer-selected channel.
