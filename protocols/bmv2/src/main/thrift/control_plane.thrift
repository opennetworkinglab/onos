namespace java org.p4.bmv2.thrift
namespace cpp cpservice

service ControlPlaneService {

   bool ping(),

   oneway void packetIn(1: i32 port, 2: i64 reason, 3: i32 tableId, 4: i32 contextId, 5: binary packet),

   oneway void hello(1: i32 thriftServerPort, 2: i32 deviceId)

}