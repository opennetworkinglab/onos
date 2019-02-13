all: basic int constants

basic: basic.p4
	p4c-bm2-ss --arch v1model -o p4c-out/bmv2/basic.json \
		--p4runtime-files p4c-out/bmv2/basic_p4info.txt \
		basic.p4

int: int.p4
	p4c-bm2-ss --arch v1model -o p4c-out/bmv2/int.json "-DTARGET_BMV2" \
		--p4runtime-files p4c-out/bmv2/int_p4info.txt \
		int.p4

constants:
	onos-gen-p4-constants \
		-o $(ONOS_ROOT)/pipelines/basic/src/main/java/org/onosproject/pipelines/basic/BasicConstants.java \
		basic p4c-out/bmv2/basic_p4info.txt

clean:
	rm -rf p4c-out/bmv2/*
