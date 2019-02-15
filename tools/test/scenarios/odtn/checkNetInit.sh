#!/bin/bash

# Two input parameters:
# $1 - one of {device, port, link}, specify what needs to be checked.
# $2 - IP address of ONOS instance.

line_num=`cat ~/emulator/net-summary.json | wc -l`
if [[ "$line_num" != "1" ]]; then
    echo "JSON file should have only 1 line."
    exit 1
fi

# Extract specific value from returned json string under onos command "odtn-show-tapi-context"
function get_json_value()
{
    local json=$1
    local key=$2

    if [[ -z "$3" ]]; then
    local num=1
    else
    local num=$3
    fi

    local value=$(echo "${json}" | awk -F"[,:}]" '{for(i=1;i<=NF;i++){if($i~/'${key}'\042/){print $(i+1)}}}' | tr -d '"' | sed -n ${num}p)

    return ${value}
}

tried=0
case "$1" in
    "device" )
        get_json_value $( cat ~/emulator/net-summary.json) device_num
        device_num=$?
        num_in_topo=`onos $2 devices | wc -l`
        num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<node>" | wc -l`
        while [[ "$num_in_topo" != "$device_num" || "$num_in_tapi" != "$device_num" ]]
        do
            echo "On ONOS $2, current device num in topo:$num_in_topo, num in tapi:$num_in_tapi, expected $device_num. Waiting..."
            sleep 10
            num_in_topo=`onos $2 devices | wc -l`
                num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<node>" | wc -l`
            let "tried=tried+1"
            if [[ "$tried" == "10" ]]; then
                exit 99
            fi
        done
        ;;
    "port" )
        get_json_value $( cat ~/emulator/net-summary.json) port_num
        port_num=$?
        get_json_value $( cat ~/emulator/net-summary.json) device_num
        device_num=$?
        num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<owned-node-edge-point>" | wc -l`
        num_in_topo=`onos $2 ports | wc -l`
        num_in_topo=$[num_in_topo-device_num]
        while [[ "$num_in_topo" != "$port_num" || "$num_in_tapi" != "$port_num" ]]
            do
            echo "On ONOS $2, current port num in topo: $num_in_topo, num in tapi: $num_in_tapi, expected $port_num. Waiting..."
                    sleep 10
            num_in_topo=`onos $2 ports | wc -l`
            num_in_topo=$[num_in_topo-device_num]
            num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<owned-node-edge-point>" | wc -l`
            let "tried=tried+1"
            if [[ "$tried" == "10" ]]; then
                exit 99
            fi
            done
            ;;
    "link" )
        get_json_value $( cat ~/emulator/net-summary.json) link_num
        link_num=$?
        num_in_topo=`onos $2 links | wc -l`
        num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<link>" | wc -l`
            while [[ "$num_in_topo" != "$link_num" || "$num_in_tapi" != "$link_num" ]]
            do
                    echo "On ONOS $2, current link num: $num_in_topo, expected $link_num. Waiting..."
            sleep 10
                    num_in_topo=`onos $2 links | wc -l`
            num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<link>" | wc -l`
            let "tried=tried+1"
            if [[ "$tried" == "10" ]]; then
                exit 99
            fi
            done
esac
