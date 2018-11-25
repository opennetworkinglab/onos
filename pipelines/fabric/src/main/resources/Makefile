all: fabric fabric-spgw fabric-int fabric-spgw-int fabric-full constants

fabric:
	@./bmv2-compile.sh "fabric" ""

fabric-spgw:
	@./bmv2-compile.sh "fabric-spgw" "-DWITH_SPGW"

fabric-int:
	@./bmv2-compile.sh "fabric-int" "-DWITH_INT_SOURCE -DWITH_INT_TRANSIT"

fabric-spgw-int:
	@./bmv2-compile.sh "fabric-spgw-int" "-DWITH_SPGW -DWITH_INT_SOURCE -DWITH_INT_TRANSIT"

fabric-full:
	@./bmv2-compile.sh "fabric-full" " -DWITH_MULTICAST -DWITH_IPV6 \
		-DWITH_SIMPLE_NEXT -DWITH_HASHED_NEXT -DWITH_SPGW \
		-DWITH_INT_SOURCE -DWITH_INT_TRANSIT -DWITH_INT_SINK"

constants:
	onos-gen-p4-constants \
		-o $(ONOS_ROOT)/pipelines/fabric/src/main/java/org/onosproject/pipelines/fabric/FabricConstants.java \
		fabric p4c-out/fabric-full/bmv2/default/p4info.txt

clean:
	rm -rf p4c-out/*/bmv2
