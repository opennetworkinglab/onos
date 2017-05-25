ONOS Web UI - Topo2Model.js Documentation
====================================

Topo2Collection is a Class that contains a Collection of Models.

#Usage
```Javacsript
var model = new Model();
```

#Exposed methods
## initialize()
Is called after the Object Creation

## onChange(property: string, value: any, options: object)
Method to be overridden

## get(key: string)
Returns the attribute matching the key given

## set({key: string, value: any})
Sets the model[key] value

## toJSON()
Returns a JSON Array of all the models in the collection

## remove()
Removes the model from the collection (if any) it's associated with
