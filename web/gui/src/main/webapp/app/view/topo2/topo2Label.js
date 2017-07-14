/*
 * Copyright 2017-present Open Networking Foundation
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
 ONOS GUI -- Topo2Label
 The base class for creating a label
 */

(function () {

    // TODO: THEME
    var defaultStyles = {
        label: {
            text: {
                fill: '#000000',
            },
            rect: {
                fill: '#ffffff',
            },
        },
        icon: {
            glyph: {
                fill: '#000000',
            },
            rect: {
                fill: '#ffffff',
            },
        },
    };

    angular.module('ovTopo2')
        .factory('Topo2Label', [
            'Topo2Model', 'SvgUtilService', 'IconService', 'Topo2ZoomService',
            function (Model, sus, is, t2zs) {
                return Model.extend({
                    className: 'topo2-label',
                    _iconG: {},
                    _labelG: {},

                    initialize: function (data, node) {
                        this.parent = node;
                        t2zs.addZoomEventListener(this.setScale.bind(this));
                        this.beforeRender();
                        this.render();
                        this.afterRender();
                    },
                    onChange: function (property) {
                        if (property === 'x' || property === 'y') {
                            this._position();
                        }

                        if (property === 'label') {
                            var width = this._labelG.text.node().getBBox().width + 20,
                                height = this._labelG.text.node().getBBox().height + 10;

                            this._labelG.text.text(this.get('label'));
                            this._labelG.rect.attr({
                                width: width,
                                height: height,
                            }).style({
                                transform: sus.translate(-(width/2) + 'px', -(height/2) + 'px'),
                            });
                        }
                    },

                    setPosition: function () {},
                    setScale: function () {},

                    applyStyles: function () {
                        var styles = _.extend({}, defaultStyles, this.get('styles') || {});

                        if (this.get('label')) {
                            this._labelG.text.style(styles.label.text);
                            this._labelG.rect.style(styles.label.rect);
                        }

                        if (this.get('icon')) {
                            this._iconG.glyph.style(styles.icon.glyph);
                            this._iconG.rect.style(styles.icon.rect);
                        }
                    },
                    _position: function () {
                        this.el.style('transform', sus.translate(this.get('x') + 'px',
                            this.get('y') + 'px'));
                    },
                    renderText: function () {
                        this._labelG.el = this.content.append('g')
                            .attr('class', 'label-group');

                        this._labelG.rect = this._labelG.el.append('rect');
                        this._labelG.text = this._labelG.el.append('text')
                            .text(this.get('label'))
                            .attr('y', '0.4em')
                            .style('text-anchor', 'middle');

                        this._labelG.rect.attr({
                            width: this._labelG.text.node().getBBox().width + 20,
                            height: this._labelG.text.node().getBBox().height + 10,
                        }).style({
                            transform: sus.translate('-50%', '-50%'),
                        });
                    },
                    renderIcon: function () {
                        var bbox = this._labelG.el.node().getBBox();
                        this.iconSize = bbox.height;

                        this._iconG.el = this.content.append('g')
                            .attr('class', 'icon-group');

                        this._iconG.rect = this._iconG.el.append('rect')
                            .attr({
                                width: this.iconSize,
                                height: this.iconSize,
                            });

                        this._iconG.glyph = is.addDeviceIcon(this._iconG.el,
                            this.get('icon'), this.iconSize);


                        var iconX = (-bbox.width / 2) - this.iconSize + 'px',
                            iconY = -this.iconSize /2 + 'px';
                        this._iconG.el.style({
                            transform: sus.translate(iconX, iconY),
                        });
                    },
                    beforeRender: function () {},
                    render: function () {
                        this.el = this.parent.append('g')
                            .attr('class', 'topo2-label')
                            .style({
                                transform: 'translate(300px, 300px)',
                            });

                        this.content = this.el.append('g')
                            .attr('class', 'topo2-label__content');

                        this.renderText();

                        if (this.get('icon')) {
                            this.renderIcon();
                        }

                        this.applyStyles();
                        this.setPosition();
                        this.setScale();
                    },
                    afterRender: function () {},
                    remove: function () {
                        this.el.remove();
                    },
                });
            },
        ]);
})();
