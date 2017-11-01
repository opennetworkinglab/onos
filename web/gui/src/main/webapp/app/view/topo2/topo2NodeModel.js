/*
 * Copyright 2016-present Open Networking Foundation
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

    var t2ps, sus, is, ts, t2mcs, t2nps, fn;

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
                offline: '#cccccc',
            },
            dark: {
                // TODO: theme
                online: '#444444',
                offline: '#cccccc',
            },
        },
        // and here are the stroke colors of the glyph, per theme
        dUseTheme = {
            light: 'white',
            dark: 'black',
        };

    angular.module('ovTopo2')
    .factory('Topo2NodeModel', [
        'Topo2Model', 'FnService', 'Topo2PrefsService',
        'SvgUtilService', 'IconService', 'ThemeService',
        'Topo2MapConfigService', 'Topo2ZoomService', 'Topo2NodePositionService',
        'Topo2SelectService', 'Topo2MastershipService',
        function (Model, _fn_, _t2ps_, _sus_, _is_, _ts_,
            _t2mcs_, zoomService, _t2nps_, t2ss, t2mss) {

            ts = _ts_;
            fn = _fn_;
            t2ps = _t2ps_;
            sus = _sus_;
            is = _is_;
            t2mcs = _t2mcs_;
            t2nps = _t2nps_;

            return Model.extend({
                initialize: function () {
                    this.node = this.createNode();
                    this.mastershipService = t2mss;
                    this._events = {
                        'mouseover': 'mouseoverHandler',
                        'mouseout': 'mouseoutHandler',
                    };
                },
                select: function () {
                    this.set('selected', true);
                },
                index: function () {

                    var models = this.collection.models,
                        id = this.get('id');

                    var index = _.find(models, function (model, i) {
                        return model.get('id') === id;
                    });

                    return index || models.length;
                },
                deselect: function () {
                    this.set('selected', false);
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
                onClick: function () {
                    if (d3.event.defaultPrevented) return;

                    d3.event.preventDefault();
                    t2ss.selectObject(this, this.multiSelectEnabled);
                },
                fix: function (fixed) {
                    this.set({ fixed: fixed });
                    this.fixed = fixed;
                },
                icon: function () {
                    return 'unknown';
                },
                labelIndex: function () {
                    return t2ps.get('dlbls');
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
                devGlyphColor: function () {
                    var o = this.get('online'),
                        id = this.get('master'),
                        otag = o ? 'online' : 'offline';
                    return o ? sus.cat7().getColor(id, 0, ts.theme()) :
                        dColTheme[ts.theme()][otag];
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
                        height: dim + (labelPad * 2),
                    };
                },
                iconBox: function (dim, labelWidth) {
                    return {
                        x: -dim / 2,
                        y: -dim / 2,
                        width: dim + labelWidth,
                        height: dim,
                    };
                },
                svgClassName: function () {
                    return fn.classNames('node',
                        this.nodeType,
                        this.get('type'),
                        {
                            online: this.get('online'),
                            selected: this.get('selected'),
                            hovered: this.get('hovered'),
                            fixed: this.get('fixed'),
                            suppressedmax: this.get('mastership'),
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
                displayMastership: function () {
                    this.set({ mastership: t2mss.mastership() !== null });
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

                    if (this.setOfflineVisibility) {
                        this.setOfflineVisibility();
                    }
                },
                setScale: function () {

                    if (!this.el) return;

                    var dim = devIconDim,
                        multipler = 1;

                    if (dim * zoomService.scale() < devIconDimMin) {
                        multipler = devIconDimMin / (dim * zoomService.scale());
                    } else if (dim * zoomService.scale() > devIconDimMax) {
                        multipler = devIconDimMax / (dim * zoomService.scale());
                    }

                    this.el.select('.node-content')
                        .style('transform', 'scale(' + multipler + ')');
                },
                addLabelElements: function (label) {

                    var labelG = this.el.select('.node-content')
                        .append('g')
                        .attr('class', 'label');

                    var rect = labelG.append('rect')
                        .attr('class', 'node-container');

                    var text = labelG.append('text').text(label)
                        .attr('text-anchor', 'left')
                        .attr('y', '0.3em')
                        .attr('x', halfDevIcon + labelPad + textPad);

                    return {
                        rect: rect,
                        text: text,
                    };
                },
                addIconElements: function (el) {

                    var glyphId = this.icon(this.get('type')),
                        glyph;

                    var iconG = el.append('g')
                        .attr('class', 'icon');

                    iconG.append('rect')
                        .attr('class', 'icon-rect')
                        .attr('y', -halfDevIcon)
                        .attr('x', -halfDevIcon)
                        .attr('width', devIconDim)
                        .attr('height', devIconDim)
                        .style('fill', this.devGlyphColor.bind(this));

                    // Icon
                    glyph = is.addDeviceIcon(iconG, glyphId, devIconDim);
                    glyph.attr(this.iconBox(devIconDim, 0));
                    glyph.style('fill', dUseTheme[ts.theme()]);
                },
                render: function () {
                    var node = this.el,
                        label = this.trimLabel(this.label()),
                        labelWidth;

                    var nodeG = node.append('g')
                        .attr('class', 'node-content');

                    // Label
                    var labelElements = this.addLabelElements(label);
                    labelWidth = label ? this.computeLabelWidth(node) : 0;
                    labelElements.rect
                        .attr(this.labelBox(devIconDim, labelWidth));

                    this.addIconElements(nodeG);

                    node.attr('transform',
                        sus.translate(-halfDevIcon, -halfDevIcon));

                    if (this.events) {
                        this.setUpEvents();
                    }

                    this.setScale();
                },

                // Override Methods
                setOfflineVisibility: function () {},
            });
        }]
    );
})();
