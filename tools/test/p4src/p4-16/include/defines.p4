#ifndef DEFINES
#define DEFINES

#define MAX_PORTS 254
#define CPU_PORT 9w255
#define DROP_PORT 9w511

#define ETH_TYPE_IPV4 16w0x0800
#define IP_TYPE_TCP 8w6
#define IP_TYPE_UDP 8w17

typedef bit<16> group_id_t;
typedef bit<8> group_size_t;
typedef bit<9> port_t;
#endif
