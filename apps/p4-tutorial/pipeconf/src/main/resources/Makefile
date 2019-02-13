all: mytunnel

mytunnel: mytunnel.p4
	p4c-bm2-ss --arch v1model -o mytunnel.json \
		--p4runtime-files mytunnel_p4info.txt \
		mytunnel.p4
