ONOS Web UI - Topo2Breadcrumb Documentation
====================================

Topo2BreadcrumbService is used to render the current depth and location of the users view.
It also provides a way of navigating backwards through the regions

#Exposed methods
##init()
* Creates an SVG Group for the breadcrumsb
* Renders to DOM

##addBreadcrumb(crumbs: json)
Takes a json object of current breadcrumb paths and renders them to the DOM

##addLayout(_layout_: Topo2LayoutSevice)
Adds a local reference for the Topo2LayoutService

##hide()
Hides the breadcrumb element from the viewport