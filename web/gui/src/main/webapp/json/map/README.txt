see: http://bost.ocks.org/mike/map/

brew install gdal
npm install -g topojson

To generate continental US map:

    $ wget 'http://www.naturalearthdata.com/download/50m/cultural/ne_50m_admin_1_states_provinces_lakes.zip'
    $ unzip ne_50m_admin_1_states_provinces_lakes.zip
    $ ogr2ogr -f GeoJSON -where "sr_adm0_a3 IN ('USA')" states.json ne_50m_admin_1_states_provinces_lakes.shp

edit states.json to remove data for Hawaii and Alaska

    $ topojson states.json > topology.json


The .shp file above is incomplete (USA and part of Candada.)
So it may be that each region requires a bit of research to generate.
Ideally a source for public domain shp files can be found that covers all geographic regions.


For Canada:

    # wget 'http://www12.statcan.gc.ca/census-recensement/2011/geo/bound-limit/files-fichiers/gpr_000b11a_e.zip'
    # unzip gpr_000b11a_e.zip
    # ogr2ogr -f "GeoJSON" -s_srs EPSG:21781 -t_srs EPSG:4326 canada.json gpr_000b11a_e.shp
    # topojson --id-property CFSAUID -p name=PRNAME -p name canada.json > topology.json


This produces a very large (5MB) file and draws very slowly in Chrome.
So some additional processing is required to simplify the geometry. (It is not checked in.)

Also, the specification of object structure within the geojson is unclear.
In the US map the geojson structure is

    json.objects.states

but in the Canadian data it's

    json.objects.canada


Lastly, the projection that is used may be tailored to the region.
The preferred projection for the US is "albers" and d3 provides a "albersUSA" which can be used to
    project hawaii and alaska as well

For Canada, apparantly a "Lambert" projection (called conicConformal in d3) is preferred

see:
    https://github.com/mbostock/d3/wiki/Geo-Projections
    http://www.statcan.gc.ca/pub/92-195-x/2011001/other-autre/mapproj-projcarte/m-c-eng.htm


Summary:
- some additional work is required to fully generalize maps functionality.
- it may be worthwhile for ON.LAB to provide the topo files for key regions since producing these
    files is non-trivial




