# Running ONOS with Mellanox Spectrum/Spectrum2 Switches

## Spectrum and Fabric.p4
The Spectrum architecture supports the fabric.p4 pipeline, but using the spectrum_model.p4 instead of the v1model.p4.
The folder location p4c-out/fabric-mlnx/spectrum/default is where the P4 compiler artifacts should be placed for 
ONOS to properly load and configure the pipeline for the Spectrum switch. 
These files include:

* cpu_port.txt:   defines the SDN port number to be used when sending a packet to the controller
* p4info.txt:     the P4Runtime output file, in protobuf text format, when compiling fabric.p4
* spectrum.bin:   The "binary blob" P4 compiler output, which contains all the data necessary to reconfigure the 
  switch pipeline (P4 device config, as described in the P4Runtime specification)

Since at this time the Mellanox P4 compiler backend is under active development and is not currently open sourced,
please contact your Mellanox representative for access to the compiler and/or the compiler artifacts described above.

For the latest details, please take a look at the wiki page instructions:
https://wiki.onosproject.org/display/ONOS/Controlling+P4Runtime-enabled+Mellanox+Spectrum+switch+with+ONOS
