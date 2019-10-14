load("//tools/build/bazel:osgi_java_library.bzl", "osgi_proto_jar")

PROTO_SOURCE_ROOT = "/core/protobuf/models/proto"

osgi_proto_jar(
    name = "onos-core-protobuf-models-proto",
    proto_libs = [
        ":ApplicationsEnums_proto",
        ":ConfigPropertyEnums_proto",
        ":ConfigProperty_proto",
        ":NodeId_proto",
        ":RoleInfo_proto",
        ":ApplicationId_proto",
        ":ApplicationProto_proto",
        ":Version_proto",
        ":DeviceDescription_proto",
        ":DeviceEnums_proto",
        ":DeviceEvent_proto",
        ":PortDescription_proto",
        ":PortEnums_proto",
        ":PortStatistics_proto",
        ":Criterion_proto",
        ":Instruction_proto",
        ":Instructions_proto",
        ":FlowEntryEnums_proto",
        ":FlowEntry_proto",
        ":FlowRuleEnums_proto",
        ":FlowRule_proto",
        ":TraficSelector_proto",
        ":TrafficTreatment_proto",
        ":HostDescription_proto",
        ":HostEnums_proto",
        ":HostEvent_proto",
        ":LinkDescription_proto",
        ":LinkEnums_proto",
        ":LinkEvent_proto",
        ":BandEnums_proto",
        ":Band_proto",
        ":MeterEnums_proto",
        ":MeterEvent_proto",
        ":Meter_proto",
        ":MeterRequest_proto",
        ":OutboundPacket_proto",
        ":PacketEnums_proto",
        ":PacketEvent_proto",
        ":PacketProcessorEntry_proto",
        ":PacketProcessor_proto",
        ":PacketRequest_proto",
        ":RegionEnums_proto",
        ":ConnectPoint_proto",
        ":Device_proto",
        ":DisjointPath_proto",
        ":HostId_proto",
        ":HostLocation_proto",
        ":Host_proto",
        ":Link_proto",
        ":MastershipRole_proto",
        ":Path_proto",
        ":Port_proto",
        ":ProviderId_proto",
        ":Region_proto",
        ":Permission_proto",
    ],
)

### app ###
proto_library(
    name = "ApplicationsEnums_proto",
    srcs = ["app/ApplicationEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

### cfg ###
proto_library(
    name = "ConfigPropertyEnums_proto",
    srcs = ["cfg/ConfigPropertyEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "ConfigProperty_proto",
    srcs = ["cfg/ConfigPropertyProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":ConfigPropertyEnums_proto"],
)

### cluster ###
proto_library(
    name = "NodeId_proto",
    srcs = ["cluster/NodeIdProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "RoleInfo_proto",
    srcs = ["cluster/RoleInfoProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":NodeId_proto"],
)

### core ###

proto_library(
    name = "ApplicationId_proto",
    srcs = ["core/ApplicationIdProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "ApplicationProto_proto",
    srcs = ["core/ApplicationProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":ApplicationId_proto",
        ":ApplicationsEnums_proto",
        ":Permission_proto",
        ":Version_proto",
    ],
)

proto_library(
    name = "Version_proto",
    srcs = ["core/VersionProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

### net ###

### device ###
proto_library(
    name = "DeviceDescription_proto",
    srcs = ["net/device/DeviceDescriptionProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":DeviceEnums_proto"],
)

proto_library(
    name = "DeviceEnums_proto",
    srcs = ["net/device/DeviceEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "DeviceEvent_proto",
    srcs = ["net/device/DeviceEventProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":DeviceEnums_proto",
        ":Device_proto",
        ":Port_proto",
    ],
)

proto_library(
    name = "PortDescription_proto",
    srcs = ["net/device/PortDescriptionProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":PortEnums_proto"],
)

proto_library(
    name = "PortEnums_proto",
    srcs = ["net/device/PortEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "PortStatistics_proto",
    srcs = ["net/device/PortStatisticsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

### flow ###
## criteria ##
proto_library(
    name = "Criterion_proto",
    srcs = ["net/flow/criteria/CriterionProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

## instrcutions ##
proto_library(
    name = "Instruction_proto",
    srcs = ["net/flow/instructions/InstructionProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "Instructions_proto",
    srcs = ["net/flow/instructions/InstructionsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "FlowEntryEnums_proto",
    srcs = ["net/flow/FlowEntryEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "FlowEntry_proto",
    srcs = ["net/flow/FlowEntryProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":FlowEntryEnums_proto"],
)

proto_library(
    name = "FlowRuleEnums_proto",
    srcs = ["net/flow/FlowRuleEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "FlowRule_proto",
    srcs = ["net/flow/FlowRuleProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":FlowRuleEnums_proto",
        ":TrafficTreatment_proto",
        ":TraficSelector_proto",
    ],
)

proto_library(
    name = "TraficSelector_proto",
    srcs = ["net/flow/TrafficSelectorProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":Criterion_proto"],
)

proto_library(
    name = "TrafficTreatment_proto",
    srcs = ["net/flow/TrafficTreatmentProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":Instruction_proto",
        ":Instructions_proto",
    ],
)

#### host ####

proto_library(
    name = "HostDescription_proto",
    srcs = ["net/host/HostDescriptionProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":HostLocation_proto"],
)

proto_library(
    name = "HostEnums_proto",
    srcs = ["net/host/HostEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "HostEvent_proto",
    srcs = ["net/host/HostEventProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":HostEnums_proto",
        ":Host_proto",
    ],
)

#### link ####

proto_library(
    name = "LinkDescription_proto",
    srcs = ["net/link/LinkDescriptionProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":ConnectPoint_proto",
        ":LinkEnums_proto",
    ],
)

proto_library(
    name = "LinkEnums_proto",
    srcs = ["net/link/LinkEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "LinkEvent_proto",
    srcs = ["net/link/LinkEventProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":LinkEnums_proto",
        ":Link_proto",
    ],
)

### meter ####

proto_library(
    name = "BandEnums_proto",
    srcs = ["net/meter/BandEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "Band_proto",
    srcs = ["net/meter/BandProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":BandEnums_proto"],
)

proto_library(
    name = "MeterEnums_proto",
    srcs = ["net/meter/MeterEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "MeterEvent_proto",
    srcs = ["net/meter/MeterEventProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":MeterEnums_proto",
        ":Meter_proto",
    ],
)

proto_library(
    name = "Meter_proto",
    srcs = ["net/meter/MeterProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":ApplicationId_proto",
        ":Band_proto",
        ":MeterEnums_proto",
    ],
)

proto_library(
    name = "MeterRequest_proto",
    srcs = ["net/meter/MeterRequestProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":ApplicationId_proto",
        ":Band_proto",
        ":MeterEnums_proto",
    ],
)

### packet ####

proto_library(
    name = "OutboundPacket_proto",
    srcs = ["net/packet/OutboundPacketProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":TrafficTreatment_proto"],
)

proto_library(
    name = "PacketEnums_proto",
    srcs = ["net/packet/PacketEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "PacketEvent_proto",
    srcs = ["net/packet/PacketEventProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":OutboundPacket_proto",
        ":PacketEnums_proto",
    ],
)

proto_library(
    name = "PacketProcessorEntry_proto",
    srcs = ["net/packet/PacketProcessorEntryProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":PacketProcessor_proto"],
)

proto_library(
    name = "PacketProcessor_proto",
    srcs = ["net/packet/PacketProcessorProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "PacketRequest_proto",
    srcs = ["net/packet/PacketRequestProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":ApplicationId_proto",
        ":NodeId_proto",
        ":TraficSelector_proto",
    ],
)

#### region ####

proto_library(
    name = "RegionEnums_proto",
    srcs = ["net/region/RegionEnumsProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "ConnectPoint_proto",
    srcs = ["net/ConnectPointProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "Device_proto",
    srcs = ["net/DeviceProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":DeviceEnums_proto"],
)

proto_library(
    name = "DisjointPath_proto",
    srcs = ["net/DisjointPathProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":Path_proto"],
)

proto_library(
    name = "HostId_proto",
    srcs = ["net/HostIdProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "HostLocation_proto",
    srcs = ["net/HostLocationProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":ConnectPoint_proto"],
)

proto_library(
    name = "Host_proto",
    srcs = ["net/HostProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":HostId_proto",
        ":HostLocation_proto",
        ":ProviderId_proto",
    ],
)

proto_library(
    name = "Link_proto",
    srcs = ["net/LinkProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [
        ":ConnectPoint_proto",
        ":LinkEnums_proto",
        ":ProviderId_proto",
    ],
)

proto_library(
    name = "MastershipRole_proto",
    srcs = ["net/MastershipRoleProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "Path_proto",
    srcs = ["net/PathProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":Link_proto"],
)

proto_library(
    name = "Port_proto",
    srcs = ["net/PortProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":PortEnums_proto"],
)

proto_library(
    name = "ProviderId_proto",
    srcs = ["net/ProviderIdProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)

proto_library(
    name = "Region_proto",
    srcs = ["net/RegionProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
    deps = [":RegionEnums_proto"],
)

proto_library(
    name = "Permission_proto",
    srcs = ["security/PermissionProto.proto"],
    strip_import_prefix = PROTO_SOURCE_ROOT,
)
