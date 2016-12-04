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

    var $log, Collection, Model, ts, sus, t2zs, t2vs, t2lps, fn;

    var linkLabelOffset = '0.35em';

    var widthRatio = 1.4,
        linkScale = d3.scale.linear()
            .domain([1, 12])
            .range([widthRatio, 12 * widthRatio])
            .clamp(true),
        allLinkTypes = 'direct optical tunnel UiDeviceLink',
        allLinkSubTypes = 'not-permitted',
        labelDim = 30;

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

    function createLink() {

        var linkPoints = this.linkEndPoints(this.get('epA'), this.get('epB'));

        var attrs = angular.extend({}, linkPoints, {
            key: this.get('id'),
            class: 'link',
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

    function isLinkOnline(node) {
        return (node.get('nodeType') === 'region') ? true : node.get('online');
    }

    function linkEndPoints(srcId, dstId) {

        var allNodes = this.region.nodes();
        var sourceNode = this.region.findNodeById(this, srcId);
        var targetNode = this.region.findNodeById(this, dstId);

        if (!sourceNode || !targetNode) {
            $log.error('Node(s) not on map for link:' + srcId + '~' + dstId);
            // logicError('Node(s) not on map for link:\n' + sMiss + dMiss);
            return null;
        }

        this.source = allNodes.indexOf(sourceNode);
        this.target = allNodes.indexOf(targetNode);
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;

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
            svgClassName: function () {
                return fn.classNames('link',
                    this.nodeType,
                    this.get('type'),
                    {
                        enhanced: this.get('enhanced'),
                        selected: this.get('selected')
                    }
                );
            },
            expected: function () {
                // TODO: original code is: (s && s.expected) && (t && t.expected);
                return true;
            },
            online: function () {

                var source = this.get('source'),
                    target = this.get('target'),
                    sourceOnline = isLinkOnline(source),
                    targetOnline = isLinkOnline(target);

                return (sourceOnline) && (targetOnline);
            },
            onChange: function () {
                // Update class names when the model changes
                if (this.el) {
                    this.el.attr('class', this.svgClassName());
                }
            },
            enhance: function () {
                var data = [],
                    point;

                angular.forEach(this.collection.models, function (link) {
                    link.unenhance();
                });

                this.set('enhanced', true);

                if (showPort()) {
                    point = this.locatePortLabel();
                    angular.extend(point, {
                        id: 'topo-port-tgt',
                        num: this.get('portB')
                    });
                    data.push(point);

                    if (this.get('portA')) {
                        point = this.locatePortLabel(1);
                        angular.extend(point, {
                            id: 'topo-port-src',
                            num: this.get('portA')
                        });
                        data.push(point);
                    }

                    var entering = d3.select('#topo-portLabels')
                        .selectAll('.portLabel')
                        .data(data)
                        .enter().append('g')
                        .classed('portLabel', true)
                        .attr('id', function (d) { return d.id; })

                    entering.each(function (d) {
                        var el = d3.select(this),
                            rect = el.append('rect'),
                            text = el.append('text').text(d.num);

                        var rectSize = rectAroundText(el);

                        rect.attr(rectSize)
                            .attr('rx', 2)
                            .attr('ry', 2);

                        text.attr('dy', linkLabelOffset)
                            .attr('text-anchor', 'middle');

                        el.attr('transform', sus.translate(d.x, d.y));

                    });

                    this.setScale();
                }
            },
            unenhance: function () {
                this.set('enhanced', false);
                d3.select('#topo-portLabels').selectAll('.portLabel').remove();
            },
            select: function () {
                var ev = d3.event;

                // TODO: if single selection clear selected devices, hosts, sub-regions
                var s = Boolean(this.get('selected'));
                // Clear all selected Items
                _.each(this.collection.models, function (m) {
                    m.set('selected', false);
                });

                this.set('selected', !s);

                var selected = this.collection.filter(function (m) {
                    return m.get('selected');
                });

                return selected;
            },
            showDetails: function () {
                var selected = this.select(d3.event);

                if (selected) {
                    t2lps.displayLink(this);
                } else {
                    t2lps.hide();
                }
            },
            locatePortLabel: function (src) {

                var offset = 32 / (labelDim * t2zs.scale()),
                    sourceX = this.get('position').x1,
                    sourceY = this.get('position').y1,
                    targetX = this.get('position').x2,
                    targetY = this.get('position').y2,
                    nearX = src ? sourceX : targetX,
                    nearY = src ? sourceY : targetY,
                    farX = src ? targetX : sourceX,
                    farY = src ? targetY : sourceY;

                function dist(x, y) {
                    return Math.sqrt(x * x + y * y);
                }

                var dx = farX - nearX,
                    dy = farY - nearY,
                    k = (32 * offset) / dist(dx, dy);

                return { x: k * dx + nearX, y: k * dy + nearY };
            },
            restyleLinkElement: function (immediate) {
                // this fn's job is to look at raw links and decide what svg classes
                // need to be applied to the line element in the DOM
                var th = ts.theme(),
                    el = this.el,
                    type = this.get('type'),
                    online = this.online(),
                    modeCls = this.expected() ? '-inactive' : 'not-permitted',
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
                        .attr('stroke', linkConfig[th].baseColor);

                    this.setScale();
                }
            },
            onEnter: function (el) {
                var link = d3.select(el);

                this.el = link;
                this.restyleLinkElement();

                // TODO: Needs improving - originally this was calculated
                // from mouse position.
                this.el.on('mouseover', this.enhance.bind(this));
                this.el.on('mouseout', this.unenhance.bind(this));
                this.el.on('click', this.showDetails.bind(this));

                if (this.get('type') === 'hostLink') {
                    // sus.visible(link, api.showHosts());
                }
            },
            setScale: function () {
                var width = linkScale(widthRatio) / t2zs.scale();
                this.el.style('stroke-width', width + 'px');

                var labelScale = labelDim / (labelDim * t2zs.scale());

                d3.select('#topo-portLabels')
                    .selectAll('.portLabel')
                    .selectAll('*')
                    .style('transform', 'scale(' + labelScale + ')');

            },
            update: function () {
                if (this.get('enhanced')) {
                    this.enhance();
                }
            }
        });

        var LinkCollection = Collection.extend({
            model: LinkModel
        });

        return new LinkCollection(data);
    }

    function showPort() {
        return t2vs.getPortHighlighting();
    }

    angular.module('ovTopo2')
    .factory('Topo2LinkService',
        ['$log', 'Topo2Collection', 'Topo2Model',
        'ThemeService', 'SvgUtilService', 'Topo2ZoomService',
        'Topo2ViewService', 'Topo2LinkPanelService', 'FnService',
            function (_$log_, _Collection_, _Model_, _ts_, _sus_,
                _t2zs_, _t2vs_, _t2lps_, _fn_) {

                $log = _$log_;
                ts = _ts_;
                sus = _sus_;
                t2zs = _t2zs_;
                t2vs = _t2vs_;
                Collection = _Collection_;
                Model = _Model_;
                t2lps = _t2lps_;
                fn = _fn_;

                return {
                    createLinkCollection: createLinkCollection
                };
            }
        ]);

})();
