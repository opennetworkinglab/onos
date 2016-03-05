#!/usr/bin/env python
import sys
import os
import fcntl
import socket
from struct import pack

def getIPAddress(intf):
    #Borrowed from:
    #http://stackoverflow.com/questions/24196932/how-can-i-get-the-ip-address-of-eth0-in-python
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
            s.fileno(),
            0x8915,  # SIOCGIFADDR
            pack('256s', intf[:15])
    )[20:24])

def gratuitousArp(intf, ip=None, mac=None):
    #Adapted from:
    #https://github.com/krig/send_arp.py/blob/master/send_arp.py
    sock = socket.socket(socket.AF_PACKET, socket.SOCK_RAW)
    try:
        sock.bind((intf, socket.SOCK_RAW))
    except:
        print 'Device does not exist: %s' % intf
        return

    if not ip:
        try:
            ip = getIPAddress(intf)
        except IOError:
            print 'No IP for %s' % intf
            return
    packed_ip = pack('!4B', *[int(x) for x in ip.split('.')])

    if mac:
        packed_mac = pack('!6B', *[int(x,16) for x in mac.split(':')])
    else:
        packed_mac = sock.getsockname()[4]

    bcast_mac = pack('!6B', *(0xFF,)*6)
    zero_mac = pack('!6B', *(0x00,)*6)
    eth_arp = pack('!H', 0x0806)
    arp_proto = pack('!HHBBH', 0x0001, 0x0800, 0x0006, 0x0004, 0x0001)
    arpframe = [
        ## ETHERNET
        # destination MAC addr
        bcast_mac,
        # source MAC addr
        packed_mac,
        # eth proto
        eth_arp,

        ## ARP
        arp_proto,
        # sender MAC addr
        packed_mac,
        # sender IP addr
        packed_ip,
        # target hardware addr
        bcast_mac,
        # target IP addr
        packed_ip
    ]

    # send the ARP packet
    sock.send(''.join(arpframe))

if __name__ == "__main__":
    if len(sys.argv) > 1:
        intfs = sys.argv[1:]
    else:
        intfs = os.listdir('/sys/class/net/')

    for intf in intfs:
        gratuitousArp(intf)