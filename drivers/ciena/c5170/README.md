# Ciena 5170

This driver allows connection to the Ciena 5170

The User Guide for this product is available on request from Ciena, and gives a full explanation of the theory of operation, functionality and how all functions can be accessed through the REST interface.

Currently only a subset of it's functionality is supported through ONOS, but this will expand to full functionality in future releases.

More information about ONOS's NETCONF support can be found at `https://wiki.onosproject.org/display/ONOS/NETCONF`.

## Compile and Installation

Currently this driver is built using BUCK<br/>

All that is required to activate the driver is to run the following at the ONOS CLI

```bash
app activate org.onosproject.drivers.ciena.c5170
```

## Usage

### Creating Devices

Ciena 5170 Devices are not Openflow devices. The connectivity is initiated from ONOS. They have to be created through the network/configuration interface in ONOS.

* The name must follow the format **netconf:ipaddr:port**
* The **ip** and **port** must correspond to the ip and port in the name (above).
* The **connect-timeout** and **reply-timeout** are optional and control NETCONF communication timeouts
* The **name** is optional and allows you to set a human friendly name for the device

```bash
curl -X POST
  http://onos-ip:8181/onos/v1/network/configuration
  -H 'Authorization: Basic b25vczpyb2Nrcw==' \
  -H 'Content-Type: application/json' \
  -d '{
    "devices": {
      "netconf:10.184.136.181:830": {
        "netconf": {
          "port": 830,
          "ip": "10.184.136.181",
          "username": "su",
          "password": "cws",
          "connect-timeout": 120,
          "reply-timeout": 640
        },
        "basic": {
          "driver": "ciena-c5170-netconf",
          "name": "my-switch"
        }
      }
    }
}'
```



#### Verify Connected Device

When the 5170 is configured and connected is should be visible in ONOS through the `devices` command.

```bash
onos> devices
id=netconf:10.184.136.181:830, available=true, local-status=connected 1s ago, role=MASTER, type=SWITCH, mfr=Ciena, hw=CN5170, sw=saos-01-01-00-0025, serial=1C1161D18800, driver=ciena-5170-netconf, gridX=null, gridY=null, ipaddress=10.184.136.181, latitude=null, locType=none, longitude=null, name=s6, port=830, protocol=NETCONF
```
