# Redis GEO: a sorted set scored by a 52-bit geohash; radius queries scan the covering cells for you.
GEOADD places:blr 77.5946 12.9716 "cafe:1" 77.6101 12.9752 "cafe:2" 77.6245 12.9352 "cafe:3"

# Everything within 2 km of a point, nearest first, with distances
GEOSEARCH places:blr FROMLONLAT 77.5950 12.9720 BYRADIUS 2 km ASC WITHDIST COUNT 20

# Box search for a map viewport
GEOSEARCH places:blr FROMLONLAT 77.60 12.96 BYBOX 6 6 km ASC

GEOHASH places:blr "cafe:1"          # the member's 11-character geohash
GEODIST places:blr "cafe:1" "cafe:3" km
