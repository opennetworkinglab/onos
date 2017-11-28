OFAgent : OpenFlow agent for virtual subsystem
====================================

### What is OFAgent?
OFAgent is an OpenFlow agent which exposes virtual network to the external OpenFlow controllers.

### Top-Level Features

* *TODO*: add features.

## OFAgent Tracer how-to

Builtin OFAgent tracer enables filtering of OFAgent logs per specific OFAgent tenant. Tracer is not enabled by default. Steps required for its activation are:

1. Create Karaf alias ofagent_tracer by executing CLI command:
`onos>ofagent_tracer = { log:display | grep "OFAGENT_tenantId:" | grep "$1" }`

2. Use `ofagent_tracer` in form:
`onos>ofagent_tracer <tenant_id>`

Default log level is `INFO`. Optionally, OFAgent log level can be changed with CLI command:
``log:set <log_level> org.onosproject.ofagent``
where ``<log_level>`` can be `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`.

_Note 1.:_ Useful Karaf CLI commands for changing log configuration are (reference: [Apache Karaf Container 3.x - Documentation](http://karaf.apache.org/manual/latest-3.0.x/#_log)):
 
* `log:clear` - clear the log entries

* `log:display` -  displays the log entries of the rootLogger

* `log:tail` -  exactly the same as `log:display` but it continuously displays the log entries

* `log:display <logger>` -  displays the log entries of the `<logger>`

* `log:exception-display` - displays the last occurred exception

* `log:get` - shows the current log level of a ROOT logger

* `log:get ALL` - shows the current log level of all loggers

* `log:get <logger>` - show the current log level of a `<logger>`

* `log:set <log_level>` - sets `<log_level>` for ROOT logger

* `log:set <log_level> <logger>` - sets `<log_level>` for `<logger>` 

_Note 2.:_ Karaf log4j configuration is in file $ONOS_INSTALL_DIR/apache-karaf-3.0.8/etc/org.ops4j.pax.logging.cfg and it can be changed on the fly.  
 

More documentation is available in vBrigade [wiki](https://wiki.onosproject.org/display/ONOS/Virtualization+brigade) and [vBrigade weekly scrum notes](https://docs.google.com/document/d/1PNtZyjVcZ1jr4Yw12ngDsAx-nkry03lvAkRlc1OcHUg).
   