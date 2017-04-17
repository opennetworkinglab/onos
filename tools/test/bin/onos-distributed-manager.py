#!/bin/python
"""
 Copyright 2017-present Open Networking Laboratory
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
"""

from flask import Flask, jsonify, request

app = Flask(__name__)
controller_list = {}

"""
Onos Distributed Manager
this app handles controller information in system

Message Samples :
--Adding node
curl  -H "Content-Type: application/json" -X POST
     --data '{"Id":"1.2.3.4","IpAddress":"1.2.3.4","Port":3456,"IsEnable":true}' http://localhost:5000
--Updating node
curl  -H "Content-Type: application/json" -X PUT --data '{"Id":"1.2.3.4","IsEnable":false}' http://localhost:5000
--Deleting node
curl  -H "Content-Type: application/json" -X DELETE --data '{"Id":"1.2.3.4","IsEnable":false}' http://localhost:5000
--Getting node data
curl  -X GET  http://10.15.176.228:5000/cluster.json

"""


@app.route('/', methods=['GET', 'POST', 'DELETE', 'PUT'])
def data_handler():

    if request.method == 'GET':
        pagereturn = "<h2> Onos Distributed Controller Manager2 </h2>"
        pagereturn += "<br><h3> Status of Added controllers  </h3><br>"
        pagereturn += " Id,&emsp; Ip,&emsp; Port,&emsp; Is Active <br> "

        for key in controller_list.keys():
            pagereturn += controller_list[key]["Id"] + ",&emsp; " + \
                  controller_list[key]["IpAddress"] + ",&emsp; " + \
                  str(controller_list[key]["Port"]) + ",&emsp; " + \
                  str(controller_list[key]["IsEnable"])
            pagereturn += " <br>"

        return pagereturn
    elif request.method == 'POST':

        if request.is_json:
            content = dict(request.json)

            if content["Id"] in controller_list:
                return "Content Id is already in the list"

            else:
                controller_list[content["Id"]] = content

        else:
            return "json required"

        return "POST called with content"

    elif request.method == 'PUT':
        if request.is_json:
            content = dict(request.json)

            if content["Id"] in controller_list:
                controller_list[content["Id"]] = content

            else:
                return "Id %s is not found ", content["Id"]
        else:
            return "json data is missing"

    elif request.method == 'DELETE':
        if request.is_json:
            content = dict(request.json)

        else:
            return "No json is found"

        if content["Id"] in controller_list:
            del controller_list[content["Id"]]
            return "Deletion succeed."

        else:
            return "Id is not found"

    else:
        return "Undefined method call"
"""
This function returns onos cluster information
based on data is
"""


@app.route('/cluster.json', methods=['GET'])
def cluster_responder():

    cluster_info = dict()
    nodes = list()
    partition = dict()
    # Todo: For first release , only 1 partition implemented
    cluster_members = list()

    # "nodes" array
    for controller_id in controller_list:
        controller_node = controller_list[controller_id]
        if controller_node["IsEnable"]:
            node_data = dict()
            node_data["ip"] = controller_node["IpAddress"]
            node_data["id"] = controller_node["Id"]
            node_data["port"] = controller_node["Port"]
            nodes.append(node_data)
            cluster_members.append(controller_node["Id"])

    partition["id"] = 1  # Todo: this will be updated .
    partition["members"] = cluster_members

    cluster_info["nodes"] = nodes
    cluster_info["name"] = -1394421542720337000
    cluster_info["partitions"] = partition
    return jsonify(cluster_info)

if __name__ == '__main__':
    app.run(host="0.0.0.0", debug=True)