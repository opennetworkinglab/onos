#!/bin/bash

validate_number () {
    local re="^[0-9]+$"
    if [[ ! $1 =~ $re ]] ; then
	return 1
    fi

    return 0
}

find_node () {
    if validate_number $1 ; then
	# input is a number, try to find if an OC node is defined

	oc_try="OC$1"
	node=${!oc_try}

	if [ -n "$node" ]; then
    	    # node lookup succeeded, return node
	    echo $node
	else
    	    # node lookup failed, return original input
	    echo $1
	fi

    else
	echo $1
    fi

    return 0
}
