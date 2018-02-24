# Layer 2 Monitoring with CFM and SOAM in ONOS

Connectivity Fault Management (CFM) was first introduced in IEEE 802.1ag, which 
has been superseded by IEEE 802.1Q-2011 and subsequently by IEEE 802.1Q-2014 - 
the IEEE Standard for Local and metropolitan area networks - Bridges and Bridged 
Networks.

It defines protocols and practices for OAM (Operations, Administration, and 
Maintenance) for paths through 802.1 bridges and local area networks (LANs). 
It is an amendment to IEEE 802.1Q-2005 and was approved in 2007.

IEEE 802.1ag is largely identical with ITU-T Recommendation Y.1731, which 
additionally addresses performance monitoring.

In addition the Metro Ethernet Forum (MEF) has defined SOAM (Service Operations 
Administration and Management) in MEF-17 on top of these standards to build a 
framework for monitoring in conjunction with MEF services. This and other MEF 
standards are available in their technical documentation section.

## Modelling
Working these combined standards into a unified model is a complex task an 
inevitably requires some compromises. For instance while  802.1Q talks about 
Maintenance Domains and Maintenance Associations the MEF 17 standard uses the 
name Maintenance Entity (ME) and Maintenance Entity Group (MEG). MEF 38 and 39 
defines a model that encompasses all of the concepts as a set of YANG modules, 
that separate out the Monitoring aspects of MEF 17 from MEF Carrier Ethernet 
Services. In this way a loose coupling exists between the CFM and MEF Services 
linked together only through VLAN ids. 

MEF 38 defines a root object mef-cfm which represents 802.1Q and Y.1731 CFM 
entities, which is augmented by mef-soam-fm which adds on additional attributes 
and operations from SOAM related to fault management.

MEF 39 augments mef-cfm further with the performance management aspects of SOAM 
as mef-soam-pm including monitoring of delay, jitter and loss measurements.

These YANG models act as a unified definition of these standards that it makes 
practical sense to reuse in ONOS.

## Implementation in ONOS
Direct translation of the MEF 38 and MEF 39 YANG models into ONOS does not provide 
a completely clean way to define the models in ONOS, because of the complication 
brought by the augments. Instead the models have been defined independently in 
ONOS as a new structure under reusing common ONOS classes such as DeviceId, 
PortNumber, NetworkResource, Identifier, MacAddress etc. 

* onos/apps/cfm/api/src/main/java/org/onosproject/l2monitoring/cfm
* onos/apps/cfm/api/src/main/java/org/onosproject/l2monitoring/soam

## Java Model
These immutable objects are defined as Java POJOs with little dependencies on 
outside elements, and built the familiar builder pattern. 

As with 802.1Q the model’s top level starts with the Maintenance Domain - an 
abstract reference object defining the span of the domain of activity for 
maintenance in the network. Maintenance Domains are defined at 8 different levels 
to indicate the scope of their span.

Beneath the Maintenance Domain is the Maintenance Association, another abstract 
object specific to a VLAN that contains common parameters for a group of MEPs 
(Maintenance Association Endpoints). It is these MEPs that are created on devices 
in the network and can be thought of as monitoring points. Devices must support 
the CfmMepProgrammable behaviour to support the creation of MEPs.

The MEP is the key item then that allows specific actions to be taken on devices 
such as 

* Continuity Check Messaging (CCM)
* Loopback testing
* Linktrace testing

In addition these MEPs allow the creation of 
* Delay Measurement entities (which run tests for Delay and Jitter on a continuous basis)
* Loss Measurement entities (which run tests for Loss Measurement on a continuous basis)

The following shows the structure of Java classes defined in ONOS to represent 
these entities.

`Maintenance-Domain*`<br/>
&nbsp;`|-MdId` (MdIdCharStr or MdIdDomainName or MdIdMacUint or MdIdNone)<br/>
&nbsp;`|-Maintenance-Association*`<br/>
&nbsp;&nbsp;&nbsp;`|-MaIdShort` (MaIdCharStr or MaIdPrimaryVid or Ma2Octet 
                                     or MaIdRfc2685VpnId or MaIdIccY1731)<br/>
&nbsp;&nbsp;&nbsp;`|-Component*`<br/>
&nbsp;&nbsp;&nbsp;`|-Mep*` (Maintenance-Association-EndPoint) and MepEntry*<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;`|-MepId`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;`|-MepLbEntry`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;`|-MepLtEntry`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;`|-MepLtTransactionEntry*`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;`|-MepLtReply*`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;`|-SenderIdTlv`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;`|-DelayMeasurement*` (SOAM)<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;`|-DmId`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;`|-DelayMeasurement-Stat-Current`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;|`-DelayMeasurement-Stat-History*`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;|`-LossMeasurement*` (SOAM)<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;`|-LmId`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;`|-LossMeasurement-Stat-Current`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;`|-LossMeasurement-Stat-History*`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;`|-RemoteMepEntry*`<br/>
&nbsp;&nbsp;&nbsp;|&nbsp;`|-MepId`<br/>
&nbsp;&nbsp;&nbsp;`|-RemoteMepId`<br/>


The classes and interfaces can be broken down into 4 main categories:
* Those used as identifiers
* Those for configuring the object
* Those representing both the status attributes of the object and its 
configuration (usually ending in Entry)
* Those used in commands called on objects - for Loopback, Linktrace, Delay and 
Loss Measurement these are not configured - they are called as a method with 
parameters


# Java Services
A set of services are defined for managing the object model
* CFM MD Service
* CFM MEP Service
* SOAM Service

## CFM MD Service
This is an interface that manages only the configuration of Maintenance Domain 
and Maintenance Association. It acts as a standalone service in ONOS that manages 
only these 2 levels of the model. The Maintenance Domain contains a list of 
Maintenance Associations does not contain Meps - these are left to the CfmMepService.

These objects are persisted in ONOS in a distributed data store by this service.

There are CLI commands for creating, deleting and modifying these objects.
* cfm-md-list-all - Lists all CFM Maintenance Domains.
* cfm-md-list - Lists a single CFM Maintenance Domain.
* cfm-md-add - Add a CFM Maintenance Domain.
* cfm-md-delete - Delete a CFM Maintenance Domain and its children.
* cfm-ma-add - Add a CFM Maintenance Association to a Maintenance Domain.
* cfm-ma-delete - Delete a CFM Maintenance Association and its children.

## CFM MEP Service
This is an interface that manages the MEP and objects and commands below it. A 
MEP managed under this service has a reference to the Maintenance Domain and 
Maintenance Association above it.

MEPs are identified by a combination of MdId, MaID and MepId.

The service has the following actions associated with it
* Create MEP
* Delete MEP
* Get all MEPs (for a particular MD and MA)
* Get individual MEP
* Transmit Loopback
* Abort Loopback
* Transmit Linktrace
* Create Test Signal
* Abort Test Signal

At the moment the MEP object is only maintained in memory and not persisted in 
the ONOS data store. The objects Loopback, Linktrace, Test Signal, Loss 
Measurement and Delay Measurement are not maintained in ONOS at all as they are 
not really configuration objects. They are called on the fly and are passed down 
as a collection of method parameters to the device implementing the MEP. This 
device will operate on them and their results can be obtained by querying the 
responsible MEP on the device. For instance the results of LinkTrace, Loopback 
and Test Signal will appear in the MepLbEntry, MepLbEntry and MepTsEntry under 
the MepEntry when the “Get Individual Mep” method is called.

See in
* apps/cfm/src/test/resources/examples

for some JSON examples that can be used to activate this functionality.