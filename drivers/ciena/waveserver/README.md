# Ciena Waveserver

This driver allows connection to the Ciena Waveserver

The User Guide for this product is available on request from Ciena, and gives a full explanation of the theory of operation, functionality and how all functions can be accessed through the REST interface.

Currently only a subset of it's functionality is supported through ONOS, but this will expand to full functionality in future releases.

## Compile and Installation

Currently this driver is built using BUCK and uses version 2.1 of onos-yang-tools<br/>

All that is required to activate the driver is to run the following at the ONOS CLI

```bash
app activate org.onosproject.drivers.ciena.waveserver
```

## Usage

### Creating Devices

Ciena Waveserver Devices are not Openflow devices. The connectivity is initiated from ONOS. They have to be created through the network/configuration REST interface in ONOS.

* The name must follow the format **rest:ipaddr:port**
* The **ip** and **port** must correspond to the ip and port in the name (above).

```bash
curl -X POST
  http://onos-ip:8181/onos/v1/network/configuration
  -H 'Authorization: Basic b25vczpyb2Nrcw==' \
  -H 'Content-Type: application/json' \
  -d '{
    "devices": {
      "rest:10.181.66.217:443": {
        "rest": {
          "port": 443,
          "ip": "10.181.66.217",
          "username": "su",
          "password": "cws",
          "protocol": "https",
          "url": "/yang-api/datastore/"
        },
        "basic": {
          "driver": "ciena-waveserver-rest"
        }
      }
    }
}'
```

#### Verify Connected Device

When the Waveserver is configured and connected is should be visible in ONOS through the `devices` command.

```bash 
onos> devices 
id=rest:10.181.66.217:443, available=true, local-status=connected 32s ago, role=MASTER, type=SWITCH, mfr=unknown, hw=unknown, sw=unknown, serial=unknown, driver=ciena-waveserver-rest, ipaddress=10.181.66.217, locType=geo, name=rest:10.181.66.217:443, protocol=REST
```
