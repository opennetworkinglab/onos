# Microsemi Edge Assure 1000 SFP-NID
This driver allows connection to the Microsemi Edge Assure 1000 SFP-NID
[EdgeAssure 1000 Product Page](https://www.microsemi.com/existing-parts/parts/137346)

The User Guide for this product is available on request from Microsemi, and gives a full explanation of the theory of operation, functionality and how all functions can be accessed through the NETCONF interface only.<br/>

Currently only a subset of it's functionality is supported through ONOS, but this will expand to full functionality in future releases.

# Compile and Installation
Currently this driver is built using BUCK and uses version 2.1 of onos-yang-tools<br/>

All that is required to activate the driver is to run the following at the ONOS CLI<br/>
**`onos:app activate org.onosproject.drivers.microsemi`**<br/>
This will load any dependent apps also. To verify the driver has been loaded run the command<br/>
**onos:apps -a -s**

# Change NETCONF default connection timeout
Connection timeouts need to be increased from default values when using EA1000. At ONOS command line run

```
onos:cfg set org.onosproject.netconf.ctl.impl.NetconfControllerImpl netconfConnectTimeout 50
onos:cfg set org.onosproject.netconf.ctl.impl.NetconfControllerImpl netconfReplyTimeout 50
```

# Creating Devices
EA1000 Devices will not be automatically discovered at present in ONOS. They have to be created through the network/configuration REST interface in ONOS.

* The name must follow the format **netconf:ipaddr:port**
* The **ip** and **port** must correspond to the ip and port in the name (above).

```js
{
  "devices": {
    "netconf:192.168.56.10:830": {
      "netconf": {
        "username": "admin",
        "password": "admin",
        "ip": "192.168.56.10",
        "port": 830,
        "connect-timeout": 50,
        "reply-timeout": 50,
        "idle-timeout": 40 
      },
      "basic": {
        "driver": "microsemi-netconf",
        "type": "SWITCH",
        "manufacturer": "Microsemi",
        "hwVersion": "EA1000"
      }
    }
  }
}
```


# Connected Device
When the EA1000 is configured and connected is should be visible in ONOS through the **devices** command.

```
onos> devices
id=netconf:192.168.56.10:830, available=true, local-status=connected 33s ago, role=MASTER, type=SWITCH, mfr=Microsemi, hw=EA1000, sw=4.4.0-53-generic, serial=Eagle Simulator., driver=microsemi-netconf, ipaddress=192.168.56.10, latitude=51.8865467, locType=geo, longitude=-8.4040440, name=netconf:192.168.56.10:830, port=830, protocol=NETCONF 
```

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

```
onos> ports
id=netconf:192.168.56.10:830, available=true, local-status=connected 15s ago, role=MASTER, type=SWITCH, mfr=Microsemi, hw=EA1000, sw=4.4.0-53-generic, serial=Eagle Simulator., driver=microsemi-netconf, ipaddress=192.168.56.10, latitude=51.8865467, locType=geo, longitude=-8.4040440, name=netconf:192.168.56.10:830, port=830, protocol=NETCONF
    port=0, state=enabled, type=fiber, speed=1000, portName=Optics
    port=1, state=enabled, type=copper, speed=1000, portName=Host
```

# OpenFlow Emulation
Currently the EA1000 supports only a limited set of OpenFlow rules through the Flows REST API and the Flow Objective API.

## IP Source Address filtering
A feature of the EA1000 that may be configured through Flow Rules is IP Source Address Filtering. This can only be activated on Port 0 (the optics Port). An example of this kind of flow is

`POST /onos/v1/flows/ HTTP/1.1`<br/>
```js
{
  "flows": [
    {
      "priority": 50000,
      "timeout": 0,
      "isPermanent": true,
      "deviceId": "netconf:192.168.56.10:830",
      "tableId": 8,
      "treatment": {
        "instructions": [ {"type": "NOACTION"} ],
        "deferred": []
      },
      "selector": {
        "criteria": [
          {"type": "IPV4_SRC", "ip": "192.168.8.0/24"},
          {"type": "IN_PORT", "port": "0"}
        ]
      }
    }
  ]
}
```

## Vlan Tag Manipulation
**Note: Before Vlan Tag manipulation can be done the mode of the interface on the EA1000 has to be changed to be compatible with the type of tags that are being received. If this is not done, once an EVC is created the packets that have the Vlan tagged on will be treated as untagged by EA1000, and dropped if there is no EVC present corresponding to the interface PVID. See the section below on the "Setting EA1000 Interface Tagging Mode"**

Flows that Push, Pop or Overwrite VLAN tags are implemented in EA1000 and are treated as MEF Carrier Ethernet EVCs. Both CTags and STags can be pushed on to matching Ethernet packets at network Layer 2.

`POST /onos/v1/flows/ HTTP/1.1`<br/>
```js
{
  "flows": [
    {
      "priority": 50000,
      "timeout": 0,
      "isPermanent": true,
      "tableId": 6, // This sets the EVC id - this first flow is configuring the customer side - UNI-C
      "deviceId": "netconf:192.168.56.10:830",
      "treatment": {
        "instructions": [
          {
            "type": "L2MODIFICATION",
            "subtype": "VLAN_PUSH", // This pushes a VLAN on
            "ethernetType": "0x88a8" // The pushed VLAN type is QinQ
          },
          {
            "type": "L2MODIFICATION",
            "subtype": "VLAN_ID",
            "vlanId": "200" // The pushed VLAN id is 200
          }],
        "deferred": []
      },
      "selector": {
        "criteria": [
        {
          "type": "VLAN_VID",
          "vlanId": "100" // Applies only to packets already tagged with VLAN 100..
        },
        {
          "type": "IN_PORT",
          "port": "1" // ...that are coming in on the Host port
        }]
        }
    },
    {
      "priority": 50000,
      "timeout": 0,
      "isPermanent": true,
      "tableId": 6, // The same EVC, but now we are configuring another VLAN
      "deviceId": "netconf:192.168.56.10:830",
      "treatment": {
        "instructions": [
          {
            "type": "L2MODIFICATION",
            "subtype": "VLAN_PUSH", // Push again
            "ethernetType": "0x88a8" // QinQ again
          },
          {
            "type": "L2MODIFICATION",
            "subtype": "VLAN_ID",
            "vlanId": "200" // VLAN 200 again
          }],
        "deferred": []
      },
      "selector": {
        "criteria": [
          {
            "type": "VLAN_VID",
            "vlanId": "101" // Applies only to packets already tagged with VLAN 101..
          },
          {
            "type": "IN_PORT",
            "port": "1" // ... that are coming in on the Host port
          }]
      }
    },
    {
      "priority": 50000,
      "timeout": 0,
      "isPermanent": true,
      "tableId": 6, // The same EVC, but now we are configuring the opposite side
      "deviceId": "netconf:192.168.56.10:830",
      "treatment": {
        "instructions": [
        {
          "type": "L2MODIFICATION",
          "subtype": "VLAN_POP" // Here we are popping the top level tag
        }],
        "deferred": []
      },
      "selector": {
        "criteria": [
          {
            "type": "VLAN_VID", // Applies only to packets tagged with VLAN 200
            "vlanId": "200"
          },
          {
            "type": "IN_PORT",
            "port": "0" // That are coming in on the Optics Port
          }]
      }
    }
  ]
}
```

## Setting EA1000 Interface Tagging Mode
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

On an EA1000 accessed through *yangcli-pro* the following commands can be used to make these changes:

```
admin@192.168.2.234> discard-changes
admin@192.168.2.234> copy-config source=startup target=candidate
admin@192.168.2.234> merge /interfaces/interface[name='eth0']/frame-format --value='stag'
admin@192.168.2.234> merge /interfaces/interface[name='eth1']/frame-format --value='ctag'
admin@192.168.2.234> commit
admin@192.168.2.234> copy-config source=running target=startup
```

# ONOS Carrier Ethernet Application
The ONOS [Carrier Ethernet](https://wiki.onosproject.org/display/ONOS/Carrier+Ethernet+Application) application allows EVCs to be created between UNIs that are linked together in ONOS. This is translated down to OpenFlow switches by converting the **ce-evc-create** commands in to Flow Rules similar to those shown above.

While EA1000 is not an OpenFlow switch, it's driver can convert these flows in to NETCONF which can be used to configure EVCs on the EA1000. Because EA1000 is just a 2 port device it represents only 1 UNI (conventionally on switches each port represnts a UNI).

When an EA1000 device is configured in ONOS the Carrier Ethernet Application considers both of its ports to be UNIs, as can be seen in the listing below:

```
onos> ce-uni-list
CarrierEthernetUni{id=netconf:192.168.56.10:830/0, cfgId=netconf:192.168.56.10:830/0, role=null, refCount=0, ceVlanIds=[], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[]}
CarrierEthernetUni{id=netconf:192.168.56.10:830/1, cfgId=netconf:192.168.56.10:830/1, role=null, refCount=0, ceVlanIds=[], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[]}
```

This mismatch is handled in the driver - both side appear to be separate here but they will apply to the same single UNI on the EA1000 in opposite directions.

For an EVC to be created there has to an ONOS 'link' between 2 UNIs for a POINT-TO-POINT connection - an Ethernet Virtual Private Line (**EVPL**). There has to be more than 2 UNIs to create a MULTIPOINT-TO-MULTIPOINT link - an E-Lan.

## Links
In a simple scenario that has 2 EA1000s (netconf:192.168.56.10:830 and netconf:192.168.56.20:830) configured in ONOS, the two might be linked together through their optics ports (port 0). The following result is expected for a bi-directional link:

```
onos> links
src=netconf:192.168.56.10:830/0, dst=netconf:192.168.56.20:830/0, type=DIRECT, state=ACTIVE, expected=false
src=netconf:192.168.56.20:830/0, dst=netconf:192.168.56.10:830/0, type=DIRECT, state=ACTIVE, expected=false
```
This will not exist by default since Link Discovery is not yet a feature of the EA1000 driver. These have to be created manually - through the network/configuration REST API.

`POST /onos/v1/network/configuration/ HTTP/1.1`<br/>
```js
{
  "links": {
    "netconf:192.168.56.10:830/0-netconf:192.168.56.20:830/0": { // 10 to 20
      "basic" : {
        "type" : "DIRECT"
      }
    },
    "netconf:192.168.56.20:830/0-netconf:192.168.56.10:830/0" : { // and reverse
      "basic" : {
        "type" : "DIRECT"
      }
    }
  }
}
```

## EVPL Creation
To create a simple EVPL the following command can be used at the ONOS CLI:<br/>

```
onos>ce-evc-create --cevlan 101 evpl1 POINT_TO_POINT netconf:192.168.57.10:830/0 netconf:192.168.57.20:830/0
```

This returns without any message. Tailing through the ONOS logs will reveal any error that might have occurred.<br/>

This EVC can be viewed only through the command line:
```
onos> ce-evc-list
CarrierEthernetVirtualConnection{id=EP-Line-1, cfgId=evpl1, type=POINT_TO_POINT, state=ACTIVE,
UNIs=[
CarrierEthernetUni{id=netconf:192.168.56.10:830/0, cfgId=netconf:192.168.56.10:830/0, role=Root, refCount=0, ceVlanIds=[101], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[CarrierEthernetBandwidthProfile{id=FC-1, type=EVC, cir=0.0, cbs=0, eir=0.0, ebs=0}]},
CarrierEthernetUni{id=netconf:192.168.56.20:830/0, cfgId=netconf:192.168.56.20:830/0, role=Root, refCount=0, ceVlanIds=[101], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[CarrierEthernetBandwidthProfile{id=FC-1, type=EVC, cir=0.0, cbs=0, eir=0.0, ebs=0}]}],
FCs=[CarrierEthernetForwardingConstruct{id=FC-1, cfgId=null, type=POINT_TO_POINT, vlanId=1, metroConnectId=null, refCount=1,
LTPs=[
CarrierEthernetLogicalTerminationPoint{id=netconf:192.168.56.10:830/0, cfgId=netconf:192.168.56.10:830/0, role=Root, ni=CarrierEthernetUni{id=netconf:192.168.56.10:830/0, cfgId=netconf:192.168.56.10:830/0, role=Root, refCount=0, ceVlanIds=[101], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[CarrierEthernetBandwidthProfile{id=FC-1, type=EVC, cir=0.0, cbs=0, eir=0.0, ebs=0}]}}, CarrierEthernetLogicalTerminationPoint{id=netconf:192.168.56.20:830/0, cfgId=netconf:192.168.56.20:830/0, role=Root, ni=CarrierEthernetUni{id=netconf:192.168.56.20:830/0, cfgId=netconf:192.168.56.20:830/0, role=Root, refCount=0, ceVlanIds=[101], capacity=1000000000, usedCapacity=0.0, bandwidthProfiles=[CarrierEthernetBandwidthProfile{id=FC-1, type=EVC, cir=0.0, cbs=0, eir=0.0, ebs=0}]}}]}]}
```

## EVPL flows
This creates a set of flows in ONOS that are pushed down to the two EA1000s through NETCONF to configure the EVCs

```
onos> flows
deviceId=netconf:192.168.56.10:830, flowRuleCount=2
id=71000050d21dd5, state=ADDED, bytes=0, packets=0, duration=0, liveType=UNKNOWN, priority=50000, tableId=1, appId=org.onosproject.ecord.carrierethernet, payLoad=null, selector=[IN_PORT:0, VLAN_VID:1], treatment=DefaultTrafficTreatment{immediate=[VLAN_POP], deferred=[], transition=None, meter=None, cleared=false, metadata=null} # This represents ingress on the Optics port 0 on device A and POPs off the S-Tag
id=710000b5c1f057, state=ADDED, bytes=0, packets=0, duration=0, liveType=UNKNOWN, priority=50000, tableId=1, appId=org.onosproject.ecord.carrierethernet, payLoad=null, selector=[IN_PORT:1, VLAN_VID:101], treatment=DefaultTrafficTreatment{immediate=[VLAN_PUSH:qinq, VLAN_ID:1], deferred=[], transition=TABLE:0, meter=None, cleared=false, metadata=null} # This represents ingress on the Host port 1 on device A and pushes on the S-Tag 1

deviceId=netconf:192.168.56.20:830, flowRuleCount=2
id=710000613c8252, state=ADDED, bytes=0, packets=0, duration=0, liveType=UNKNOWN, priority=50000, tableId=1, appId=org.onosproject.ecord.carrierethernet, payLoad=null, selector=[IN_PORT:0, VLAN_VID:1], treatment=DefaultTrafficTreatment{immediate=[VLAN_POP], deferred=[], transition=None, meter=None, cleared=false, metadata=null} # This represents ingress on the Optics port 0 on device B and POPs off the S-Tag 1
id=7100006ca2573f, state=ADDED, bytes=0, packets=0, duration=0, liveType=UNKNOWN, priority=50000, tableId=1, appId=org.onosproject.ecord.carrierethernet, payLoad=null, selector=[IN_PORT:1, VLAN_VID:101], treatment=DefaultTrafficTreatment{immediate=[VLAN_PUSH:qinq, VLAN_ID:1], deferred=[], transition=TABLE:0, meter=None, cleared=false, metadata=null} # This represents ingress on the Host port 1 on device B and pushes on the S-Tag 1
```

Through these flows it's clear that the CE-VLAN on the UNI-C side is 101 and that the S-Tag that is being pushed on is VLAN 1. Packets coming back in on the UNI-N on port 0 have their S-Tag popped off. In this scenario this will create evc-1 on both of the EA1000s.<br/>

On the actual EA1000 itself using a NETCONF CLI Client like yangcli-pro, the result is:
```
admin@192.168.56.10> sget-config /mef-services/uni source=running
rpc-reply {
  data {
    mef-services {
      uni { # There is only one UNI on the EA1000
        name Uni-on-192.168.56.10:830 # Automatically assigned
        evc 1 { # From the VLAN 1 from CE app
          evc-index 1
          name EVC-1 # Automatically assigned
          evc-per-uni {
            evc-per-uni-c { # The UNI-C side
            ce-vlan-map 101 # Could be a range of values
            flow-mapping {
              ce-vlan-id 101
              flow-id 31243725464268887 # For tracking with ONOS
            }
            ingress-bwp-group-index 0 # No meters
            tag-push { # Push on an a VLAN
              push-tag-type pushStag # Push type is S-TAG
              outer-tag-vlan 1 # Push value is 1
              }
            }
            evc-per-uni-n { # For the UNI-N side
              ce-vlan-map 1 # The VLAN to match for egress on this side
              flow-mapping {
                ce-vlan-id 1
                flow-id 31243723770830293
              }
              ingress-bwp-group-index 0
              tag-pop { # Pop off the S-TAG
              }
            }
          }
        }
      }
    }
  }
}
admin@192.168.56.10>
```

## CIR and EIR as OpenFlow Meters
** Note: The meters created by Carrier Ethernet are not compatible with Open vSwitch at present.They will disrupt the configuration of the EA1000 if there are Open VSwitch based OpenFlow switches between the UNIs **<br/>

To create limits on how the EVPL can transport data the CIR, EIR and CBS and EBS values can be specified:

```
onos> ce-evc-create --cevlan 102 -c 400 -e 200 -cbs 3000 -ebs 2000 evpl2 POINT_TO_POINT netconf:192.168.56.10:830/0 netconf:192.168.56.20:830/0
```

* -c 400 means Commit Information Rate is 400 MB/s
* -e 200 means Excess information Rate is 200 MB/s
* -cbs 3000 is Committed Burst Size of 3000 **Bytes**
* -ebs 2000 is Excess Burst Rate of 2000 **Bytes**

These will be created as meters in Open Flow.

```
onos> meters
DefaultMeter{device=netconf:192.168.56.20:830, id=1, appId=org.onosproject.ecord.carrierethernet, unit=KB_PER_SEC, isBurst=true, state=PENDING_ADD, bands=[DefaultBand{rate=50000, burst-size=3000, type=REMARK, drop-precedence=0}, DefaultBand{rate=75000, burst-size=5000, type=DROP, drop-precedence=null}]}
DefaultMeter{device=netconf:192.168.56.10:830, id=1, appId=org.onosproject.ecord.carrierethernet, unit=KB_PER_SEC, isBurst=true, state=PENDING_ADD, bands=[DefaultBand{rate=75000, burst-size=5000, type=DROP, drop-precedence=null}, DefaultBand{rate=50000, burst-size=3000, type=REMARK, drop-precedence=0}]}
```

## EVC Deletion
EVCs can be deleted individually with **ce-evc-remove <evc-id>** or all together with **ce-evc-remove-all**.

# Support for Layer 2 Monitoring
EA1000 supports both Connectivity Fault Management (CFM) and MEF Services OAM. This is achieved through the EA1000 driver supporting the ONOS behaviors CfmMepProgrammable and SoamDmProgrammable described in [Layer 2 Monitoring with CFM and Services OAM](https://wiki.onosproject.org/display/ONOS/Layer+2+Monitoring+with+CFM+and+Services+OAM).

With EA1000 the CFM entities (Maintenance Association Endpoints or MEPs) are created in parallel with the EVC services that they are designed to test, and related loosely to each other only through VLAN ID.
For instance an EVC might be created with a VLAN of 101, and separately a Maintenance Association would be created with the same VLAN ID, and MEPs created under this for monitoring that VLAN (and by inference that EVC).

The CFM interface to ONOS is exposed through a REST API at /onos/cfm
In ONOS Maintenance Domains and Maintenance Associations beneath them are created and persisted in a distributed datastore. These are logical entities that can span across an ONOS cluster and are not directly related to devices.

The Maintenance Association Endpoint - MEP (the child of the Maintenance Association, and grandchild of the Maintenance Domain) is also a logical entity but has a hard many:1 association to a device that supports the CfmMepProgrammable behaviour. EA1000 is one such device, and so one to many MEPs can be associated with an EA1000 device.

For example to create an Maintenance Domain in ONOS the following might be POSTed to 
`POST http://localhost:8181/onos/cfm/md HTTP/1.1`</br>
```js
{"md": {
    "mdName": "Microsemi",
    "mdNameType": "CHARACTERSTRING",
    "mdLevel": "LEVEL3",
    "mdNumericId": 1
   }
}
```

To create a Maintenance Association under this
`POST http://localhost:8181/onos/cfm/md/Microsemi/ma HTTP/1.1`</br>
```js
{
  "ma": {
    "maName": "ma-vlan-1",
    "maNameType": "CHARACTERSTRING",
    "maNumericId": 1,
    "ccm-interval": "INTERVAL_1S",
    "component-list": [
      { "component": {
        "component-id":"1",
        "tag-type": "VLAN_STAG",
        "vid-list": [
          {"vid":1}
        ]
        }
      }
    ],
    "rmep-list": [
      { "rmep":10 },
      { "rmep":20 },
      { "rmep":30 }
    ]
  }
}
```

To create a MEP under this:
`POST http://localhost:8181/onos/cfm/md/Microsemi/ma/ma-vlan-1/mep HTTP/1.1`</br>
```js
{
  "mep": {
    "mepId": 10,
    "deviceId": "netconf:10.205.86.26:830",
    "port": 0,
    "direction": "DOWN_MEP",
    "primary-vid": 1,
    "administrative-state": true,
    "ccm-ltm-priority": 4,
    "cci-enabled" :true
  }
}
```

When the MEP is created a configuration is written down to the EA1000 device at 10.205.86.26 through NETCONF roughly in the format:
```xml
<maintenance-domain>
 <id>1</id>
 <name>Microsemi</name>
 <name-type>CHARACTER_STRING</name-type>
 <md-level>3</md-level>
 <maintenance-association>
   <id>1</id>
   <name>ma-vlan-1</name>
   <name-type>CHARACTER_STRING</name-type>
   <component-list>
     <tag-type>vlan-stag</tag-type>
     <vid>1</vid> 
   </component-list>
   <remote-mep>10</remote-mep>
   <remote-mep>20</remote-mep>
   <remote-mep>30</remote-mep>
   <maintenance-association-endpoint>
     <mep-identifier>10</mep-identifier>
     ...
   </maintenance-association-endpoint>
 </maintenance-association>
</maintenance-domain>
```

There are a few things to note here:
* On EA1000 the MD and MA are indexed by their _id_ and not by name. This means that it is essential when working with EA1000 that all MD's and MA's have a numeric ID specified, and that the numeric IDs of Maintenance Domains should be unique. The numeric IDs of Maintenance Associations should be unique _within_ Maintenance Domains.
* The component list is flattened down to a singleton object. While in the CFM model many Components are possible, EA1000 supports only 1
* With Remote Meps - the local and all remote meps must be specified by their ID. In this instance 10 is the local on device 10.205.86.26 and 20 and 30 are remote meps that we expect will be local to some other devices
* Even though the write to the EA1000 only happens when the MEP is created it brings down the MD and MA to the device with it.
* When the MEP is deleted the MD and MA are left behind on the device. If the MD and MA were then to be changed in ONOS and a new MEP pushed down to the device, there would be an error, as the MD and MA would remain on the device since the earlier time. To remedy this, the MD and MA would need to be deleted manually through yangcli-pro.
