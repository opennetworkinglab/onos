BMV2_CPU_PORT=255

BMV2_OPTIONS=-DTARGET_BMV2 -DCPU_PORT=$(BMV2_CPU_PORT)

all: bmv2 bmv2-spgw

bmv2: makedir
	p4c-bm2-ss --arch v1model -o p4c-out/bmv2/fabric.json \
		$(BMV2_OPTIONS) \
		--p4runtime-file p4c-out/bmv2/fabric.p4info \
		--p4runtime-format text fabric.p4

bmv2-spgw: makedir
	p4c-bm2-ss --arch v1model -o p4c-out/bmv2/fabric-spgw.json \
		$(BMV2_OPTIONS) -DWITH_SPGW \
		--p4runtime-file p4c-out/bmv2/fabric-spgw.p4info \
		--p4runtime-format text fabric.p4

makedir:
	mkdir -p p4c-out/bmv2

clean:
	rm -rf p4c-out/bmv2/*
