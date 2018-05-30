Here is an answer to the questions that you can compare with your own.

* **Which protocol headers are being extracted from each packet?**
 There are three headers defined for packets from the controlplane (ethernet_t, my_tunnel_t and ipv4_t) and two headers for packets from/to controller (packet_out_header_t and packet_in_header_t ).

* **How can the parser distinguish a packet with MyTunnel encapsulation from one without?**
  This is done checking  the value of  the ether_type field. If ETH_TYPE_MYTUNNEL = 0x1212 this is a packet with MyTunnel encapsulation.

* **How many match+action tables are defined in the P4 program?**
  There are three tables defined: t_l2_fwd, t_tunnel_ingress and t_tunnel_fwd.

* **What is the first table in the pipeline applied to every packet?**
  The first table in the pipeline is t_l2_fwd.

* **Which headers can be matched on table t_l2_fwd?**
            hdr.ethernet.dst_addr,  hdr.ethernet.src_addr,  and hdr.ethernet.ether_type.

* **Which type of match is applied to t_l2_fwd? E.g. exact match, ternary, or longest-prefix match?**
	Ternary.

* **Which actions can be executed on matched packets?**
  set_out_port, send_to_cpu, _drop, NoAction.

* **Which action can be used to send a packet to the controller?**
  action send_to_cpu()

* **What happens if a matching entry is not found in table t_l2_fwd? What's the next table applied to the packet?**
  If a matching entry is not found in table t_l2_fwd the next table applied is t_tunnel_ingress to process only non-tunneled IPv4 packets or t_tunnel_fwd for tunneled packets.
