all: basic int constants

basic: basic.p4
	p4c-bm2-ss --arch v1model -o p4c-out/bmv2/basic.json \
		--p4runtime-file p4c-out/bmv2/basic.p4info \
		--p4runtime-format text basic.p4

int: int.p4
	p4c-bm2-ss --arch v1model -o p4c-out/bmv2/int.json "-DTARGET_BMV2" \
		--p4runtime-file p4c-out/bmv2/int.p4info \
		--p4runtime-format text int.p4

constants:
	onos-gen-p4-constants \
		-o $(ONOS_ROOT)/pipelines/basic/src/main/java/org/onosproject/pipelines/basic/BasicConstants.java \
		basic p4c-out/bmv2/basic.p4info

clean:
	rm -rf p4c-out/bmv2/*
