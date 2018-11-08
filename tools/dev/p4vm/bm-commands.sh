#!/usr/bin/env bash

bm-cli () {
    if [ -z "$1" ]; then
        echo "No argument supplied. Usage: bm-cli <MININET SWITCH NAME>"
        return
    fi
    tport=$(head -n 1 /tmp/bmv2-$1-thrift-port)
    echo "Starting CLI for BMv2 instance $1 (Thrift port $tport)..."
    bm_CLI --thrift-port ${tport} --pre SimplePreLAG ${@:2}
}

bm-dbg () {
    if [ -z "$1" ]; then
        echo "No argument supplied. Usage: bm-dbg <MININET SWITCH NAME>"
        return
    fi
    tport=$(head -n 1 /tmp/bmv2-$1-thrift-port)
    echo "Starting debugger for BMv2 instance $1 (Thrift port $tport)..."
    sudo bm_p4dbg --thrift-port ${tport} --socket ipc:///tmp/bmv2-$1-debug.ipc ${@:2}
}

bm-nmsg () {
    if [ -z "$1" ]; then
        echo "No argument supplied. Usage: bm-nmsg <MININET SWITCH NAME>"
        return
    fi
    tport=$(head -n 1 /tmp/bmv2-$1-thrift-port)
    echo "Starting nanomsg event listener for BMv2 instance $1 (Thrift port $tport)..."
    sudo bm_nanomsg_events --thrift-port ${tport} ${@:2}
}

bm-log () {
    if [ -z "$1" ]; then
        echo "No argument supplied. Usage: bm-log <MININET SWITCH NAME>"
        return
    fi
    echo "Showing log for BMv2 instance $1..."
    echo "---"
    tail -f /tmp/bmv2-$1-log
}
