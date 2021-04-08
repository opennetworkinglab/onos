all: fabric fabric-spgw fabric-bng fabric-int fabric-spgw-int fabric-full constants

fabric:
	@./bmv2-compile.sh "fabric" ""

fabric-spgw:
	@./bmv2-compile.sh "fabric-spgw" "-DWITH_SPGW"

fabric-bng:
	@./bmv2-compile.sh "fabric-bng" "-DWITH_BNG -DWITHOUT_XCONNECT"

fabric-int:
	@./bmv2-compile.sh "fabric-int" "-DWITH_INT_SOURCE -DWITH_INT_TRANSIT"

fabric-spgw-int:
	@./bmv2-compile.sh "fabric-spgw-int" "-DWITH_SPGW -DWITH_INT_SOURCE -DWITH_INT_TRANSIT"

fabric-full:
	@./bmv2-compile.sh "fabric-full" " -DWITH_MULTICAST -DWITH_IPV6 \
		-DWITH_SIMPLE_NEXT -DWITH_HASHED_NEXT -DWITH_BNG -DWITH_SPGW \
		-DWITH_INT_SOURCE -DWITH_INT_TRANSIT -DWITH_INT_SINK"

constants:
ifndef ONOS_ROOT
	$(error ONOS_ROOT is undefined)
endif
	docker run --rm -v $(ONOS_ROOT):/onos -w /onos/tools/dev/bin \
		--entrypoint ./onos-gen-p4-constants opennetworking/p4mn:stable \
		-o /onos/pipelines/fabric/api/src/main/java/org/onosproject/pipelines/fabric/FabricConstants.java \
		fabric /onos/pipelines/fabric/impl/src/main/resources/p4c-out/fabric-full/bmv2/default/p4info.txt

clean:
	rm -rf p4c-out/*/bmv2
