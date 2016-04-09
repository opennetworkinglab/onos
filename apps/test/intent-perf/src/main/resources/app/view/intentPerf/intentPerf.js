/*
 * Copyright 2015-present Open Networking Laboratory
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

/*
 ONOS GUI -- Intent Performance View Module
 */
(function () {
    'use strict';

    // injected refs
    var $log, tbs, ts, wss, sus, flash, fs, mast;

    // internal state
    var handlerMap,
        openListener,
        theSample = [],
        graph;

    // ==========================

    function createGraph(h, samples) {
        var stopped = false,
            n = 243,
            duration = 750,
            now = new Date(Date.now() - duration),
            headers = h,
            data = [];

        var dim = fs.windowSize(mast.mastHeight());
        var margin, width, height, x, y;
        var svg, axis;

        var lines = [],
            paths = [];

        var transition = d3.select({}).transition()
            .duration(duration)
            .ease("linear");

        svg = d3.select("#intent-perf-chart").append("p").append("svg")
            .attr("id", "intent-perf-svg")
            .append("g")
            .attr("id", "intent-perf-svg-g");

        svg.append("defs").append("clipPath")
            .attr("id", "intent-perf-clip")
            .append("rect");

        axis = svg.append("g")
            .attr("class", "x axis")
            .attr("id", "intent-perf-x");

        svg.append("g").attr("class", "y axis")
            .attr("id", "intent-perf-yl");

        svg.append("g")
            .attr("class", "y axis")
            .attr("id", "intent-perf-yr");

        resize(dim);

        headers.forEach(function (h, li) {
            // Prime the data to match the headers and zero it out.
            data[li] = d3.range(n).map(function() { return 0 });

            if (li < headers.length - 1) {
                samples.forEach(function (s, i) {
                    var di = dataIndex(s.time);
                    if (di >= 0) {
                        data[li][di] = s.data[li];
                    }
                });

                data[li].forEach(function (d, i) {
                    if (!d && i > 0) {
                        data[li][i] = data[li][i - 1];
                    }
                });
            } else {
                data[li].forEach(function (t, i) {
                    for (var si = 0; si < headers.length - 1; si++) {
                        data[li][i] = data[si][i];
                    }
                });
            }

            // Create the lines
            lines[li] = d3.svg.line()
                .interpolate("basis")
                .x(function(d, i) { return x(now - (n - 1 - i) * duration); })
                .y(function(d, i) { return y(d); });

            // Create the SVG paths
            paths[li] = svg.append("g")
                .attr("clip-path", "url(#intent-perf-clip)")
                .append("path")
                .datum(function () { return data[li]; })
                .attr("id", "line" + li);

            if (li < headers.length - 1) {
                paths[li].attr("class", "line").style("stroke", lineColor(li));
            } else {
                paths[li].attr("class", "lineTotal");
            }
        });

        function dataIndex(time) {
            var delta = now.getTime() - time;
            var di = Math.round(n - 2 - (delta / duration));
            // $log.info('now=' + now.getTime() + '; then=' + time + '; delta=' + delta + '; di=' + di + ';');
            return di >= n || di < 0 ? -1 : di;
        }

        function lineColor(li) {
            return sus.cat7().getColor(li, false, ts.theme());
        }

        function tick() {
            if (stopped) {
                return;
            }

            transition = transition.each(function() {
                // update the domains
                now = new Date();
                x.domain([now - (n - 2) * duration, now - duration]);

                data.forEach(function (d, li) {
                    // push the new most recent sample onto the back
                    d.push(theSample[li]);

                    // redraw the line and slide it left
                    paths[li].attr("d", lines[li]).attr("transform", null);
                    paths[li].transition()
                        .attr("transform", "translate(" + x(now - (n - 1) * duration) + ")");

                    // pop the old data point off the front
                    d.shift();
                });

                // slide the x-axis left
                axis.call(x.axis);
            }).transition().each("start", tick);
        }

        function start() {
            stopped = false;
            headers.forEach(function (h, li) {
                theSample[li] = data[li][n-1];
            });
            tick();
        }

        function stop() {
            headers.forEach(function (h, li) {
                theSample[li] = 0;
            });
            // Schedule delayed stop to allow 0s to render.
            setTimeout(function () { stopped = true; }, 1000);
        }

        function resize(dim) {
            margin = {top: 20, right: 90, bottom: 20, left: 70};
            width = dim.width - margin.right - margin.left;
            height = 480 - margin.top - margin.bottom;

            x = d3.time.scale()
                .domain([now - (n - 2) * duration, now - duration])
                .range([0, width]);

            y = d3.scale.linear()
                .domain([0, 200000])
                .range([height, 0]);

            d3.select("#intent-perf-svg")
                .attr("width", width + margin.left + margin.right)
                .attr("height", height + margin.top + margin.bottom);
            d3.select("#intent-perf-svg-g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            d3.select("#intent-perf-clip rect")
                .attr("width", width)
                .attr("height", height);

            d3.select("#intent-perf-x")
                .attr("transform", "translate(0," + height + ")")
                .call(x.axis = d3.svg.axis().scale(x).orient("bottom"));

            d3.select("#intent-perf-yl")
                .call(d3.svg.axis().scale(y).orient("left"))
            d3.select("#intent-perf-yr")
                .attr("transform", "translate(" + width + " ,0)")
                .call(d3.svg.axis().scale(y).orient("right"))
        }

        return {
            start: start,
            stop: stop,
            resize: resize
        };
    }


    function wsOpen(host, url) {
        $log.debug('IntentPerf: web socket open - cluster node:', host, 'URL:', url);
        // Request batch of initial data from the new server
        wss.sendEvent('intentPerfStart');
    }

    function createAndInitGraph(d) {
        if (!graph) {
            d.headers.push("total");
            graph = createGraph(d.headers, d.samples);
        }
        graph.start();
    }

    function graphResized(dim) {
        $log.info("Resized: " + dim.width + "x" + dim.height);
        if (graph) {
            graph.resize(dim);
        }
    }

    function recordSample(sample) {
        var total = 0;
        sample.data.forEach(function (d, i) {
            theSample[i] = d;
            total = total + d;
        });
        theSample[sample.data.length] = total;
    }

    function createHandlerMap() {
        handlerMap = {
            intentPerfInit: createAndInitGraph,
            intentPerfSample: recordSample
        };
    }

    // define the controller

    angular.module('ovIntentPerf', ['onosUtil'])
    .controller('OvIntentPerfCtrl',
        ['$scope', '$log', 'ToolbarService', 'WebSocketService',
            'ThemeService', 'FlashService', 'SvgUtilService', 'FnService',
            'MastService',

        function ($scope, _$log_, _tbs_, _wss_, _ts_, _flash_, _sus_, _fs_, _mast_) {
            var self = this;

            $log = _$log_;
            tbs = _tbs_;
            wss = _wss_;
            ts = _ts_;
            flash = _flash_;
            sus = _sus_;
            fs = _fs_;
            mast = _mast_;

            createHandlerMap();

            self.notifyResize = function () {
                graphResized(fs.windowSize(mast.mastHeight()));
            };

            function start() {
                openListener = wss.addOpenListener(wsOpen);
                wss.bindHandlers(handlerMap);
                wss.sendEvent('intentPerfStart');
                $log.debug('intentPerf comms started');
            }

            function stop() {
                graph.stop();
                wss.sendEvent('intentPerfStop');
                wss.unbindHandlers(handlerMap);
                wss.removeOpenListener(openListener);
                openListener = null;
                graph = null;
                $log.debug('intentPerf comms stopped');
            }

            // Cleanup on destroyed scope..
            $scope.$on('$destroy', function () {
                $log.log('OvIntentPerfCtrl is saying Buh-Bye!');
                stop();
            });

            $log.log('OvIntentPerfCtrl has been created');

            start();
        }]);
}());
