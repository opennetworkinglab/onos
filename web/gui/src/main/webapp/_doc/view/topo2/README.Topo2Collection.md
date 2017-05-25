ONOS Web UI - Topo2Collection.js Documentation
====================================

Topo2Collection is a Class that contains a Collection of Models.

#Usage
```Javacsript
// Empty Collection
var collection = new Collection();

// Create a Collection with data
var collection = new Collection([]: JsonArray)
```

#Exposed methods
## initialize()
Is called after the Object Creation

## add(data: any)
If data is a Json Object/Array, it will convert the data to a `Model` object
If data is a `Model` it will be added with no conversion.
Note that an `id` property is required.

## remove(model: Model)
Find and remove the Model from the collection

## get(id: string)
Returns a model with the given Id.

## sort()
Require the `comparator` property to be set on the collection.
If set it will order the models in which the `comparator` determines.

## filter(comparator: function)
Returns an array of Models selected by the filter

## empty()
Clears and resets the collection

## toJSON()
Returns a JSON Array of all the models in the collection
