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

    var $log, Collection, Model, ts, sus, t2zs, t2vs, t2lps,
        fn, ps, t2mss, t2ts;

    var linkLabelOffset = '0.35em';

    var widthRatio = 1.4,
        linkScale = d3.scale.linear()
            .domain([1, 12])
            .range([widthRatio, 12 * widthRatio])
            .clamp(true),
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

        var findNodeById = this.region.model.findNodeById.bind(this.region),
            allNodes = this.region.model.nodes(),
            sourceNode = findNodeById(this, srcId),
            targetNode = findNodeById(this, dstId);

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
            initialize: function () {
                this.super = this.constructor.__super__;
                this.super.initialize.apply(this, arguments);
                this.createLink();
            },
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
                        selected: this.get('selected'),
                        suppressedmax: this.get('mastership')
                    },
                    (this.linkLabel) ? this.linkLabel.linkLabelCSSClass() : null
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
                    this.setScale();
                }
            },
            enhance: function () {
                var data = [],
                    point;

                if (showPort()) {
                    this.set('enhanced', true);
                    point = this.locatePortLabel();
                    angular.extend(point, {
                        id: 'topo2-port-tgt',
                        num: this.get('portB')
                    });
                    data.push(point);

                    if (this.get('portA')) {
                        point = this.locatePortLabel(1);
                        angular.extend(point, {
                            id: 'topo2-port-src',
                            num: this.get('portA')
                        });
                        data.push(point);
                    }

                    var entering = d3.select('.topo2-portLabels')
                        .selectAll('.portLabel')
                        .data(data)
                        .enter().append('g')
                        .classed('portLabel', true)
                        .attr('id', function (d) { return d.id; });

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
                d3.select('.topo2-portLabels').selectAll('.portLabel').remove();
                this.setScale();
            },
            amt: function (numLinks, index) {
                var bbox = this.get('source').el.node().getBBox(),
                    gap = bbox.width / 4;
                return (index - ((numLinks - 1) / 2)) * gap;
            },
            defaultPosition: function () {
                return {
                    x1: this.get('source').x,
                    y1: this.get('source').y,
                    x2: this.get('target').x,
                    y2: this.get('target').y
                }
            },
            calcMovement: function (amt, flipped) {
                var pos = this.defaultPosition(),
                    mult = flipped ? -amt : amt,
                    dx = pos.x2 - pos.x1,
                    dy = pos.y2 - pos.y1,
                    length = Math.sqrt((dx * dx) + (dy * dy));

                return {
                    x1: pos.x1 + (mult * dy / length),
                    y1: pos.y1 + (mult * -dx / length),
                    x2: pos.x2 + (mult * dy / length),
                    y2: pos.y2 + (mult * -dx / length)
                };
            },
            setPosition: function () {
                var multiline = this.get('multiline');
                if (multiline) {
                    var offsetAmt = this.amt(multiline.deviceLinks, multiline.index);
                    this.set('position', this.calcMovement(offsetAmt));
                } else {
                    this.set('position', this.defaultPosition());
                }

                if (this.get('enhanced')) {
                    this.updatePortPosition();
                }

                if (this.el) {
                    this.el.attr(this.get('position'));
                }

                if (this.linkLabel) {
                    this.linkLabel.setPosition();
                }
            },
            updatePortPosition: function () {
                var sourcePos = this.locatePortLabel(1),
                    targetPos = this.locatePortLabel();
                d3.select('#topo2-port-src')
                    .attr('transform', sus.translate(sourcePos.x, sourcePos.y));
                d3.select('#topo2-port-tgt')
                    .attr('transform', sus.translate(targetPos.x, targetPos.y));
            },
            getSelected: function () {
                return this.collection.filter(function (m) {
                    return m.get('selected');
                });
            },
            select: function () {
                this.set({ 'selected': true });
                return this.getSelected();
            },
            deselect: function () {
                this.set({
                    'selected': false,
                    'enhanced': false
                });
            },
            showDetails: function () {
                var selected = this.getSelected();

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
            onEnter: function (el) {
                var link = d3.select(el),
                    th = ts.theme(),
                    delay = 1000;

                this.el = link;

                link.transition()
                    .duration(delay)
                    .attr('stroke', linkConfig[th].baseColor);

                this.setVisibility();
                this.setScale();
            },
            linkWidth: function () {
                var width = widthRatio;
                if (this.get('enhanced')) { width = 3.5; }
                if (this.linkLabel) {
                    var scale = d3.scale.ordinal()
                            .rangeRoundPoints([4, 8]),
                        label = this.linkLabel.get('label').split(' ');

                    switch (t2ts.selectedTrafficOverlay()) {
                        case 'flowStatsBytes':
                            scale.domain(['KB', 'MB', 'GB']);
                            width = scale(label[1]);
                            break;
                        case 'portStatsBitSec':
                            scale.domain(['Kbps', 'Mbps', 'Gbps']);
                            width = scale(label[1]);
                            break;
                        case 'portStatsPktSec':
                            scale = d3.scale.linear()
                                .domain([1, 10, 100, 1000, 10000])
                                .range(d3.range(3.5, 9))
                                .clamp(true);
                            width = scale(parseInt(label[0]));
                    }
                }

                return width;
            },
            setScale: function () {

                if (!this.el) return;

                var linkWidthRatio = this.linkWidth();

                var width = linkScale(linkWidthRatio) / t2zs.scale();
                this.el.attr('stroke-width', width + 'px');

                var labelScale = labelDim / (labelDim * t2zs.scale());

                d3.select('.topo2-portLabels')
                    .selectAll('.portLabel')
                    .selectAll('*')
                    .style('transform', 'scale(' + labelScale + ')');

                this.setPosition();

                if (this.linkLabel) {
                    this.linkLabel.setScale();
                }
            },
            update: function () {
                if (this.get('enhanced')) {
                    this.enhance();
                }
            },
            setVisibility: function () {

                if (this.get('type') !== 'UiEdgeLink') {
                    return;
                }

                var visible = ps.getPrefs('topo2_prefs')['hosts'];
                this.el.style('visibility', visible ? 'visible' : 'hidden');
            },
            displayMastership: function () {
                this.set({ mastership: t2mss.mastership() !== null});
            },
            remove: function () {

                var width = linkScale(widthRatio) / t2zs.scale();

                this.el.transition()
                    .duration(300)
                    .attr('stroke', '#ff0000')
                    .style('stroke-width', width * 4)
                    .transition()
                    .delay(1000)
                    .style('opacity', 0);
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
    .factory('Topo2LinkService', [
        '$log', 'Topo2Collection', 'Topo2Model',
        'ThemeService', 'SvgUtilService', 'Topo2ZoomService',
        'Topo2ViewService', 'Topo2LinkPanelService', 'FnService', 'PrefsService',
        'Topo2MastershipService', 'Topo2TrafficService',
        function (_$log_, _c_, _Model_, _ts_, _sus_,
            _t2zs_, _t2vs_, _t2lps_, _fn_, _ps_, _t2mss_, _t2ts_) {

            $log = _$log_;
            ts = _ts_;
            sus = _sus_;
            t2zs = _t2zs_;
            t2vs = _t2vs_;
            Collection = _c_;
            Model = _Model_;
            t2lps = _t2lps_;
            fn = _fn_;
            ps = _ps_;
            t2mss = _t2mss_;
            t2ts = _t2ts_;

            return {
                createLinkCollection: createLinkCollection
            };
        }
    ]);
})();
