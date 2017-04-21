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

---

_(to be continued)_