Function Service
================

### Function Descriptions

`fs.isF(f)`
* Returns the argument `f` if it is a function, `null` otherwise

`fs.isA(a)`
* Returns the argument `a` if it is an array, `null` otherwise

`fs.isS(s)`
* Returns the argument `s` if it is a string, `null` otherwise

`fs.isO(o)`
* Returns the argument `o` if it is an object, `null` otherwise

`fs.contains(a, x)`
* Returns the index of `x` if it is in the array `a`, otherwise returns `-1`

`fs.areFunctions(api, fnNames)`
* `api`: API object to check
* `fnNames`: array of function names expected
* Returns `true` if functions defined on `api` _exactly matches_ `fnNames` list

`fs.areFunctionsNonStrict(api, fnNames)`
* `api`: API object to check
* `fnNames`: array of function names expected
* Returns `true` if functions defined on `api` include _all_ `fnNames` list

`fs.windowSize(offH, offW)`
* Returns current size of browser window (in pixels)
* `offH` if specified, is subtracted from window height
* `offW` if specified, is subtracted from window width
* Return value: `{ height: h, width: w }`

`fs.isMobile()`
* Returns `true` if the current browser is determined to be a mobile device

`fs.isChrome()`
* Returns `true` if the current browser is determined to be Chrome

`fs.isSafari()`
* Returns `true` if the current browser is determined to be Safari

`fs.isFirefox()`
* Returns `true` if the current browser is determined to be Firefox

`fs.find(key, array, tag)`
* Searches through the array of objects, returning the index of the 
  first item that matches the specified key for the given tagged property
* `key`: The key (value of tagged property) to search for
* `array`: An array of objects to search
* `tag`: If not specified, defaults to `"id"`
* Returns `-1` for no match

`fs.inArray(item, array)`
* Searches through the array of items (e.g. strings) to find the first
  occurence of the specified items, returning its index in the array
* `item`: the item to search for
* `array`: the array to search
* Returns `-1` for no match

`fs.removeFromArray(item, array)`:
* Removes the first occurrence of the specified item from the given array, 
  if any.
* `item`: the item to search for and remove
* `array`: the array to search
* Returns `true` if the item was found and removed

`fs.isEmptyObject(o)`
* Returns `true` if the object `o` is empty (has no properties)

`fs.sameObjectProps(o1, o2)`
* Returns `true` if objects `o1` and `o2` have precisely the same 
  set of property keys
  
`fs.containsObj(arr, obj)`
* Returns `true` if the array contains the object
* Note that this function scans the array and uses 
  `sameObjectProps(...)` to determine the result
  
`fs.cap(s)`
* Returns the given string `s` with the first character capitalized

`fs.eecode(h, w)`
* Returns encoding structure for given parameters
  (part of Easter Egg)
  
`fs.noPx(num)`
* Returns the parameter `num` stripped of its `"px"` suffix (if any)

`fs.noPxStyle(elem, prop)`
* Returns the value of a DOM element's given style, stripped 
  of its `"px"` suffix
* `elem`: DOM element
* `prop`: Style property (e.g. "height")

`fs.endsWith(str, suffix)`
* Returns `true` if the given string `str` ends with the specified `suffix`

`fs.debugOn(tag)`
* Returns `true` if the specified debug tag is active
* Debug tags can be specified in the query string, e.g.
  * `?debug=txrx,foo`
  * will return true for `fs.debugOn('txrx')` and `fs.debugOn('foo')`
  
`fs.debug(tag, arg1, arg2, ...)`
* Outputs debug message to console, if debug `tag` is set
  * e.g. `fs.debug('foo', '1 + 2 is', 1 + 2);`
  * will output to console, only if 'foo' is set in debug query parameter
  
`addToTrie(trie, word, data)`
* Adds `word` (converted to uppercase) to the specified `trie` data structure
* `data`: the data associated with the word
* Returns `"added"` or `"updated"`

`removeFromTrie(trie, word)`
* Removes `word` (converted to uppercase) from the specified `trie` data
  structure
* Returns `"removed"` or `"absent"`

`trieLookup(trie, word)`
* Does a lookup of the given `word` (converted to uppercase), in the specified
  `trie` data structure
* Returns:
  * `undefined`: if the word is not in the trie
  * `-1`: for a partial match (word is a prefix of an existing word)
  * `data`: associated with the word, on an exact match
  
`classNames(a1, a2, ...)`
* Returns a space-delimited string of CSS class names generated from the given 
  arguments
  * strings and numbers are used as is
  * arrays are recursively flattened
  * objects use key names
* For example:
  * `var arr = ['foo', 'bar'];`
  * `var obj = {goo: 2, zoo: 3}`
  * `fs.classNames(arr, 'baz', obj)`
  * returns `"foo bar baz goo zoo"`
  
`fs.extend(protoProps, staticProps)`
* Creates and returns a "child" object that inherits from the 
  "class" that this function is defined on, extended with the given 
   prototype and static properties
* This function is not designed to be called directly, but a reference to it
  placed on an object designed for extension
* Used in the class object hierarchy for the "topo2" implementation
  _(e.g. see topo2Model.js)_