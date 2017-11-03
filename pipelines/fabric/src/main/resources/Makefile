all: fabric.json

fabric.json: fabric.p4
	p4c-bm2-ss -o p4c-out/bmv2/fabric.json \
	--p4runtime-file p4c-out/bmv2/fabric.p4info --p4runtime-format text \
	fabric.p4

clean:
	rm -rf p4c-out/bmv2/*.json
	rm -rf p4c-out/bmv2/*.p4info
