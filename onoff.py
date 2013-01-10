#!/usr/bin/python
import socket
from sys import argv

broadcast_ip = '192.168.0.255'
port = 9

usage = 'Usage: python onoff.py wake|sleep mac'

def sendPacket(byte, mac):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    sock.sendto(byte*6 + mac*16, (broadcast_ip, port))

def wake(mac):
    sendPacket('\xff', mac)
    return 'Sent wake packet'

def sleep(mac):
    sendPacket('\x00', mac)
    return 'Sent sleep packet'

def process(action, mac):
    if action == 'wake':
        return wake(mac)
    elif action == 'sleep':
        return sleep(mac)
    else:
        return usage
        
def hexMac(mac):
    macBytes = mac.split(':')
    return ''.join([chr(int(b, 16)) for b in macBytes])

if __name__ == '__main__':
    if len(argv) < 2:
        print usage
    else:
        action = argv[1]
        mac = hexMac(argv[2])
        print process(action, mac)

