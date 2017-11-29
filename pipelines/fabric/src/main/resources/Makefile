BMV2_CPU_PORT=255

BMV2_OPTIONS=-DTARGET_BMV2

all: bmv2

bmv2:
	p4c -v -x p4-16 -b bmv2-ss-p4org \
	$(BMV2_OPTIONS) -o p4c-out/bmv2 \
	--p4runtime-file p4c-out/bmv2/fabric.p4info \
	--p4runtime-format text fabric.p4
	mv p4c-out/bmv2/fabric.p4rt p4c-out/bmv2/fabric.p4info
	rm -f p4c-out/bmv2/fabric.p4i

custom:
	p4c -v -x p4-16 -b $(BACKEND) \
		$(BACKEND_OPTIONS) -o p4c-out/$(BACKEND) \
		--p4runtime-file p4c-out/$(BACKEND)/fabric.p4info \
		--p4runtime-format text fabric.p4

clean:
	rm -rf p4c-out/*
