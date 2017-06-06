all: default.json empty.json ecmp.json wcmp.json

default.json: default.p4
	p4c-bm2-ss -o p4c-out/default.json \
	--p4runtime-file p4c-out/default.p4info --p4runtime-format text \
	default.p4

empty.json: empty.p4
	p4c-bm2-ss -o p4c-out/empty.json \
	--p4runtime-file p4c-out/empty.p4info --p4runtime-format text \
	empty.p4

ecmp.json: ecmp.p4
	p4c-bm2-ss -o p4c-out/ecmp.json \
	--p4runtime-file p4c-out/ecmp.p4info --p4runtime-format text \
	ecmp.p4

wcmp.json: wcmp.p4
	p4c-bm2-ss -o p4c-out/wcmp.json \
	--p4runtime-file p4c-out/wcmp.p4info --p4runtime-format text \
	wcmp.p4

clean:
	rm -rf p4c-out/*.json
	rm -rf p4c-out/*.p4info
