#!/usr/bin/env bash

bm-cli () {
    if [ -z "$1" ]; then
        echo "No argument supplied. Usage: bm-cli <BMV2 DEVICE ID>"
        return
    fi
    tport=$(head -n 1 /tmp/bmv2-$1-thrift-port)
    echo "Starting CLI for BMv2 instance $1 (Thrift port $tport)..."
    sudo bm_CLI --thrift-port ${tport} ${@:2}
}

bm-dbg () {
    if [ -z "$1" ]; then
        echo "No argument supplied. Usage: bm-dbg <BMV2 DEVICE ID>"
        return
    fi
    tport=$(head -n 1 /tmp/bmv2-$1-thrift-port)
    echo "Starting debugger for BMv2 instance $1 (Thrift port $tport)..."
    sudo bm_p4dbg --thrift-port ${tport} ${@:2}
}

bm-nmsg () {
    if [ -z "$1" ]; then
        echo "No argument supplied. Usage: bm-nmsg <BMV2 DEVICE ID>"
        return
    fi
    tport=$(head -n 1 /tmp/bmv2-$1-thrift-port)
    echo "Starting nanomsg event listener for BMv2 instance $1 (Thrift port $tport)..."
    sudo bm_nanomsg_events --thrift-port ${tport} ${@:2}
}

bm-log () {
    if [ -z "$1" ]; then
        echo "No argument supplied. Usage: bm-log <BMV2 DEVICE ID>"
        return
    fi
    echo "Showing log for BMv2 instance $1..."
    echo "---"
    tail -f /tmp/bmv2-$1-log
}
