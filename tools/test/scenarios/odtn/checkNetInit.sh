#!/bin/bash

# Three input parameters:
# $1 - one of {device, port, link}, specify what needs to be checked.
# $2 - IP address of ONOS instance.
# $3 - Optional. absolute path of net summary json file. The default path is "/tmp/odtn/net-summary.json".

summary="/tmp/odtn/net-summary.json"
if [[ $# == 3 ]];then
    summary=$3
fi
# The 'sed'command behind 'wc -l' is uset to strip leading spaces.
# Because in some scenarios, 'wc -l' always outputs leading spaces (https://lists.gnu.org/archive/html/bug-coreutils/2005-01/msg00029.html).
line_num=`cat $summary | wc -l | sed -e 's/^[ ]*//g'`
if [[ "$line_num" != "1" ]]; then
    echo "JSON file should have only 1 line."
    exit 1
fi
content=`cat $summary`
echo -e "The content of the json file is :\n $content"
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

    eval value=$(echo "${json}" | awk -F"[,:}]" '{for(i=1;i<=NF;i++){if($i~/'${key}'\042/){print $(i+1)}}}' | tr -d '"' | sed -n ${num}p)

    return ${value}
}

tried=0
case "$1" in
    "device" )
        eval get_json_value '$content' device_num
        device_num=$?
        num_in_topo=`onos $2 devices | wc -l | sed -e 's/^[ ]*//g'`
        num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<node>" | wc -l | sed -e 's/^[ ]*//g'`
        while [[ "$num_in_topo" != "$device_num" || "$num_in_tapi" != "$device_num" ]]
        do
            echo "On ONOS $2, current device num in topo:$num_in_topo, num in tapi:$num_in_tapi, expected $device_num. Waiting..."
            sleep 10
            num_in_topo=`onos $2 devices | wc -l | sed -e 's/^[ ]*//g'`
                num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<node>" | wc -l | sed -e 's/^[ ]*//g'`
            let "tried=tried+1"
            if [[ "$tried" == "10" ]]; then
                exit 99
            fi
        done
        ;;
    "port" )
        eval get_json_value '$content' port_num
        port_num=$?
        eval get_json_value '$content' device_num
        device_num=$?
        num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<owned-node-edge-point>" | wc -l | sed -e 's/^[ ]*//g'`
        num_in_topo=`onos $2 ports | wc -l | sed -e 's/^[ ]*//g'`
        num_in_topo=$[num_in_topo-device_num]
        while [[ "$num_in_topo" != "$port_num" || "$num_in_tapi" != "$port_num" ]]
            do
            echo "On ONOS $2, current port num in topo: $num_in_topo, num in tapi: $num_in_tapi, expected $port_num. Waiting..."
                    sleep 10
            num_in_topo=`onos $2 ports | wc -l | sed -e 's/^[ ]*//g'`
            num_in_topo=$[num_in_topo-device_num]
            num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<owned-node-edge-point>" | wc -l | sed -e 's/^[ ]*//g'`
            let "tried=tried+1"
            if [[ "$tried" == "10" ]]; then
                exit 99
            fi
            done
            ;;
    "link" )
        eval get_json_value '$content' link_num
        link_num=$?
        num_in_topo=`onos $2 links | wc -l | sed -e 's/^[ ]*//g'`
        num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<link>" | wc -l | sed -e 's/^[ ]*//g'`
            while [[ "$num_in_topo" != "$link_num" || "$num_in_tapi" != "$link_num" ]]
            do
                    echo "On ONOS $2, current link num: $num_in_topo, expected $link_num. Waiting..."
            sleep 10
                    num_in_topo=`onos $2 links | wc -l | sed -e 's/^[ ]*//g'`
            num_in_tapi=`onos $2 odtn-show-tapi-context | grep "<link>" | wc -l | sed -e 's/^[ ]*//g'`
            let "tried=tried+1"
            if [[ "$tried" == "10" ]]; then
                exit 99
            fi
            done
esac
