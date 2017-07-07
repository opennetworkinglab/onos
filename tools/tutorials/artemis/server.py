#!/usr/bin/python3

async_mode = 'threading'

import time
from flask import Flask, render_template, abort
import socketio
from sys import stdin, stdout, stderr
import json
import time
from netaddr import IPNetwork, IPAddress

sio = socketio.Server(logger=False, async_mode=async_mode)
app = Flask(__name__)
app.wsgi_app = socketio.Middleware(sio, app.wsgi_app)
app.config['SECRET_KEY'] = 'secret!'
thread = None
clients = {}


def message_parser(line):
    try:
        temp_message = json.loads(line)
        if temp_message['type'] == 'update':
            for origin in temp_message['neighbor']['message']['update']['announce']['ipv4 unicast']:
                message = {
                    'type': 'A',
                    'timestamp': temp_message['time'],
                    'peer': temp_message['neighbor']['ip'],
                    'host': 'exabgp',
                    'path': temp_message['neighbor']['message']['update']['attribute']['as-path'],
                }
                for prefix in temp_message['neighbor']['message']['update']['announce']['ipv4 unicast'][origin]:
                    message['prefix'] = prefix
                    for sid in clients.keys():
                        try:
                            if IPAddress(str(prefix).split('/')[0]) in clients[sid][0]:
                                print('Sending exa_message to ' +
                                      str(clients[sid][0]), file=stderr)
                                sio.emit(
                                    'exa_message', message, room=sid, namespace='/onos')
                        except:
                            print('Invalid format received from %s'.format(str(sid)))
    except Exception as e:
        print(str(e), file=stderr)


def exabgp_update_event():
    while True:
        line = stdin.readline().strip()
        messages = message_parser(line)


@app.route('/')
def index():
    abort(404)


@sio.on('connect', namespace='/onos')
def onos_connect(sid, environ):
    global thread
    if thread is None:
        thread = sio.start_background_task(exabgp_update_event)


@sio.on('disconnect', namespace='/onos')
def onos_disconnect(sid):
    if sid in clients:
        del clients[sid]


@sio.on('exa_subscribe', namespace='/onos')
def onos_exa_subscribe(sid, message):
    try:
        clients[sid] = [IPNetwork(message['prefix']), True]
    except:
        print('Invalid format received from %s'.format(str(sid)))

if __name__ == '__main__':
    app.run(host='0.0.0.0', threaded=True)
