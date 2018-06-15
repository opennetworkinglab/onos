all: basic int

basic: basic.p4
	p4c-bm2-ss --arch v1model -o p4c-out/bmv2/basic.json \
		--p4runtime-file p4c-out/bmv2/basic.p4info \
		--p4runtime-format text basic.p4

int: int.p4
	p4c-bm2-ss --arch v1model -o p4c-out/bmv2/int.json \
		--p4runtime-file p4c-out/bmv2/int.p4info \
		--p4runtime-format text int.p4

clean:
	rm -rf p4c-out/bmv2/*
