/*
 * Copyright 2016-present Open Networking Laboratory
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
 ONOS GUI -- Topology Links Module.
 Module that holds the links for a region
 */

(function () {
    'use strict';

    var $log;
    var Collection, Model, region, ts, sus;

    var linkLabelOffset = '0.35em';

    var widthRatio = 1.4,
        linkScale = d3.scale.linear()
            .domain([1, 12])
            .range([widthRatio, 12 * widthRatio])
            .clamp(true),
        allLinkTypes = 'direct indirect optical tunnel UiDeviceLink',
        allLinkSubTypes = 'inactive not-permitted';

    // configuration
    var linkConfig = {
        light: {
            baseColor: '#939598',
            inColor: '#66f',
            outColor: '#f00'
        },
        dark: {
            // TODO : theme
            baseColor: '#939598',
            inColor: '#66f',
            outColor: '#f00'
        },
        inWidth: 12,
        outWidth: 10
    };

    var defaultLinkType = 'direct',
        nearDist = 15;

    function createLink() {

        var linkPoints = this.linkEndPoints(this.get('epA'), this.get('epB'));

        var attrs = angular.extend({}, linkPoints, {
            key: this.get('id'),
            class: 'link',
            weight: 1,
            srcPort: this.get('srcPort'),
            tgtPort: this.get('dstPort'),
            position: {
                x1: 0,
                y1: 0,
                x2: 0,
                y2: 0
            }
            // functions to aggregate dual link state
            // extra: link.extra
        });

        this.set(attrs);
    }

    function rectAroundText(el) {
        var text = el.select('text'),
            box = text.node().getBBox();

        // translate the bbox so that it is centered on [x,y]
        box.x = -box.width / 2;
        box.y = -box.height / 2;

        // add padding
        box.x -= 4;
        box.width += 8;
        return box;
    }

    function linkEndPoints(srcId, dstId) {

        var sourceNode = this.region.findNodeById(srcId)
        var targetNode = this.region.findNodeById(dstId)

        if (!sourceNode || !targetNode) {
            $log.error('Node(s) not on map for link:' + srcId + ':' + dstId);
            //logicError('Node(s) not on map for link:\n' + sMiss + dMiss);
            return null;
        }

        this.source = sourceNode.toJSON();
        this.target = targetNode.toJSON();

        return {
            source: sourceNode,
            target: targetNode
        };
    }

    function createLinkCollection(data, _region) {

        var LinkModel = Model.extend({
            region: _region,
            createLink: createLink,
            linkEndPoints: linkEndPoints,
            type: function () {
                return this.get('type');
            },
            expected: function () {
                // TODO: original code is: (s && s.expected) && (t && t.expected);
                return true;
            },
            online: function () {
                // TODO: remove next line
                return true;

                return both && (s && s.online) && (t && t.online);
            },
            enhance: function () {
                var data = [],
                    point;

                angular.forEach(this.collection.models, function (link) {
                    link.unenhance();
                });

                this.el.classed('enhanced', true);
                point = this.locatePortLabel();
                angular.extend(point, {
                    id: 'topo-port-tgt',
                    num: this.get('portB')
                });
                data.push(point);

                var entering = d3.select('#topo-portLabels').selectAll('.portLabel')
                    .data(data).enter().append('g')
                    .classed('portLabel', true)
                    .attr('id', function (d) { return d.id; });

                entering.each(function (d) {
                    var el = d3.select(this),
                        rect = el.append('rect'),
                        text = el.append('text').text(d.num);

                    rect.attr(rectAroundText(el))
                        .attr('rx', 2)
                        .attr('ry', 2);

                    text.attr('dy', linkLabelOffset)
                        .attr('text-anchor', 'middle');

                    el.attr('transform', sus.translate(d.x, d.y));
                });
            },
            unenhance: function () {
                this.el.classed('enhanced', false);
                d3.select('#topo-portLabels').selectAll('.portLabel').remove();
            },
            locatePortLabel: function (link, src) {
                var offset = 32,
                    pos = this.get('position'),
                    nearX = src ? pos.x1 : pos.x2,
                    nearY = src ? pos.y1 : pos.y2,
                    farX = src ? pos.x2 : pos.x1,
                    farY = src ? pos.y2 : pos.y1;

                function dist(x, y) { return Math.sqrt(x*x + y*y); }

                var dx = farX - nearX,
                    dy = farY - nearY,
                    k = offset / dist(dx, dy);

                return {x: k * dx + nearX, y: k * dy + nearY};
            },
            restyleLinkElement: function (immediate) {
                // this fn's job is to look at raw links and decide what svg classes
                // need to be applied to the line element in the DOM
                var th = ts.theme(),
                    el = this.el,
                    type = this.get('type'),
                    online = this.online(),
                    modeCls = this.expected() ? 'inactive' : 'not-permitted',
                    lw = 1.2,
                    delay = immediate ? 0 : 1000;

                // NOTE: understand why el is sometimes undefined on addLink events...
                // Investigated:
                // el is undefined when it's a reverse link that is being added.
                // updateLinks (which sets ldata.el) isn't called before this is called.
                // Calling _updateLinks in addLinkUpdate fixes it, but there might be
                // a more efficient way to fix it.
                if (el && !el.empty()) {
                    el.classed('link', true);
                    el.classed(allLinkSubTypes, false);
                    el.classed(modeCls, !online);
                    el.classed(allLinkTypes, false);
                    if (type) {
                        el.classed(type, true);
                    }
                    el.transition()
                        .duration(delay)
                        .attr('stroke-width', linkScale(lw))
                        .attr('stroke', linkConfig[th].baseColor);
                }
            },
            onEnter: function (el) {
                var _this = this,
                    link = d3.select(el);

                this.el = link;

                this.restyleLinkElement();

                if (this.get('type') === 'hostLink') {
                    sus.visible(link, api.showHosts());
                }
            }
        });

        var LinkCollection = Collection.extend({
            model: LinkModel,
        });

        return new LinkCollection(data);
    }

    angular.module('ovTopo2')
    .factory('Topo2LinkService',
        ['$log', 'Topo2Collection', 'Topo2Model', 'ThemeService', 'SvgUtilService',

            function (_$log_, _Collection_, _Model_, _ts_, _sus_) {

                $log = _$log_;
                ts = _ts_;
                sus = _sus_;
                Collection = _Collection_;
                Model = _Model_;

                return {
                    createLinkCollection: createLinkCollection
                };
            }
        ]);

})();
