BMV2_CPU_PORT=255

BMV2_OPTIONS=-DTARGET_BMV2 -DCPU_PORT=$(BMV2_CPU_PORT)

all: bmv2 bmv2-spgw

bmv2:
	p4c -v -x p4-16 -b bmv2-v1model \
	$(BMV2_OPTIONS) -o p4c-out/bmv2 \
	--p4runtime-file p4c-out/bmv2/fabric.p4info \
	--p4runtime-format text fabric.p4
	mv p4c-out/bmv2/fabric.p4rt p4c-out/bmv2/fabric.p4info
	rm -f p4c-out/bmv2/fabric.p4i

bmv2-spgw:
	p4c-bm2-ss -o p4c-out/bmv2/fabric-spgw.json \
	        $(BMV2_OPTIONS) -DWITH_SPGW \
    		--p4runtime-file p4c-out/bmv2/fabric-spgw.p4info \
    		--p4runtime-format text fabric.p4

clean:
	rm -rf p4c-out/*
