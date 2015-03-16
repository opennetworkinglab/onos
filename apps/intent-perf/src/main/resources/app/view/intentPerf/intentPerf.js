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

/*
 ONOS GUI -- Intent Performance View Module
 */
(function () {
    'use strict';

    // injected refs
    var $log, tbs, flash;

    function start() {
        //var format = d3.time.format("%m/%d/%y");
        var format = d3.time.format("%H:%M:%S");
        var samples = [];

        var margin = {top: 20, right: 30, bottom: 30, left: 40},
            width = 960 - margin.left - margin.right,
            height = 500 - margin.top - margin.bottom;

        var x = d3.time.scale()
            .range([0, width]);

        var y = d3.scale.linear()
            .range([height, 0]);

        var z = d3.scale.category20c();

        var xAxis = d3.svg.axis()
            .scale(x)
            .orient("bottom")
            .ticks(d3.time.seconds);

        var yAxis = d3.svg.axis()
            .scale(y)
            .orient("left");

        var stack = d3.layout.stack()
            .offset("zero")
            .values(function(d) { return d.values; })
            .x(function(d) { return d.date; })
            .y(function(d) { return d.value; });

        var nest = d3.nest()
            .key(function(d) { return d.key; });

        var area = d3.svg.area()
            .interpolate("cardinal")
            .x(function(d) { return x(d.date); })
            .y0(function(d) { return y(d.y0); })
            .y1(function(d) { return y(d.y0 + d.y); });

        var svg = d3.select("body").append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
            .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(xAxis);

        svg.append("g")
            .attr("class", "y axis")
            .call(yAxis);

        function fetchData() {
            d3.csv("app/view/intentPerf/data.csv", function (data) {
                samples = data;
                updateGraph();
            });
        }

        function updateGraph() {
            samples.forEach(function(d) {
                d.date = format.parse(d.date);
                d.value = +d.value;
            });

            var layers = stack(nest.entries(samples));

            x.domain(d3.extent(samples, function(d) { return d.date; }));
            y.domain([0, d3.max(samples, function(d) { return d.y0 + d.y; })]);

            svg.selectAll(".layer")
                .data(layers)
                .enter().append("path")
                .attr("class", "layer")
                .attr("d", function(d) { return area(d.values); })
                .style("fill", function(d, i) { return z(i); });

            svg.select(".x")
                .attr("transform", "translate(0," + height + ")")
                .call(xAxis);

            svg.select(".y")
                .call(yAxis);

            console.log('tick');
        }
    }

    start();

    // define the controller

    angular.module('ovIntentPerf', ['onosUtil'])
    .controller('OvIntentPerfCtrl',
        ['$scope', '$log', 'ToolbarService', 'FlashService',

        function ($scope, _$log_, _tbs_, _flash_) {
            var self = this

            $log = _$log_;
            tbs = _tbs_;
            flash = _flash_;

            self.message = 'Hey there dudes!';
            start();

            // Clean up on destroyed scope
            $scope.$on('$destroy', function () {
            });

         $log.log('OvIntentPerfCtrl has been created');
    }]);
}());
