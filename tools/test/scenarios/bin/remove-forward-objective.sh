# /bin/sh

if [ "$#" -ne 6 ]
then
   echo usage: create-forward-objective.sh onos device src-mac dst-mac src-port dst-port
   exit 1
fi


onos=$1
device=$2
srcMac=$3
dstMac=$4
srcPort=$5
dstPort=$6

curl -u onos:rocks -X POST --header "Content-Type: application/json" --header "Accept: application/json" -d "{
  \"priority\": 100,
  \"isPermanent\": \"false\",
  \"timeout\": 100,
  \"flag\": \"VERSATILE\",
  \"operation\": \"REMOVE\",
  \"selector\": {
    \"criteria\": [
        {\"type\": \"ETH_TYPE\", \"ethType\": 2048},
        {\"type\": \"IN_PORT\", \"port\": \"$srcPort\"},
        {\"type\": \"ETH_DST\", \"mac\": \"$dstMac\"},
        {\"type\": \"ETH_SRC\", \"mac\": \"$srcMac\"}
    ]
  },
  \"treatment\":
  {
    \"instructions\":
    [
      {\"type\":\"OUTPUT\",\"port\":$dstPort}
    ],
    \"deferred\":[]
  }
}" http://${onos}:8181/onos/v1/flowobjectives/$device/forward

echo
