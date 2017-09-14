all: main

main: main.p4
	p4c-bm2-ss -o main.json --p4runtime-file main.p4info --p4runtime-format text main.p4
	# Fix for BMv2/p4c bug...
	sed -i -e 's/"value" : "0xff"/"value" : "0x00ff"/g' main.json