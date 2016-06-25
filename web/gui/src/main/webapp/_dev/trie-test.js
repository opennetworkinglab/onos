/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 ONOS GUI -- Test code illustrating use of trie functions
 */

(function () {
    'use strict';

    // injected refs
    var $log, fs;

    // internal state
    var out,
        trie = {},
        counter = 5000;

    function write(string) {
        out.append('div').text(string);
    }

    function lookup(word) {
        var result = fs.trieLookup(trie, word),
            f = fs.isF(result),
            show = f ? '{function}' : result;


        write('------> ' + word + ' ==> ' + show);

        f && f();
    }

    function add(word, data) {
        var result = fs.addToTrie(trie, word, data);
        write('   ADD> ' + word + ' [' + data + '] ==> ' + result);
    }

    function remove(word) {
        var result = fs.removeFromTrie(trie, word);
        write('REMOVE> ' + word + ' ==> ' + result);
    }

    function func1() {
        counter++;
        write('** function call **  ' + counter);
    }

    function func2() {
        counter += 11;
        write('** alternate call **  ' + counter);
    }

    function runTests() {
        lookup('cat');

        add('cat', 101);

        lookup('ca');
        lookup('cat');
        lookup('cats');

        add('cab', 103);
        add('cog', 105);

        lookup('cut');
        lookup('cab');

        remove('cab');

        lookup('cab');
        lookup('cat');

        add('fun', func1);

        lookup('fun');
        lookup('fun');
        lookup('fun');
        lookup('cat');
        lookup('fun');

        add('fun', func2);

        lookup('fun');
        lookup('fun');
        lookup('fun');

        remove('fun');

        lookup('fun');
    }

    angular.module('trie', ['onosUtil'])
    .controller('OvTrieTest', ['$log', 'FnService',

        function (_$log_, _fs_) {
            $log = _$log_;
            fs = _fs_;
            out = d3.select('#output');

            runTests();
        }]);
}());
