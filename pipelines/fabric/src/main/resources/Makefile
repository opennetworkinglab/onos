all: fabric fabric-spgw

fabric:
	@./bmv2-compile.sh "fabric" ""

fabric-spgw:
	@./bmv2-compile.sh "fabric-spgw" "-DWITH_SPGW"

clean:
	rm -rf p4c-out/*/bmv2
