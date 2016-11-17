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
 ONOS GUI -- Topology Node Module.
 Module that contains model for nodes within the topology
 */

(function () {
    'use strict';

    var ps, sus, is, ts, t2mcs, t2nps, fn;

    var devIconDim = 36,
        devIconDimMin = 20,
        devIconDimMax = 40,
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

    angular.module('ovTopo2')
    .factory('Topo2NodeModel',
        ['Topo2Model', 'FnService', 'Topo2PrefsService',
        'SvgUtilService', 'IconService', 'ThemeService',
        'Topo2MapConfigService', 'Topo2ZoomService', 'Topo2NodePositionService',
        function (Model, _fn_, _ps_, _sus_, _is_, _ts_,
            _t2mcs_, zoomService, _t2nps_) {

            ts = _ts_;
            fn = _fn_;
            ps = _ps_;
            sus = _sus_;
            is = _is_;
            t2mcs = _t2mcs_;
            t2nps = _t2nps_;

            return Model.extend({
                initialize: function () {
                    this.node = this.createNode();
                    this._events = {
                        'mouseover': 'mouseoverHandler',
                        'mouseout': 'mouseoutHandler'
                    };
                },
                select: function () {
                    var ev = d3.event;

                    // TODO: if single selection clear selected devices, hosts, sub-regions

                    if (ev.shiftKey) {
                        // TODO: Multi-Select Details Panel
                        this.set('selected', true);
                    } else {

                        var s = Boolean(this.get('selected'));
                        // Clear all selected Items
                        _.each(this.collection.models, function (m) {
                            m.set('selected', false);
                        });

                        this.set('selected', !s);
                    }

                    var selected = this.collection.filter(function (m) {
                        return m.get('selected');
                    });

                    return selected;
                },
                createNode: function () {
                    this.set('svgClass', this.svgClassName());
                    t2nps.positionNode(this);
                    return this;
                },
                setUpEvents: function () {
                    var _this = this,
                        events = angular.extend({}, this._events, this.events);
                    angular.forEach(events, function (handler, key) {
                        _this.el.on(key, _this[handler].bind(_this));
                    });
                },
                mouseoverHandler: function () {
                    this.set('hovered', true);
                },
                mouseoutHandler: function () {
                    this.set('hovered', false);
                },
                icon: function () {
                    return 'unknown';
                },
                labelIndex: function () {
                    return ps.get('dlbls');
                },
                label: function () {
                    var props = this.get('props'),
                        id = this.get('id'),
                        friendlyName = props && props.name ? props.name : id,
                        labels = ['', friendlyName || id, id],
                        nli = this.labelIndex(),
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
                    var rect = this.el.append('rect')
                        .attr('class', 'node-container');
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
                            online: this.get('online'),
                            selected: this.get('selected')
                        }
                    );
                },
                lngLatFromCoord: function (coord) {
                    var p = t2mcs.projection();
                    return p ? p.invert(coord) : [0, 0];
                },
                resetPosition: function () {
                    t2nps.setLongLat(this);
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
                setScale: function () {

                    var dim = devIconDim,
                        multipler = 1;

                    if (dim * zoomService.scale() < devIconDimMin) {
                        multipler = devIconDimMin / (dim * zoomService.scale());
                    } else if (dim * zoomService.scale() > devIconDimMax) {
                        multipler = devIconDimMax / (dim * zoomService.scale());
                    }

                    this.el.selectAll('*')
                        .style('transform', 'scale(' + multipler + ')');
                },
                render: function () {
                    var node = this.el,
                        glyphId = this.icon(this.get('type')),
                        label = this.trimLabel(this.label()),
                        glyph, labelWidth;

                    // Label
                    var labelElements = this.addLabelElements(label);
                    labelWidth = label ? this.computeLabelWidth(node) : 0;
                    labelElements.rect
                        .attr(this.labelBox(devIconDim, labelWidth));

                    // Icon
                    glyph = is.addDeviceIcon(node, glyphId, devIconDim);
                    glyph.attr(this.iconBox(devIconDim, 0));
                    glyph.style('fill', 'white');

                    node.attr('transform',
                        sus.translate(-halfDevIcon, -halfDevIcon));

                    if (this.events) {
                        this.setUpEvents();
                    }

                    this.setScale();
                }
            });
        }]
    );
})();
