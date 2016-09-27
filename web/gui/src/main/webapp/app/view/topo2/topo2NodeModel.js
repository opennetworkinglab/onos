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
 ONOS GUI -- Topology Layout Module.
 Module that contains the d3.force.layout logic
 */

(function () {
    'use strict';

    var randomService, ps, sus, is, ts, t2mcs;
    var fn;

    // Internal state;
    var nearDist = 15;

    var devIconDim = 36,
        labelPad = 5,
        textPad = 5,
        halfDevIcon = devIconDim / 2;

    // note: these are the device icon colors without affinity (no master)
    var dColTheme = {
        light: {
            online: '#444444',
            offline: '#cccccc'
        },
        dark: {
            // TODO: theme
            online: '#444444',
            offline: '#cccccc'
        }
    };

    function devGlyphColor(d) {
        var o = this.get('online'),
            id = this.get('master'),
            otag = o ? 'online' : 'offline';
        return o ? sus.cat7().getColor(id, 0, ts.theme()) :
            dColTheme[ts.theme()][otag];
    }

    function positionNode(node, forUpdate) {
        var meta = node.get('metaUi'),
            x = meta && meta.x,
            y = meta && meta.y,
            dim = [800, 600],
            xy;

        // If the device contains explicit LONG/LAT data, use that to position
        if (setLongLat(node)) {
            return true;
        }

        // else if we have [x,y] cached in meta data, use that...
        if (x !== undefined && y !== undefined) {
            node.fixed = true;
            node.px = node.x = x;
            node.py = node.y = y;
            return;
        }

        // if this is a node update (not a node add).. skip randomizer
        if (forUpdate) {
            return;
        }

        // Note: Placing incoming unpinned nodes at exactly the same point
        //        (center of the view) causes them to explode outwards when
        //        the force layout kicks in. So, we spread them out a bit
        //        initially, to provide a more serene layout convergence.
        //       Additionally, if the node is a host, we place it near
        //        the device it is connected to.

        function rand() {
            return {
                x: randomService.randDim(dim[0]),
                y: randomService.randDim(dim[1])
            };
        }

        function near(node) {
            return {
                x: node.x + nearDist + randomService.spread(nearDist),
                y: node.y + nearDist + randomService.spread(nearDist)
            };
        }

        function getDevice(cp) {
            return rand();
        }

        xy = (node.class === 'host') ? near(getDevice(node.cp)) : rand();

        if (node.class === 'sub-region') {
            xy = rand();
            node.x = node.px = xy.x;
            node.y = node.py = xy.y;
        }
        angular.extend(node, xy);
    }

    function setLongLat(node) {
        var loc = node.location,
            coord;

        if (loc && loc.type === 'lnglat') {
            coord = coordFromLngLat(loc);
            node.fixed = true;
            node.px = node.x = coord[0];
            node.py = node.y = coord[1];
            return true;
        }
    }

    function coordFromLngLat(loc) {
        var p = t2mcs.projection();
        return p ? p.invert([loc.lng, loc.lat]) : [0, 0];
    }

    angular.module('ovTopo2')
    .factory('Topo2NodeModel',
        ['Topo2Model', 'FnService', 'RandomService', 'Topo2PrefsService',
        'SvgUtilService', 'IconService', 'ThemeService', 'Topo2MapConfigService',
        function (Model, _fn_, _RandomService_, _ps_, _sus_, _is_, _ts_, _t2mcs_) {

            randomService = _RandomService_;
            ts = _ts_;
            fn = _fn_;
            ps = _ps_;
            sus = _sus_;
            is = _is_;
            t2mcs = _t2mcs_;

            return Model.extend({
                initialize: function () {
                    this.node = this.createNode();
                },
                createNode: function () {
                    this.set('class', this.nodeType);
                    this.set('svgClass', this.svgClassName());
                    positionNode(this);
                    return this;
                },
                setUpEvents: function () {
                    var _this = this;
                    angular.forEach(this.events, function (handler, key) {
                        _this.el.on(key, _this[handler].bind(_this));
                    });
                },
                icon: function () {
                    return 'unknown';
                },
                label: function () {
                    var props = this.get('props'),
                        id = this.get('id'),
                        friendlyName = props ? props.name : id,
                        labels = ['', friendlyName, id],
                        nli = ps.get('dlbls'),
                        idx = (nli < labels.length) ? nli : 0;

                    return labels[idx];
                },
                trimLabel: function (label) {
                    return (label && label.trim()) || '';
                },
                computeLabelWidth: function (el) {
                    var text = el.select('text'),
                        box = text.node().getBBox();
                    return box.width + labelPad * 2;
                },
                addLabelElements: function (label) {
                    var rect = this.el.append('rect');
                    var glythRect = this.el.append('rect')
                        .attr('y', -halfDevIcon)
                        .attr('x', -halfDevIcon)
                        .attr('width', devIconDim)
                        .attr('height', devIconDim)
                        .style('fill', devGlyphColor.bind(this));

                    var text = this.el.append('text').text(label)
                        .attr('text-anchor', 'left')
                        .attr('y', '0.3em')
                        .attr('x', halfDevIcon + labelPad + textPad);

                    return {
                        rect: rect,
                        glythRect: glythRect,
                        text: text
                    };
                },
                labelBox: function (dim, labelWidth) {
                    var _textPad = (textPad * 2) - labelPad;

                    if (labelWidth === 0) {
                        _textPad = 0;
                    }

                    return {
                        x: -dim / 2 - labelPad,
                        y: -dim / 2 - labelPad,
                        width: dim + labelWidth + (labelPad * 2) + _textPad,
                        height: dim + (labelPad * 2)
                    };
                },
                iconBox: function (dim, labelWidth) {
                    return {
                        x: -dim / 2,
                        y: -dim / 2,
                        width: dim + labelWidth,
                        height: dim
                    };
                },
                svgClassName: function () {
                    return fn.classNames('node',
                        this.nodeType,
                        this.get('type'),
                        {
                            online: this.get('online')
                        }
                    );
                },
                lngLatFromCoord: function (coord) {
                    var p = t2mcs.projection();
                    return p ? p.invert(coord) : [0, 0];
                },
                update: function () {
                    this.updateLabel();
                },
                updateLabel: function () {
                    var node = this.el,
                        label = this.trimLabel(this.label()),
                        labelWidth;

                    node.select('text').text(label);
                    labelWidth = label ? this.computeLabelWidth(node) : 0;

                    node.select('rect')
                        .transition()
                        .attr(this.labelBox(devIconDim, labelWidth));
                },
                onEnter: function (el) {
                    this.el = d3.select(el);
                    this.render();
                },
                render: function () {
                    var node = this.el,
                        glyphId = this.icon(this.get('type')),
                        label = this.trimLabel(this.label()),
                        glyph, labelWidth;

                    // Label
                    var labelElements = this.addLabelElements(label);
                    labelWidth = label ? this.computeLabelWidth(node) : 0;
                    labelElements.rect.attr(this.labelBox(devIconDim, labelWidth));

                    // Icon
                    glyph = is.addDeviceIcon(node, glyphId, devIconDim);
                    glyph.attr(this.iconBox(devIconDim, 0));
                    glyph.style('fill', 'white');

                    node.attr('transform', sus.translate(-halfDevIcon, -halfDevIcon));

                    if (this.events) {
                        this.setUpEvents();
                    }
                }
            });
        }]
    );
})();
