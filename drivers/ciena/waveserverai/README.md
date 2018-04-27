# Ciena Waveserver Ai

This driver allows connection to the Ciena Waveserver Ai

The User Guide for this product is available on request from Ciena, and gives a full explanation of the theory of operation, functionality and how all functions can be accessed through the NETCONF interface.

Currently only a subset of it's functionality is supported through ONOS, but this will expand to full functionality in future releases.

## Compile and Installation

All that is required to activate the driver is to run the following at the ONOS CLI

```bash
app activate org.onosproject.drivers.ciena.waveserverai
```

## Usage

### Link Discovery Requirements

The Waveserver Ai does not support LLDP on the I-NNI ports.  In order to allow discovery, the ports must be configured with labels:

CLI configuration example on device A
```bash
port set port 1/1 label ${remote_mac:00238afa4552}${remote_port:1-1}
```

A corresponding port label must be set on the far end device.

### Creating Devices

Ciena Waveserver Ai Devices are not Openflow devices. The connectivity is initiated from ONOS. They have to be created through the network/configuration REST interface in ONOS.

* The name must follow the format **netconf:ipaddr:port**
* The **ip** and **port** must correspond to the ip and port in the name (above).

```bash
curl -X POST \
  http://localhost:8181/onos/v1/network/configuration \
  -H 'Authorization: Basic b25vczpyb2Nrcw==' \
  -H 'Content-Type: application/json' \
  -d '{
    "devices": {
      "netconf:10.132.241.91:830": {
        "netconf": {
          "port": 830,
          "ip": "10.132.241.91",
          "username": "su",
          "password": "ciena"
        },
        "basic": {
          "driver": "ciena-waveserverai-netconf"
        }
      }
    }
}'
```

#### Verify Connected Device

When the Waveserver Ai is configured and connected is should be visible in ONOS through the `devices` command.

```bash 
onos> devices 
id=netconf:10.132.241.91:830, available=true, local-status=connected 4m22s ago, role=MASTER, type=OTHER, mfr=Ciena, hw=WaverserverAi, sw=bar, serial=foo, driver=ciena-waveserverai-netconf, ipaddress=10.132.241.91, locType=geo, name=netconf:10.132.241.91:830, port=830, protocol=NETCONF
```
