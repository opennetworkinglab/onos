all: fabric fabric-spgw fabric-int fabric-full constants

fabric:
	@./bmv2-compile.sh "fabric" ""

fabric-spgw:
	@./bmv2-compile.sh "fabric-spgw" "-DWITH_SPGW"

fabric-int:
	@./bmv2-compile.sh "fabric-int" "-DWITH_INT"

fabric-full:
	@./bmv2-compile.sh "fabric-full" "-DWITH_MULTICAST -DWITH_IPV6 -DWITH_SPGW -DWITH_INT"

constants:
	onos-gen-p4-constants \
		-o $(ONOS_ROOT)/pipelines/fabric/src/main/java/org/onosproject/pipelines/fabric/FabricConstants.java \
		fabric p4c-out/fabric-full/bmv2/default/p4info.txt

clean:
	rm -rf p4c-out/*/bmv2
