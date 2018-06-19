all: mytunnel

mytunnel: mytunnel.p4
	p4c-bm2-ss --arch v1model -o mytunnel.json \
		--p4runtime-file mytunnel.p4info --p4runtime-format text \
		mytunnel.p4
