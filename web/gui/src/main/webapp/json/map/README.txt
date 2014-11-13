see: http://bost.ocks.org/mike/map/

brew install gdal
npm install -g topojson

To generate continental US map:

$ wget 'http://www.naturalearthdata.com/http//www.naturalearthdata.com/download/50m/cultural/ne_50m_admin_1_states_provinces_lakes.zip'
$ unzip ne_50m_admin_1_states_provinces_lakes.zip
$ ogr2ogr -f GeoJSON -where "sr_adm0_a3 IN ('USA')" states.json ne_50m_admin_1_states_provinces_lakes.shp

edit states.json to remove data for Hawaii and Alaska

$ topojson states.json > topology.json

