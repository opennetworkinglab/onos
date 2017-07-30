#ifndef DEFINES
#define DEFINES

#define MAX_PORTS 254

#define ETH_TYPE_IPV4 16w0x800
#define IP_TYPE_TCP 8w6
#define IP_TYPE_UDP 8w17

typedef bit<9> port_t;

const port_t CPU_PORT = 255;
const port_t DROP_PORT = 511;

#endif
