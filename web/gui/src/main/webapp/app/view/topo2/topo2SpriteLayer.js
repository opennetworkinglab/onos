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
 ONOS GUI -- Topology Sprite Module.
 Defines behavior for loading sprites into the sprite layer.
 */

 (function () {
    'use strict';

    var instance;

    function vbox(w, h) {
      return '0 0 ' + w + ' ' + h;
    }

    angular.module('ovTopo2')
        .factory('Topo2SpriteLayerService', [
            '$log', 'FnService',
            'Topo2ViewController', 'SpriteService', 'ThemeService',

            function ($log, fs, ViewController, ss, ts) {

                var SpriteLayer = ViewController.extend({

                    id: 'topo2-sprites',
                    displayName: 'Sprite Layer',

                    init: function () {
                        this.svg = d3.select('#topo2');
                        this.createSpriteDefs();
                        this.container = this.appendElement('#topo2-background', 'g');
                    },
                    loadLayout: function (id) {
                        var _this = this;
                        this.container.selectAll('*').remove();
                        this.layout = ss.layout(id);

                        if (this.layout) {
                            this.width = this.layout.data.w;
                            this.height = this.layout.data.h;

                            this.renderLayout();

                            if (fs.debugOn('sprite_grid')) {
                                this.renderGrid();
                            }
                        } else {
                            $log.warn('no sprite layout registered:', id);
                        }

                        // Returns a promise for consistency with Topo2MapService
                        return new Promise(function (resolve) {
                            resolve(_this);
                        });
                    },
                    createSpriteDefs: function () {
                       this.defs = this.svg.append('defs')
                            .attr('id', 'sprites');
                    },
                    getWidth: function () {
                        return this.width;
                    },
                    getHeight: function () {
                        return this.height;
                    },
                    renderSprite: function (spriteData) {

                        var id = spriteData.sprite.data.id,
                            definition = d3.select('#' + id);
                            // is '#<id>' sufficient? namespace? '#t2spr-<id>' ?

                        if (definition.empty()) {

                            var data = spriteData.sprite.data,
                                spriteEl = this.defs.append('symbol')
                                    .attr('viewBox', vbox(data.w, data.h))
                                    .attr('id', id);

                            // TODO: add elements to sprite in the order defined
                            //   when the sprite was created (layered bottom up)
                            //   e.g. (1) rect, (2) path, (3) rect...
                            //   not in groups by type.
                            //   For now, assume rectangles are behind paths...

                            // draw rectangles before paths
                            _.each(spriteData.sprite.rects, function (rect) {
                                spriteEl.append('rect')
                                    .attr('width', rect.w)
                                    .attr('height', rect.h)
                                    .attr('x', rect.x)
                                    .attr('y', rect.y)
                                    .style('fill', ts.spriteColor(rect.o.fill, 'fill'))
                                    .style('stroke', ts.spriteColor(rect.o.stroke, 'stroke'));
                            });

                            // draw paths after rectangles
                            _.each(spriteData.sprite.paths, function (path) {
                                spriteEl.append('path')
                                    .attr('d', path.d)
                                    .style('fill', ts.spriteColor(path.o.fill, 'fill'))
                                    .style('stroke', ts.spriteColor(path.o.stroke, 'stroke'));
                            });
                        }

                        return spriteEl;
                    },
                    renderLayout: function () {

                        var _this = this;
                        var layout = this.container.append('svg')
                            .attr('class', layout)
                            .attr('viewBox', vbox(this.layout.data.w, this.layout.data.h));

                        _.each(this.layout.sprites, function (spriteData) {
                            _this.renderSprite(spriteData);

                            layout
                                .append('g')
                                .append('use')
                                .attr('xlink:href', '#rack')
                                .attr('width', 20)
                                .attr('height', 25)
                                .attr('x', spriteData.x)
                                .attr('y', spriteData.y);
                        });
                    },
                    renderGrid: function () {

                        var layout = this.container.select('svg').append('g');

                        var gridSpacing = 5,
                            x = this.width / gridSpacing,
                            y = this.height / gridSpacing,
                            i, l;

                        for (i = 0, l = x; i < l; i++) {
                            layout.append('rect')
                                .attr('width', 0.1)
                                .attr('height', this.height)
                                .attr('x', i * gridSpacing)
                                .attr('y', 0);
                        }

                        for (i = 0, l = y; i < l; i++) {
                            layout.append('rect')
                                .attr('height', 0.1)
                                .attr('width', this.width)
                                .attr('y', i * gridSpacing)
                                .attr('x', 0);
                        }
                    },
                });

                function getInstance() {
                    return instance || new SpriteLayer();
                }

                return getInstance();
            },
        ]);
 })();
