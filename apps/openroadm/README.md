ONOS Open ROADM Application
====================================
Welcome to the ONOS Open ROADM application. Please refer to the ONOS
wiki for the latest documentation:

[wiki](https://wiki.onosproject.org/display/ONOS/Open+ROADM+MSA)

This ONOS app consists of the Open ROADM YANG data models compiled to
Java, and loads the schema into the YANG runtime where it becomes
available to the dynamic config subsystem and the NETCONF/RESTCONF
serializers.

Service implementers have access to the complete data model in Java.
Alternatively, one can interact with the different data models using
RESTCONF / NETCONF northbound APIs.
A distributed store for dynamic configuration data is automatically
maintained by ONOS.
All of this functionality is available for the device, network and
service models defined by Open ROADM.

This application supports the latest available version of Open ROADM,
Version 1.2.1 (released Feb 6th, 2017).

### What is Open ROADM MSA?
The Open ROADM Multi-Source Agreement (MSA) defines interoperability
specifications for Reconfigurable Optical Add/Drop Multiplexers (ROADM).
Included are the ROADM switch as well as transponders and pluggable optics.
Specifications consist of both Optical interoperability as well as YANG data models.

### How to run
After compilation, load the following applications:

* org.onosproject.openroadm (this will load all the dependencies below)
* org.onosproject.yang
* org.onosproject.yms
* org.onosproject.config
* org.onosproject.restconf
* org.onosproject.protocols.restconfserver
* org.onosproject.netconf
* org.onosproject.netconfsb
* org.onosproject.yang-gui if you want to visualize the model in the ONOS UI

### Notes
In order to successfully compile the YANG models, standard models were
added to the tree, as well as a few changes were introduced.

The standard models that were included are:

* [ietf-yang-types](http://dld.netconfcentral.org/src/ietf-yang-types@2013-07-15.yang) (revision 2013-07-15)
* [ietf-inet-types](http://dld.netconfcentral.org/src/ietf-inet-types@2013-07-15.yang) (revision 2013-07-15)
* [ietf-netconf](http://dld.netconfcentral.org/src/ietf-netconf@2011-06-01.yang) (revision 2011-06-01)
* [ietf-netconf-acm](http://dld.netconfcentral.org/src/ietf-netconf-acm@2012-02-22.yang) (revision 2012-02-22)
* [iana-afn-safi](http://dld.netconfcentral.org/src/iana-afn-safi@2013-07-04.yang) (revision 2013-07-04)

We deleted the following YANG model because the YANG parser was not able to deal
with the augment block.
* org-openroadm-optical-multiplex-interfaces.yang

We commented out the following extension definitions,
as well as any reference to these extension.
* nc:get-filter-element-attributes
* nacm:default-deny-all
* nacm:default-deny-write

Finally, we used ONOS YANG tools version 1.12.0-b7.

### Useful links
To learn more about the ONOS project, check out our [website](http://www.onosproject.org)
and our [wiki](https://wiki.onosproject.org/).

To learn more about the Open ROADM MSA, check out the [website](http://www.openroadm.org)
and the [GitHub page](https://github.com/OpenROADM/OpenROADM_MSA_Public).