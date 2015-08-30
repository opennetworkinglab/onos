/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {

    var ws, flow,
        nodes = [],
        links = [],
        nodeIndexes = {};

    var width = 2400,
        height = 2400;

    var color = d3.scale.category20();

    var force = d3.layout.force()
        .charge(-820)
        .linkDistance(50)
        .size([width, height]);

    // Process flow graph layout
    function createNode(n) {
        nodeIndexes[n.name] = nodes.push(n) - 1;
    }

    function createLink(e) {
        e.source = nodeIndexes[e.src];
        e.target = nodeIndexes[e.dst];
        links.push(e);
    }

    // Returns the newly computed bounding box of the rectangle
    function adjustRectToFitText(n) {
        var text = n.select('text'),
            box = text.node().getBBox();

        text.attr('text-anchor', 'left')
            .attr('y', 2)
            .attr('x', 4);

        // add padding
        box.x -= 4;
        box.width += 8;
        box.y -= 2;
        box.height += 4;

        n.select("rect").attr(box);
    }

    function processFlow() {
        var svg = d3.select("body").append("svg")
            .attr("width", width)
            .attr("height", height);

        flow.steps.forEach(createNode);
        flow.requirements.forEach(createLink);

        force
            .nodes(nodes)
            .links(links)
            .start();

        var link = svg.selectAll(".link")
            .data(links)
          .enter().append("line")
            .attr("class", "link")
            .style("stroke-width", function(d) { return d.isSoft ? 1 : 2; });

        var node = svg.selectAll(".node")
            .data(nodes)
          .enter().append("g")
            .attr("class", "node")
            .call(force.drag);

        node.append("rect")
            .attr({ rx: 5, ry:5, width:180, height:18 })
            .style("fill", function(d) { return color(d.group); });

        node.append("text").text( function(d) { return d.name; })
            .attr({ dy:"1.1em", width:100, height:16, x:4, y:2 });

        node.append("title")
            .text(function(d) { return d.name; });

        force.on("tick", function() {
            link.attr("x1", function(d) { return d.source.x; })
                .attr("y1", function(d) { return d.source.y; })
                .attr("x2", function(d) { return d.target.x; })
                .attr("y2", function(d) { return d.target.y; });

            node.attr("transform", function(d) { return "translate(" + (d.x - 180/2) + "," + (d.y - 18/2) + ")"; });
        });
    }


    // Web socket callbacks

    function handleOpen() {
        console.log('WebSocket open');
    }

    // Handles the specified (incoming) message using handler bindings.
    function handleMessage(msg) {
        console.log('rx: ', msg);
        evt = JSON.parse(msg.data);
        if (evt.event === 'progress') {

        } else if (evt.event === 'log') {

        } else if (evt.event === 'flow') {
            flow = evt.payload;
            processFlow();
        }
    }

    function handleClose() {
        console.log('WebSocket closed');
    }

    if (false) {
        d3.json("data.json", function (error, data) {
            flow = data;
            processFlow();
        });
        return;
    }

    // Open the web-socket
    ws = new WebSocket(document.location.href.replace('http:', 'ws:'));
    if (ws) {
        ws.onopen = handleOpen;
        ws.onmessage = handleMessage;
        ws.onclose = handleClose;
    }

})();