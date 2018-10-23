/*
 * Copyright 2018-present Open Networking Foundation
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
import {Inject, Injectable} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {LogService} from '../log.service';
import {Trie, TrieOp} from './trie';

// Angular>=2 workaround for missing definition
declare const InstallTrigger: any;

const matcher = /<\/?([a-zA-Z0-9]+)*(.*?)\/?>/igm;
const whitelist: string[] = ['b', 'i', 'p', 'em', 'strong', 'br'];
const evillist: string[] = ['script', 'style', 'iframe'];

/**
 * Used with the Window size function;
 **/
export interface WindowSize {
    width: number;
    height: number;
}

/**
 * For the sanitize() and analyze() functions
 */
export interface Match {
    full: string;
    name: string;
}

/**
 * ONOS GUI -- Util -- General Purpose Functions
 */
@Injectable({
  providedIn: 'root',
})
export class FnService {
    // internal state
    private debugFlags = new Map<string, boolean>([
//        [ "LoadingService", true ]
    ]);

    constructor(
        private route: ActivatedRoute,
        private log: LogService,
        // TODO: Change the any type to Window when https://github.com/angular/angular/issues/15640 is fixed.
        @Inject('Window') private w: any
    ) {
        this.route.queryParams.subscribe(params => {
            const debugparam: string = params['debug'];
//            log.debug('Param:', debugparam);
            this.parseDebugFlags(debugparam);
        });
//        this.log.debug('FnService constructed');
    }

    /**
     * Test if an argument is a function
     *
     * Note: the need for this would go away if all functions
     * were strongly typed
     */
    isF(f: any): any {
        return typeof f === 'function' ? f : null;
    }

    /**
     * Test if an argument is an array
     *
     * Note: the need for this would go away if all arrays
     * were strongly typed
     */
    isA(a: any): any {
    // NOTE: Array.isArray() is part of EMCAScript 5.1
        return Array.isArray(a) ? a : null;
    }

    /**
     * Test if an argument is a string
     *
     * Note: the need for this would go away if all strings
     * were strongly typed
     */
    isS(s: any): string {
        return typeof s === 'string' ? s : null;
    }

    /**
     * Test if an argument is an object
     *
     * Note: the need for this would go away if all objects
     * were strongly typed
     */
    isO(o: any): Object {
        return (o && typeof o === 'object' && o.constructor === Object) ? o : null;
    }

    /**
     * Test that an array contains an object
     */
    contains(a: any[], x: any): boolean {
        return this.isA(a) && a.indexOf(x) > -1;
    }

    /**
     * Returns width and height of window inner dimensions.
     * offH, offW : offset width/height are subtracted, if present
     */
    windowSize(offH: number = 0, offW: number = 0): WindowSize {
        return {
            height: this.w.innerHeight - offH,
            width: this.w.innerWidth - offW
        };
    }

    /**
     * Returns true if all names in the array are defined as functions
     * on the given api object; false otherwise.
     * Also returns false if there are properties on the api that are NOT
     * listed in the array of names.
     *
     * This gets extra complicated when the api Object is an
     * Angular service - while the functions can be retrieved
     * by an indexed get, the ownProperties does not show the
     * functions of the class. We have to dive in to the prototypes
     * properties to get these - and even then we have to filter
     * out the constructor and any member variables
     */
    areFunctions(api: Object, fnNames: string[]): boolean {
        const fnLookup: Map<string, boolean> = new Map();
        let extraFound: boolean = false;

        if (!this.isA(fnNames)) {
            return false;
        }

        const n: number = fnNames.length;
        let i: number;
        let name: string;

        for (i = 0; i < n; i++) {
            name = fnNames[i];
            if (!this.isF(api[name])) {
                return false;
            }
            fnLookup.set(name, true);
        }

        // check for properties on the API that are not listed in the array,
        const keys = Object.getOwnPropertyNames(api);
        if (keys.length === 0) {
            return true;
        }
        // If the api is a class it will have a name,
        //  else it will just be called 'Object'
        const apiObjectName: string = api.constructor.name;
        if (apiObjectName === 'Object') {
            Object.keys(api).forEach((key) => {
                if (!fnLookup.get(key)) {
                    extraFound = true;
                }
            });
        } else { // It is a class, so its functions will be in the child (prototype)
            const pObj: Object = Object.getPrototypeOf(api);
            for ( const key in Object.getOwnPropertyDescriptors(pObj) ) {
                if (key === 'constructor') { // Filter out constructor
                    continue;
                }
                const value = Object.getOwnPropertyDescriptor(pObj, key);
                // Only compare functions. Look for any not given in the map
                if (this.isF(value.value) && !fnLookup.get(key)) {
                    extraFound = true;
                }
            }
        }
        return !extraFound;
    }

    /**
     * Returns true if all names in the array are defined as functions
     * on the given api object; false otherwise. This is a non-strict version
     * that does not care about other properties on the api.
     */
    areFunctionsNonStrict(api, fnNames): boolean {
        if (!this.isA(fnNames)) {
            return false;
        }
        const n = fnNames.length;
        let i;
        let name;

        for (i = 0; i < n; i++) {
            name = fnNames[i];
            if (!this.isF(api[name])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if current browser determined to be a mobile device
     */
    isMobile() {
        const ua = this.w.navigator.userAgent;
        const patt = /iPhone|iPod|iPad|Silk|Android|BlackBerry|Opera Mini|IEMobile/;
        return patt.test(ua);
    }

    /**
     * Returns true if the current browser determined to be Chrome
     */
    isChrome() {
        const isChromium = (this.w as any).chrome;
        const vendorName = this.w.navigator.vendor;

        const isOpera = this.w.navigator.userAgent.indexOf('OPR') > -1;
        return (isChromium !== null &&
        isChromium !== undefined &&
        vendorName === 'Google Inc.' &&
        isOpera === false);
    }

    isChromeHeadless() {
        const vendorName = this.w.navigator.vendor;
        const headlessChrome = this.w.navigator.userAgent.indexOf('HeadlessChrome') > -1;

        return (vendorName === 'Google Inc.' && headlessChrome === true);
    }

    /**
     * Returns true if the current browser determined to be Safari
     */
    isSafari() {
        return (this.w.navigator.userAgent.indexOf('Safari') !== -1 &&
        this.w.navigator.userAgent.indexOf('Chrome') === -1);
    }

    /**
     * Returns true if the current browser determined to be Firefox
     */
    isFirefox() {
        return typeof InstallTrigger !== 'undefined';
    }

    /**
     * search through an array of objects, looking for the one with the
     * tagged property matching the given key. tag defaults to 'id'.
     * returns the index of the matching object, or -1 for no match.
     */
    find(key: string, array: Object[], tag: string = 'id'): number {
        let idx: number;
        const n: number = array.length;

        for (idx = 0 ; idx < n; idx++) {
            const d: Object = array[idx];
            if (d[tag] === key) {
                return idx;
            }
        }
        return -1;
    }

    /**
     * search through array to find (the first occurrence of) item,
     * returning its index if found; otherwise returning -1.
     */
    inArray(item: any, array: any[]): number {
        if (this.isA(array)) {
            for (let i = 0; i < array.length; i++) {
                if (array[i] === item) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * remove (the first occurrence of) the specified item from the given
     * array, if any. Return true if the removal was made; false otherwise.
     */
    removeFromArray(item: any, array: any[]): boolean {
        const i: number = this.inArray(item, array);
        if (i >= 0) {
            array.splice(i, 1);
            return true;
        }
        return false;
    }

    /**
     * return true if the object is empty, return false otherwise
     */
    isEmptyObject(obj: Object): boolean {
        for (const key in obj) {
            if (true) { return false; }
        }
        return true;
    }

    /**
     * returns true if the two objects have all the same properties
     */
    sameObjProps(obj1: Object, obj2: Object): boolean {
        for (const key in obj1) {
            if (obj1.hasOwnProperty(key)) {
                if (!(obj1[key] === obj2[key])) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * returns true if the array contains the object
     * does NOT use strict object reference equality,
     * instead checks each property individually for equality
     */
    containsObj(arr: any[], obj: Object): boolean {
        const len = arr.length;
        for (let i = 0; i < len; i++) {
            if (this.sameObjProps(arr[i], obj)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the given string with the first character capitalized.
     */
    cap(s: string): string {
        return s ? s[0].toUpperCase() + s.slice(1).toLowerCase() : s;
    }

    /**
     * return the parameter without a px suffix
     */
    noPx(num: string): number {
        return Number(num.replace(/px$/, ''));
    }

    /**
     * return an element's given style property without px suffix
     */
    noPxStyle(elem: any, prop: string): number {
        return Number(elem.style(prop).replace(/px$/, ''));
    }

    /**
     * Return true if a str ends with suffix
     */
    endsWith(str: string, suffix: string) {
        return str.indexOf(suffix, str.length - suffix.length) !== -1;
    }

    /**
     * output debug message to console, if debug tag set...
     * e.g. fs.debug('mytag', arg1, arg2, ...)
     */
    debug(tag, ...args) {
        if (this.debugFlags.get(tag)) {
//            this.log.debug(tag, args.join());
        }
    }

    private parseDebugFlags(dbgstr: string): void {
        const bits = dbgstr ? dbgstr.split(',') : [];
        bits.forEach((key) => {
            this.debugFlags.set(key, true);
        });
//        this.log.debug('Debug flags:', dbgstr);
    }

    /**
      * Return true if the given debug flag was specified in the query params
      */
    debugOn(tag: string): boolean {
        return this.debugFlags.get(tag);
    }



    // -----------------------------------------------------------------
    // The next section deals with sanitizing external strings destined
    // to be loaded via a .html() function call.
    //
    // See definition of matcher, evillist and whitelist at the top of this file

    /*
     * Returns true if the tag is in the evil list, (and is not an end-tag)
     */
    inEvilList(tag: any): boolean {
        return (evillist.indexOf(tag.name) !== -1 && tag.full.indexOf('/') === -1);
    }

    /*
     * Returns an array of Matches of matcher in html
     */
    analyze(html: string): Match[] {
        const matches: Match[] = [];
        let match;

        // extract all tags
        while ((match = matcher.exec(html)) !== null) {
            matches.push({
                full: match[0],
                name: match[1],
                // NOTE: ignoring attributes {match[2].split(' ')} for now
            });
        }

        return matches;
    }

    /*
     * Returns a cleaned version of html
     */
    sanitize(html: string): string {
        const matches: Match[] = this.analyze(html);

        // completely obliterate evil tags and their contents...
        evillist.forEach((tag) => {
            const re = new RegExp('<' + tag + '(.*?)>(.*?[\r\n])*?(.*?)(.*?[\r\n])*?<\/' + tag + '>', 'gim');
            html = html.replace(re, '');
        });

        // filter out all but white-listed tags and end-tags
        matches.forEach((tag) => {
            if (whitelist.indexOf(tag.name) === -1) {
                html = html.replace(tag.full, '');
                if (this.inEvilList(tag)) {
                    this.log.warn('Unsanitary HTML input -- ' +
                        tag.full + ' detected!');
                }
            }
        });

        // TODO: consider encoding HTML entities, e.g. '&' -> '&amp;'

        return html;
    }

    /**
     * add word to trie (word will be converted to uppercase)
     * data associated with the word
     * returns 'added' or 'updated'
     */
    addToTrie(trie, word, data) {
        return new Trie(TrieOp.PLUS, trie, word, data);
    }

    /**
     * remove word from trie (word will be converted to uppercase)
     * returns 'removed' or 'absent'
     */
    removeFromTrie(trie, word) {
        return new Trie(TrieOp.MINUS, trie, word);
    }

    /**
     * lookup word (converted to uppercase) in trie
     * returns:
     *    undefined if the word is not in the trie
     *    -1 for a partial match (word is a prefix to an existing word)
     *    data for the word for an exact match
     */
    trieLookup(trie, word) {
        const s = word.toUpperCase().split('');
        let p = trie;
        let n;

        while (s.length) {
            n = s.shift();
            p = p[n];
            if (!p) {
                return undefined;
            }
        }
        if (p._data) {
            return p._data;
        }
        return -1;
    }

}
