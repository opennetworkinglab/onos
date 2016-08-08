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

    var Collection, Model, region, ts;

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
        console.log(this);

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
//            extra: link.extra
        });

        this.set(attrs);
    }

    function linkEndPoints(srcId, dstId) {

        var sourceNode = this.region.get('devices').get(srcId.substring(0, srcId.length -2));
        var targetNode = this.region.get('devices').get(dstId.substring(0, dstId.length -2));

//        var srcNode = lu[srcId],
//            dstNode = lu[dstId],
//            sMiss = !srcNode ? missMsg('src', srcId) : '',
//            dMiss = !dstNode ? missMsg('dst', dstId) : '';
//
//        if (sMiss || dMiss) {
//            $log.error('Node(s) not on map for link:' + sMiss + dMiss);
//            //logicError('Node(s) not on map for link:\n' + sMiss + dMiss);
//            return null;
//        }

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
                //TODO: original code is: (s && s.expected) && (t && t.expected);
                return true;
            },
            online: function () {
                return true;
                return both && (s && s.online) && (t && t.online);
            },
            linkWidth: function () {
                var s = this.get('fromSource'),
                    t = this.get('fromTarget'),
                    ws = (s && s.linkWidth) || 0,
                    wt = (t && t.linkWidth) || 0;

                    // console.log(s);
                // TODO: Current json is missing linkWidth
                return 1.2;
                return this.get('position').multiLink ? 5 : Math.max(ws, wt);
            },

            restyleLinkElement: function (immediate) {
                // this fn's job is to look at raw links and decide what svg classes
                // need to be applied to the line element in the DOM
                var th = ts.theme(),
                    el = this.el,
                    type = this.get('type'),
                    lw = this.linkWidth(),
                    online = this.online(),
                    modeCls = this.expected() ? 'inactive' : 'not-permitted',
                    delay = immediate ? 0 : 1000;

                console.log(type);

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
                var link = d3.select(el);
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
        ['Topo2Collection', 'Topo2Model', 'ThemeService',

            function (_Collection_, _Model_, _ts_) {

                ts = _ts_;
                Collection = _Collection_;
                Model = _Model_;

                return {
                    createLinkCollection: createLinkCollection
                };
            }
        ]);

})();
