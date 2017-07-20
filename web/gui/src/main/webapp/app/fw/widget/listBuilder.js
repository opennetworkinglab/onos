/*
 * Copyright 2015-present Open Networking Laboratory
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
 ONOS GUI -- Widget -- List Service
 */

(function () {
    'use strict';

    function addProp(el, label, value) {
        var tr = el.append('tr'),
            lab;
        if (typeof label === 'string') {
            lab = label.replace(/_/g, ' ');
        } else {
            lab = label;
        }

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).text(txt);
        }

        addCell('label', lab + ' :');
        addCell('value', value);
    }

    function addSep(el) {
        el.append('tr').append('td').attr('colspan', 2).append('hr');
    }

    function listProps(el, data) {
        data.propOrder.forEach(function (p) {
            if (p === '-') {
                addSep(el);
            } else {
                addProp(el, p, data.props[p]);
            }
        });
    }

    angular.module('onosWidget')
    .factory('ListService', [
        function () {
            return {
                listProps: listProps
            };
        }]);
}());
