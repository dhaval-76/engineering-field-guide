-- Redis (6.2+): GEOADD stores members in a sorted set keyed by geohash-like scores
--   GEOADD places 72.8777 19.0760 "cafe:881"
--   GEOSEARCH places FROMLONLAT 72.8777 19.0760 BYRADIUS 2 km ASC COUNT 20 WITHDIST
-- (GEORADIUS is deprecated in favour of GEOSEARCH.)

-- PostgreSQL + PostGIS: geography type with a GiST index
CREATE TABLE places (
    id        bigint PRIMARY KEY,
    name      text NOT NULL,
    category  text NOT NULL,
    location  geography(Point, 4326) NOT NULL
);
CREATE INDEX places_location_gist ON places USING gist (location);

-- Restaurants within 2 km, nearest first
SELECT id, name, ST_Distance(location, ST_MakePoint(72.8777, 19.0760)::geography) AS metres
FROM places
WHERE category = 'restaurant'
  AND ST_DWithin(location, ST_MakePoint(72.8777, 19.0760)::geography, 2000)   -- index-assisted
ORDER BY location <-> ST_MakePoint(72.8777, 19.0760)::geography                -- KNN ordering
LIMIT 20;
