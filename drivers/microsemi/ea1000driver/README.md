#Microsemi Edge Assure 1000 SFP-NID
This driver allows connection to the Microsemi Edge Assure 1000 SFP-NID
[EdgeAssure 1000 Product Page](https://www.microsemi.com/existing-parts/parts/137346)

The User Guide for this product is available on request from Microsemi, and gives a full explanation of the theory of operation, functionality and how all functions can be accessed through the NETCONF interface only.<br/>

Currently only a subset of it's functionality is supported through ONOS, but this will expand to full functionality in future releases.

#Compile and Installation
Currently this driver is **not** built using BUCK (because it depends on an older version of onos-yang-tools, while BUCK points to the newer version of the onos-yang-tools)<br/>

Before this driver can work successfully one problem with the Yang Management System (YMS) App has to be taken in to account. This is that the Yang Codec Handler (YCH) part needs to be built in Maven (when compiled in Buck something is not configured properly).<br/>

To build YMS with Maven:
1. Change directory to onos/apps/yms/app
2. Run the command "mvn clean install" (or use the shortcut 'mci')
3. With onos running, **reinstall** the generated OAR file for YMS on the target machine (could be localhost - replace {bracketed} values with real values)
    * onos-app {onos-server} reinstall! target/onos-app-yms-{version}.oar


Then this Microsemi driver has to be built using Maven and installed. To build it:
1. Change directory to onos/drivers/microsemi
2. Run the command "mvn clean install" (or use the shortcut 'mci')
3. With onos running, install the 2 generated OAR files on the target machine (could be localhost - replace {bracketed} values with real values)
    * onos-app {onos-server} install ea1000yang/target/onos-drivers-microsemi-ea1000yang-{version}.oar
    * onos-app {onos-server} install ea1000driver/target/onos-drivers-microsemi-ea1000-{version}.oar
4. Verify that they are installed by calling **apps -s | grep microsemi** at the onos> prompt
5. Activate the modules at the onos prompt
    * onos:app activate org.onosproject.drivers.netconf org.onosproject.drivers.microsemi.yang org.onosproject.drivers.microsemi

#Change NETCONF default connection timeout
Connection timeouts need to be increased from default values when using EA1000. At ONOS command line run

`onos:cfg set org.onosproject.netconf.ctl.NetconfControllerImpl netconfConnectTimeout 150`<br/>
`onos:cfg set org.onosproject.netconf.ctl.NetconfControllerImpl netconfReplyTimeout 150`<br/>

#Creating Devices
EA1000 Devices will not be automatically discovered at present in ONOS. They have to be created through the network/configuration REST interface in ONOS.

* The name must follow the format **netconf:<ipaddr>:<port>**
* The **ip** and **port** must correspond to the ip and port in the name (above).

`{`<br/>
`&nbsp;"devices": {`<br/>
`&nbsp;&nbsp;"netconf:192.168.56.10:830": {`<br/>
`&nbsp;&nbsp;&nbsp;"netconf": {`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"username": "admin",`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"password": "admin",`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"ip": "192.168.56.10",`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"port": "830"`<br/>
`&nbsp;&nbsp;&nbsp;},`<br/>
`&nbsp;&nbsp;&nbsp;"basic": {`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;"driver": "microsemi-netconf",`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;"type": "SWITCH",`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;"manufacturer": "Microsemi",`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;"hwVersion": "EA1000"`<br/>
`&nbsp;&nbsp;&nbsp;}`<br/>
`&nbsp;&nbsp;}`<br/>
`&nbsp;}`<br/>
`}`<br/>



#Connected Device
When the EA1000 is configured and connected is should be visible in ONOS through the **devices** command.

`onos> devices`<br/>
`id=netconf:192.168.56.10:830, available=true, local-status=connected 33s ago, role=MASTER, type=SWITCH, mfr=Microsemi, hw=EA1000, sw=4.4.0-53-generic, serial=Eagle Simulator., driver=microsemi-netconf, ipaddress=192.168.56.10, latitude=51.8865467, locType=geo, longitude=-8.4040440, name=netconf:192.168.56.10:830, port=830, protocol=NETCONF`

Note how the
* software version (sw=**4.4.0-53-generic**)
* serial number (serial=**Eagle Simulator.**)
* latitude (latitude=**51.8865467**) and
* longitude (longitude=**-8.4040440**)
are all retrieved from the device on initial handshake.

In addition the time on the device is checked at this stage, and if it wrong by more than 1 day (it defaults to 1-1-1970 on startup if no NTP server is configured), then the current time is written to it at this stage. This will persist on the device until next reboot.

Also the ports of the device will be visible after connection. There are 2 ports
* Port 0 - The **Optics** port - this is a single mode 1000LX 1310nm optical connection
* Port 1 - The **Host** port - this is a 1GB Ethernet Copper connection in to an SFP Port

`onos> ports`<br/>
`id=netconf:192.168.56.10:830, available=true, local-status=connected 15s ago, role=MASTER, type=SWITCH, mfr=Microsemi, hw=EA1000, sw=4.4.0-53-generic, serial=Eagle Simulator., driver=microsemi-netconf, ipaddress=192.168.56.10, latitude=51.8865467, locType=geo, longitude=-8.4040440, name=netconf:192.168.56.10:830, port=830, protocol=NETCONF`<br/>
`&nbsp;port=0, state=enabled, type=fiber, speed=1000, portName=Optics`<br/>
`&nbsp;port=1, state=enabled, type=copper, speed=1000, portName=Host`<br/>


#OpenFlow Emulation
Currently the EA1000 supports only a limited set of OpenFlow rules through the Flows REST API and the Flow Objective API.

##IP Source Address filtering
A feature of the EA1000 that may be configured through Flow Rules is IP Source Address Filtering. This can only be activated on Port 0 (the optics Port). An example of this kind of flow is

`POST /onos/v1/flows/ HTTP/1.1`<br/>
`{`<br/>
`&nbsp;"flows": [`<br/>
`&nbsp;&nbsp;{`<br/>
`&nbsp;&nbsp;&nbsp;"priority": 40000,`<br/>
`&nbsp;&nbsp;&nbsp;"timeout": 0,`<br/>
`&nbsp;&nbsp;&nbsp;"isPermanent": true,`<br/>
`&nbsp;&nbsp;&nbsp;"deviceId": "netconf:192.168.56.10:830",`<br/>
`&nbsp;&nbsp;&nbsp;"tableId": 8,`<br/>
`&nbsp;&nbsp;&nbsp;"treatment": {`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;"instructions": [  {"type": "NOACTION"} ],`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;"deferred": []`<br/>
`&nbsp;&nbsp;&nbsp;},`<br/>
`&nbsp;&nbsp;&nbsp;"selector": {`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;"criteria": [  {`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"type": "IPV4_SRC", "ip": "192.168.8.0/24"`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;},{`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"type": "IN_PORT", "port": "0"`<br/>
`} ] } } ] }`<br/>

## Vlan Tag Manipulation
**Note: Before Vlan Tag manipulation can be done the mode of the interface on the EA1000 has to be changed to be compatible with the type of tags that are being received. If this is not done, once an EVC is created the packets that have the Vlan tagged on will be treated as untagged by EA1000, and dropped if there is no EVC present corresponding to the interface PVID. See the section below on the "Setting EA1000 Interface Tagging Mode"**

Flows that Push, Pop or Overwrite VLAN tags are implemented in EA1000 and are treated as MEF Carrier Ethernet EVCs. Both CTags and STags can be pushed on to matching Ethernet packets at network Layer 2.

`POST /onos/v1/flows/ HTTP/1.1`<br/>
`{`<br/>
`  "flows": [`<br/>
`    {`<br/>
`      "priority": 50000,`<br/>
`      "timeout": 0,`<br/>
`      "isPermanent": true,`<br/>
`      "tableId": 6,` --This sets the EVC id  - this first flow is configuring the customer side - UNI-C<br/>
`      "deviceId": "netconf:192.168.56.10:830",`<br/>
`      "treatment": {`<br/>
`        "instructions": [`<br/>
`          {`<br/>
`            "type": "L2MODIFICATION",`<br/>
`            "subtype": "VLAN_PUSH",` --This pushes a VLAN on<br/>
`            "ethernetType": "0x88a8"`  --The pushed VLAN type is QinQ<br/>
`          },{`<br/>
`            "type": "L2MODIFICATION",`<br/>
`            "subtype": "VLAN_ID",`<br/>
`            "vlanId": "200"`  --The pushed VLAN id is 200<br/>
`          }`<br/>
`        ],`<br/>
`        "deferred": []`<br/>
`      },`<br/>
`      "selector": {`<br/>
`        "criteria": [`<br/>
`         {`<br/>
`          "type": "VLAN_VID",`<br/>
`          "vlanId": "100"` -- Applies only to packets already tagged with VLAN 100..<br/>
`         },{`<br/>
`          "type": "IN_PORT",`<br/>
`          "port": "1"`  -- ..that are coming in on the Host port<br/>
`         }`<br/>
`        ]`<br/>
`      }`<br/>
`    },{`<br/>
`      "priority": 50000,`<br/>
`      "timeout": 0,`<br/>
`      "isPermanent": true,`<br/>
`      "tableId": 6,`  -- The same EVC, but now we are configuring another VLAN<br/>
`      "deviceId": "netconf:192.168.56.10:830",`<br/>
`      "treatment": {`<br/>
`       "instructions": [`<br/>
`         {`<br/>
`          "type": "L2MODIFICATION",`<br/>
`          "subtype": "VLAN_PUSH",`  -- Push again<br/>
`          "ethernetType": "0x88a8"`  -- QinQ again<br/>
`         },{`
`          "type": "L2MODIFICATION",`<br/>
`          "subtype": "VLAN_ID",`<br/>
`          "vlanId": "200"`  -- VLAN 200 again<br/>
`         }`<br/>
`        ],`<br/>
`        "deferred": []`<br/>
`      },`<br/>
`      "selector": {`<br/>
`        "criteria": [`<br/>
`         {`<br/>
`          "type": "VLAN_VID",`<br/>
`          "vlanId": "101"`  -- Applies only to packets already tagged with VLAN 101..<br/>
`         },{`<br/>
`          "type": "IN_PORT",`<br/>
`          "port": "1"`  -- ..that are coming in on the Host port<br/>
`         }`<br/>
`        ]`<br/>
`      }`<br/>
`    },{`<br/>
`      "priority": 50000,`<br/>
`      "timeout": 0,`<br/>
`      "isPermanent": true,`<br/>
`      "tableId": 6,`  -- The same EVC, but now we are configuring the opposite side<br/>
`      "deviceId": "netconf:192.168.56.10:830",`<br/>
`      "treatment": {`<br/>
`       "instructions": [`<br/>
`         {`<br/>
`          "type": "L2MODIFICATION",`<br/>
`          "subtype": "VLAN_POP"`  -- Here we are popping the top level tag<br/>
`         }`<br/>
`       ],`<br/>
`       "deferred": []`<br/>
`      },`<br/>
`      "selector": {`<br/>
`       "criteria": [`<br/>
`        {`<br/>
`         "type": "VLAN_VID",`  -- Applies only to packets tagged with VLAN 200<br/>
`         "vlanId": "200"`<br/>
`        },{`<br/>
`         "type": "IN_PORT",`<br/>
`         "port": "0"`  -- That are coming in on the Optics Port<br/>
`        }`<br/>
`      ] } } ] }`<br/>

##Setting EA1000 Interface Tagging Mode
The Interface of the EA1000 has an attribute **frame_format** that must be set to either:
* none (default)
* ctag
* stag
to correspond to the type of tagging that will be applied to packets received at that port.

For instance if Port 0 of an EA1000 is to receive and process S-Tags, the the frame-format of the interface *eth0* should be set to **stag** in advance.

This should be made a permanent setting in the NETCONF *startup* datastore, so that it will be active if the device reboots.

Likewise the opposite port should be set to *ctag*.<br/>

This changes are not made done through the ONOS Driver, and currently only possible to make this change through a NETCONF CLI client such as *yangcli-pro* or *netopeer-cli*;

See the EA1000 User Guide for details on how to use *yangcli-pro* to access the EA1000.

On an EA1000 accessed through *yangcli-pro* the following commands can be used to make these changes:<br/>
`admin@192.168.2.234> discard-changes`<br/>
`admin@192.168.2.234> copy-config source=startup target=candidate`<br/>
`admin@192.168.2.234> merge /interfaces/interface[name='eth0']/frame-format --value='stag'`<br/>
`admin@192.168.2.234> merge /interfaces/interface[name='eth1']/frame-format --value='ctag'`<br/>
`admin@192.168.2.234> commit`<br/>
`admin@192.168.2.234> copy-config source=running target=startup`<br/>


#ONOS Carrier Ethernet Application
The ONOS [Carrier Ethernet](https://wiki.onosproject.org/display/ONOS/Carrier+Ethernet+Application) application allows EVCs to be created between UNIs that are linked together in ONOS. This is translated down to OpenFlow switches by converting the **ce-evc-create** commands in to Flow Rules similar to those shown above.

While EA1000 is not an OpenFlow switch, it's driver can convert these flows in to NETCONF which can be used to configure EVCs on the EA1000. Because EA1000 is just a 2 port device it represents only 1 UNI (conventionally on switches each port represnts a UNI).

When an EA1000 device is configured in ONOS the Carrier Ethernet Application considers both of its ports to be UNIs, as can be seen in the listing below:

`onos> ce-uni-list`<br/>
`CarrierEthernetUni{id=netconf:192.168.56.10:830/0, cfgId=netconf:192.168.56.10:830/0, role=null, refCount=0, ceVlanIds=[], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[]}`<br/>
`CarrierEthernetUni{id=netconf:192.168.56.10:830/1, cfgId=netconf:192.168.56.10:830/1, role=null, refCount=0, ceVlanIds=[], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[]}`<br/>

This mismatch is handled in the driver - both side appear to be separate here but they will apply to the same single UNI on the EA1000 in opposite directions.

For an EVC to be created there has to an ONOS 'link' between 2 UNIs for a POINT-TO-POINT connection - an Ethernet Virtual Private Line (**EVPL**). There has to be more than 2 UNIs to create a MULTIPOINT-TO-MULTIPOINT link - an E-Lan.

##Links
In a simple scenario that has 2 EA1000s (netconf:192.168.56.10:830 and netconf:192.168.56.20:830) configured in ONOS, the two might be linked together through their optics ports (port 0). The following result is expected for a bi-directional link:

`onos> links`<br/>
`src=netconf:192.168.56.10:830/0, dst=netconf:192.168.56.20:830/0, type=DIRECT, state=ACTIVE, expected=false`<br/>
`src=netconf:192.168.56.20:830/0, dst=netconf:192.168.56.10:830/0, type=DIRECT, state=ACTIVE, expected=false`<br/>

This will not exist by default since Link Discovery is not yet a feature of the EA1000 driver. These have to be created manually - through the network/configuration REST API.

`POST /onos/v1/network/configuration/ HTTP/1.1`<br/>
`{`<br/>
`  "links": {`<br/>
`  "netconf:192.168.56.10:830/0-netconf:192.168.56.20:830/0" : {`  -- 10 to 20<br/>
`    "basic" : {`<br/>
`      "type" : "DIRECT"`<br/>
`    }`<br/>
`  },`<br/>
`  "netconf:192.168.56.20:830/0-netconf:192.168.56.10:830/0" : {`  -- and reverse<br/>
`    "basic" : {`<br/>
`      "type" : "DIRECT"`<br/>
`    } } } }`<br/>

##EVPL Creation
To create a simple EVPL the following command can be used at the ONOS CLI:<br/>

`onos>ce-evc-create --cevlan 101 evpl1 POINT_TO_POINT netconf:192.168.57.10:830/0 netconf:192.168.57.20:830/0`<br/>

This returns without any message. Tailing through the ONOS logs will reveal any error that might have occurred.<br/>

This EVC can be viewed only through the command line:<br/>
`onos> ce-evc-list`<br/>
`  CarrierEthernetVirtualConnection{id=EP-Line-1, cfgId=evpl1, type=POINT_TO_POINT, state=ACTIVE,`<br/>
`UNIs=[`<br/>
`CarrierEthernetUni{id=netconf:192.168.56.10:830/0, cfgId=netconf:192.168.56.10:830/0, role=Root, refCount=0, ceVlanIds=[101], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[CarrierEthernetBandwidthProfile{id=FC-1, type=EVC, cir=0.0, cbs=0, eir=0.0, ebs=0}]}, `<br/>
`CarrierEthernetUni{id=netconf:192.168.56.20:830/0, cfgId=netconf:192.168.56.20:830/0, role=Root, refCount=0, ceVlanIds=[101], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[CarrierEthernetBandwidthProfile{id=FC-1, type=EVC, cir=0.0, cbs=0, eir=0.0, ebs=0}]}], `<br/>
`FCs=[CarrierEthernetForwardingConstruct{id=FC-1, cfgId=null, type=POINT_TO_POINT, vlanId=1, metroConnectId=null, refCount=1, `<br/>
`LTPs=[`<br/>
`CarrierEthernetLogicalTerminationPoint{id=netconf:192.168.56.10:830/0, cfgId=netconf:192.168.56.10:830/0, role=Root, ni=CarrierEthernetUni{id=netconf:192.168.56.10:830/0, cfgId=netconf:192.168.56.10:830/0, role=Root, refCount=0, ceVlanIds=[101], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[CarrierEthernetBandwidthProfile{id=FC-1, type=EVC, cir=0.0, cbs=0, eir=0.0, ebs=0}]}},`   `CarrierEthernetLogicalTerminationPoint{id=netconf:192.168.56.20:830/0, cfgId=netconf:192.168.56.20:830/0, role=Root, ni=CarrierEthernetUni{id=netconf:192.168.56.20:830/0, cfgId=netconf:192.168.56.20:830/0, role=Root, refCount=0, ceVlanIds=[101], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[CarrierEthernetBandwidthProfile{id=FC-1, type=EVC, cir=0.0, cbs=0, eir=0.0, ebs=0}]}}]}]}`<br/>

##EVPL flows
This creates a set of flows in ONOS that are pushed down to the two EA1000s through NETCONF to configure the EVCs

`onos> flows`<br/>
`deviceId=netconf:192.168.56.10:830, flowRuleCount=2`<br/>
`    id=71000050d21dd5, state=ADDED, bytes=0, packets=0, duration=0, liveType=UNKNOWN, priority=50000, tableId=1, appId=org.onosproject.ecord.carrierethernet, payLoad=null, selector=[IN_PORT:0, VLAN_VID:1], treatment=DefaultTrafficTreatment{immediate=[VLAN_POP], deferred=[], transition=None, meter=None, cleared=false, metadata=null}`  -- This represents ingress on the Optics port 0 on device A and POPs off the S-Tag<br/>
`    id=710000b5c1f057, state=ADDED, bytes=0, packets=0, duration=0, liveType=UNKNOWN, priority=50000, tableId=1, appId=org.onosproject.ecord.carrierethernet, payLoad=null, selector=[IN_PORT:1, VLAN_VID:101], treatment=DefaultTrafficTreatment{immediate=[VLAN_PUSH:qinq, VLAN_ID:1], deferred=[], transition=TABLE:0, meter=None, cleared=false, metadata=null}`  -- This represents ingress on the Host port 1 on device A and pushes on the S-Tag 1<br/>

`deviceId=netconf:192.168.56.20:830, flowRuleCount=2`<br/>
`    id=710000613c8252, state=ADDED, bytes=0, packets=0, duration=0, liveType=UNKNOWN, priority=50000, tableId=1, appId=org.onosproject.ecord.carrierethernet, payLoad=null, selector=[IN_PORT:0, VLAN_VID:1], treatment=DefaultTrafficTreatment{immediate=[VLAN_POP], deferred=[], transition=None, meter=None, cleared=false, metadata=null}`  -- This represents ingress on the Optics port 0 on device B and POPs off the S-Tag 1<br/>
`    id=7100006ca2573f, state=ADDED, bytes=0, packets=0, duration=0, liveType=UNKNOWN, priority=50000, tableId=1, appId=org.onosproject.ecord.carrierethernet, payLoad=null, selector=[IN_PORT:1, VLAN_VID:101], treatment=DefaultTrafficTreatment{immediate=[VLAN_PUSH:qinq, VLAN_ID:1], deferred=[], transition=TABLE:0, meter=None, cleared=false, metadata=null}`  -- This represents ingress on the Host port 1 on device B and pushes on the S-Tag 1<br/>

Through these flows it's clear that the CE-VLAN on the UNI-C side is 101 and that the S-Tag that is being pushed on is VLAN 1. Packets coming back in on the UNI-N on port 0 have their S-Tag popped off. In this scenario this will create evc-1 on both of the EA1000s.<br/>

On the actual EA1000 itself using a NETCONF CLI Client like yangcli-pro, the result is:

`admin@192.168.56.10> sget-config /mef-services/uni source=running`<br/>
`rpc-reply {`<br/>
`&nbsp;data {`<br/>
`&nbsp;&nbsp;mef-services {`<br/>
`&nbsp;&nbsp;&nbsp;uni {`  -- There is only one UNI on the EA1000<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;name Uni-on-192.168.56.10:830`  -- Automatically assigned<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;evc  1 {`  -- From the VLAN 1 from CE app<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;evc-index 1`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name EVC-1`  -- Automatically assigned<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;evc-per-uni {`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;evc-per-uni-c {`  -- The UNI-C side<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ce-vlan-map 101`  -- Could be a range of values<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;flow-mapping {`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ce-vlan-id 101`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;flow-id 31243725464268887`  -- For tracking with ONOS<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ingress-bwp-group-index 0`  -- No meters<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;tag-push {`  -- Push on an a VLAN<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;push-tag-type pushStag`  -- Push type is S-TAG<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;outer-tag-vlan 1`  -- Push value is 1<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;evc-per-uni-n {`  -- For the UNI-N side<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ce-vlan-map 1`  -- The VLAN to match for egress on this side<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;flow-mapping {`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ce-vlan-id 1`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;flow-id 31243723770830293`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ingress-bwp-group-index 0`<br/>
`&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;tag-pop {`  -- Pop off the S-TAG<br/>
`} } } } } } } }`<br/>
`admin@192.168.56.10>`<br/>

##CIR and EIR as OpenFlow Meters
** Note: The meters created by Carrier Ethernet are not compatible with Open vSwitch at present.They will disrupt the configuration of the EA1000 if there are Open VSwitch based OpenFlow switches between the UNIs **<br/>

To create limits on how the EVPL can transport data the CIR, EIR and CBS and EBS values can be specified:

`onos> ce-evc-create --cevlan 102 -c 400 -e 200 -cbs 3000 -ebs 2000 evpl2 POINT_TO_POINT netconf:192.168.56.10:830/0 netconf:192.168.56.20:830/0`<br/>

* -c 400 means Commit Information Rate is 400 MB/s
* -e 200 means Excess information Rate is 200 MB/s
* -cbs 3000 is Committed Burst Size of 3000 **Bytes**
* -ebs 2000 is Excess Burst Rate of 2000 **Bytes**

These will be created as meters in Open Flow.

`onos> meters`<br/>
` DefaultMeter{device=netconf:192.168.56.20:830, id=1, appId=org.onosproject.ecord.carrierethernet, unit=KB_PER_SEC, isBurst=true, state=PENDING_ADD, bands=[DefaultBand{rate=50000, burst-size=3000, type=REMARK, drop-precedence=0}, DefaultBand{rate=75000, burst-size=5000, type=DROP, drop-precedence=null}]}`<br/>
` DefaultMeter{device=netconf:192.168.56.10:830, id=1, appId=org.onosproject.ecord.carrierethernet, unit=KB_PER_SEC, isBurst=true, state=PENDING_ADD, bands=[DefaultBand{rate=75000, burst-size=5000, type=DROP, drop-precedence=null}, DefaultBand{rate=50000, burst-size=3000, type=REMARK, drop-precedence=0}]}`<br/>

Here the rates is shown as Bands. For each device the Bands are
*  REMARK for CIR and CBS - the REMARK applies to any packets that exceed the CIR in kB/s (400Mb/s = 50000kB/s) and the burst size 3000 Bytes
*  DROP for EIR and EBS - the DROP applies to any packets that exceed the sum of CIR and EIR in kB/s (400Mb/s + 200Mb/s = 75000kB/s) and a burst in excess of CBS and EBS (3000 + 2000 = 5000 Bytes)

##EVC Deletion
EVCs can be deleted individually with **ce-evc-remove <evc-id>** or all together with **ce-evc-remove-all**.