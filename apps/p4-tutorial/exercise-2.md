# Exercise 2 (ONOS+P4 Tutorial)

The goal of this exercise is to demonstrate how ONOS apps can be used to
control any P4-defined pipeline, even those implementing custom non-standard
protocols.

## Overview

Similarly to exercise 1, here we want to provide connectivity between hosts of a
network when using switches programmed with the `mytunnel.p4` program.
Differently from exercise 1, forwarding between hosts will be provided by the
MyTunnel app, instead of Reactive Forwarding. The MyTunnel app provides
connectivity by programming the data plane to forward packets using the MyTunnel
protocol.

Before starting, we suggest to open the `$ONOS_ROOT/apps/p4-tutorial` directory in
your editor of choice for easier access to the different files of this
exercise. For example, if using the Atom editor:

```
$ atom $ONOS_ROOT/apps/p4-tutorial/
```

## Protocol overview

The MyTunnel protocol works by encapsulating IPv4 frames into a MyTunnel header
defined as following:

```
header my_tunnel_t {
    bit<16> proto_id; /* EtherType of the original
                         unencapsulated Ethernet frame */
    bit<32> tun_id;   /* Arbitrary tunnel identifier uniquelly
                         representing the egress endpoint of the tunnel */
}
```

A switch implementing the MyTunnel protocol can forward packets using three
different forwarding behaviors.

1. **Ingress**: for IPv4 packets received at an edge switch, i.e., the first
   node in the tunnel path, the MyTunnel header is applied with an arbitrary
   tunnel identifier decided by the control plane.

2. **Transit**: for packets with the MyTunnel header processed by an
   intermediate node in the tunnel path. When operating in this mode, the switch
   forwards the packet by simply looking at the tunnel ID field.

3. **Egress**: for packets with the MyTunnel header processed by the last node
   in the path, the switch removes the MyTunnel header before forwarding the
   packet to the output port.

## MyTunnel pipeline overview

The three forwarding behaviors described before can be achieved by inserting
entries in two different tables of `mytunnel.p4`, namely `t_tunnel_ingress` and
`t_tunnel_fwd`.

* `t_tunnel_ingress`: this table is used to implement the ingress behavior. It
matches on the IPv4 destination address (longest-prefix match), and provides
the `my_tunnel_ingress` action, which encapsulates the packet in the MyTunnel
header with a given tunnel ID (action parameter).

* `t_tunnel_fwd`: this table is used to implement both the transit and egress
behaviors. It matches on the tunnel ID, and allows two different actions,
`set_out_port` and `my_tunnel_egress`. `set_out_port` is used to set the
output port where the packet should be transmitted without further
modifications. With `my_tunnel_egress`, the packet is stripped of the MyTunnel
header before setting the output port.

## MyTunnel app overview

To begin, open [MyTunnelApp.java](mytunnel/src/main/java/org/onosproject/p4tutorial/mytunnel/MyTunnelApp.java)
in your editor of choice, and familiarize with the app implementation.

For example, if using the Atom editor:

```
$ atom $ONOS_ROOT/apps/p4-tutorial/mytunnel/src/main/java/org/onosproject/p4tutorial/mytunnel/MyTunnelApp.java
```

The MyTunnel app works by registering an event listener with the ONOS Host
Service (`class InternalHostListener` at line 308). This listener is used to
notify the MyTunnel app every time a new host is discovered. Host discovery is
performed using two ONOS core services: Host Location Provider and Proxy-ARP
app. Each time an ARP request is received (via packet-in), ONOS learns the
location of the sender of the ARP request, before generating an ARP reply or
forwarding the requests to other hosts. When learning the location of a new
host, ONOS informs all apps that have registered a listener with an `HOST_ADDED`
event.

Once a `HOST_ADDED` event is notified to the MyTunnel app, this creates two
unidirectional tunnels between that host and any other host previously
discovered. For each tunnel, the app computes the shortest path between the two
hosts (method `provisionTunnel` at line 128), and for each switch in the path it
installs flow rules for the `t_tunnel_ingress` table (method
`insertTunnelIngressRule` at line 182), and/or the `t_tunnel_fwd` table (method
`insertTunnelForwardRule` at line 219), depending on the position of the switch
in the path, the app will install rule to perform the ingress, transit, or
egress behaviors.

## Exercise steps

1. **Complete the implementation of the MyTunnel app**:

    1. Open [MyTunnelApp.java](mytunnel/src/main/java/org/onosproject/p4tutorial/mytunnel/MyTunnelApp.java) in your editor of choice.

    2. Look for the `insertTunnelForwardRule` method (line 219).

    3. Complete the implementation of this method (There's a `TODO EXERCISE`
    comment at line 251).

        **Spoiler alert:** There is a reference solution in the same directory
        as `MyTunnelApp.java`. Feel free to compare your implementation to the
        reference one.

2. **Start ONOS with all the apps required**.

    0. If ONOS is still running from the previous exercise, stop it by pressing
       `ctrl-c` in the ONOS log terminal window.

       This is required since we need to re-build ONOS to reflect the
       `MyTunnelApp.java` changes you just implemented. There is a way to build
       just the app and upload that to ONOS at runtime, but this functionality
       is not covered in this tutorial.

    1. On a first terminal window, start ONOS:

        ```
        $ cd $ONOS_ROOT
        $ ONOS_APPS=proxyarp,hostprovider,lldpprovider ok clean
        ```

        This command will automatically trigger a new build before starting ONOS.

        **Important!** Remember to save your changes to `MytunnelApp.java`
        before building and starting ONOS.

    2. On a second terminal window, use the ONOS CLI to activate the MyTunnel
       pipeconf and app.

        To access the ONOS CLI:

        ```
        $ onos localhost
        ```

        **Activate the BMv2 drivers, pipeconf, and MyTunnel app**:

        ```
        onos> app activate org.onosproject.drivers.bmv2
        onos> app activate org.onosproject.p4tutorial.pipeconf
        onos> app activate org.onosproject.p4tutorial.mytunnel
        ```

        **Hint:** To avoid accessing the CLI to start all applications, you can
        modify the value of the `ONOS_APPS` variable when starting ONOS. For
        example:

        ```
        $ cd $ONOS_ROOT
        $ ONOS_APPS=proxyarp,hostprovider,lldpprovider,drivers.bmv2,p4tutorial.pipeconf,p4tutorial.mytunnel ok clean
        ```

    3. **Check that all apps have been activated successfully**:

        ```
        onos> apps -s -a
        ```

        You should see an output like this:

        ```
        org.onosproject.hostprovider          ... Host Location Provider
        org.onosproject.lldpprovider          ... LLDP Link Provider
        org.onosproject.proxyarp              ... Proxy ARP/NDP
        org.onosproject.drivers               ... Default Drivers
        org.onosproject.protocols.grpc        ... gRPC Protocol Subsystem
        org.onosproject.protocols.p4runtime   ... P4Runtime Protocol Subsystem
        org.onosproject.p4runtime             ... P4Runtime Provider
        org.onosproject.generaldeviceprovider ... General Device Provider
        org.onosproject.drivers.p4runtime     ... P4Runtime Drivers
        org.onosproject.p4tutorial.pipeconf   ... P4 Tutorial Pipeconf
        org.onosproject.pipelines.basic       ... Basic Pipelines
        org.onosproject.protocols.gnmi        ... gNMI Protocol Subsystem
        org.onosproject.drivers.gnmi          ... gNMI Drivers
        org.onosproject.drivers.bmv2          ... BMv2 Drivers
        org.onosproject.p4tutorial.mytunnel   ... MyTunnel Demo App
        ```

    4. (optional) **Change flow rule polling interval**. Run the following
       command in the ONOS CLI:

        ```
        onos> cfg set org.onosproject.net.flow.impl.FlowRuleManager fallbackFlowPollFrequency 5
        ```

3. **Run Mininet to set up a tree topology of BMv2 switches**, on a new terminal
   window type:

    ```
    $ sudo -E mn --custom $BMV2_MN_PY --switch onosbmv2,pipeconf=p4-tutorial-pipeconf --topo tree,3 --controller remote,ip=127.0.0.1
    ```

4. **Check that all devices, link, and hosts have been discovered correctly in ONOS**.

    1. To check the devices, on the ONOS CLI, type:

        ```
        onos> devices -s
        ```

        The `-s` argument provides a more compact output.

        You should see 7 devices in total. Please note the driver that has been
        assigned to this device `bmv2:p4-tutorial-pipeconf`. It means that the
        device is being controlled using the "behaviors" provided by the BMv2
        driver (which uses P4Runtime) plus the pipeconf ones.

    2. Check the links:

        ```
        onos> links
        ```

        The `-s` argument provides a more compact output.

        You should see 12 links (the topology has 6 bidirectional links in total).

    3. Check the hosts:

        ```
        onos> hosts -s
        ```

        You should see 0 hosts, as we have not injected any ARP packet yet.

5. **Ping hosts**, on the Mininet CLI, type:

    ```
    mininet> h1 ping h7
    ```

    If the implementation of `MyTunnelApp.java` has been completed correctly,
    ping should work. If not, check the ONOS log for possible errors in the
    MyTunnel app. As a last resort, please check the reference solution in
    the same directory as `MyTunnelApp.java` and compare that to yours.

    There are 7 hosts, in the network from `h1` to `h8`. You should be able to
    get ping working between all pairs of hosts.

6. **Look around**:

    1. Repeat step 3.v and 3.vi from exercise one to check the
       flow rules in ONOS and on BMv2.

    2. Verify that hosts have been discovered by ONOS:

        ```
        onos> hosts -s
        ```

        You should see all the hosts that you pinged so far.

7. **Use Wireshark to dump packets with MyTunnel header**:

    1. Using the ONOS web UI, find the port number of any switch that forwards
       packets with the MyTunnel header applied. This will be a port of any
       link connecting two switches.

        * Open the ONOS UI at <http://127.0.0.1:8181/onos/ui/>. To log in, use
          username `onos` and password `rocks`.

        * To show/hide switch names, press the `L` key on your keyboard.

        * To see the switch port number for a particular link, position the
          mouse over the link, you should see two numbers at the two link
          endpoints. This will be your port number.
 
         * For example, an internal-facing port carrying packets with the
          MyTunnel header applied is port `3` of device `s3`.

    2. Open Wireshark using the icon located in the desktop, or by typing
       `sudo wireshark` in a terminal window.
 
     3. Start packet capture on the switch port you identified. Mininet uses the
       naming `[switch_name]-eth[port_number]` for the emulated switch ports.
       For example, for port `3` of switch `s3`, the interface name will be
       `s3-eth3`.

    4. Start a ping in Mininet between any two hosts which path uses the
       identified link/port.

        * In the ONOS UI, to show/hide hosts, press the `H` key on your keyboard.

        * For example, the path between `h1` (10.0.0.1) and `h4` (10.0.0.4)
          uses port `3` of switch `s3`.

        * Start ping in Mininet:

            ```
            mininet> h1 ping h4
            ```

    5. Analyze the captured packets in Wireshark.

        * Packets with protocol `LLDP` and `0x8942` (EtherType) are generated by
          ONOS using P4Runtime "packet-out" and are used for link discovery.

        * Packets with EtherType `0x1212` are ping packets encapsulated with the
          MyTunnel header! Indeed, since this is not a standard protocol,
          Wireshark doesn't know how to parse the Ethernet payload and shows
          them as general Ethernet packets.

8. **Congratulations, you have completed the exercise!**

    Hopefully, it should be a bit more clear by now how to use P4 to implement
    custom header processing (like the `MyTunnel` header), and how to write ONOS
    apps that control the match-action tables of switches to forward packets
    across the network using such non-standard headers (like MyTunnelApp).
