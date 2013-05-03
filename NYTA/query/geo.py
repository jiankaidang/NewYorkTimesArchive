import urllib
import urllib2
import time

from django.utils import simplejson
from django.utils.encoding import smart_str

pwd = ""
location_index = open(pwd + "location_index.txt")
location_geo = open(pwd + "location_geo.txt", "a")
location_geo_new = open(pwd + "location_index.txt", "w")
geo = location_index.readline()
attempts = 0
while geo:
    if attempts < 3:
        location = urllib.quote_plus(smart_str(geo))
        url = 'http://maps.googleapis.com/maps/api/geocode/json?address=%s&sensor=false' % location
        response = urllib2.urlopen(url).read()
        result = simplejson.loads(response)
        print geo
        print result['status'] + "\n"
        if result['status'] == 'OK':
            lat = str(result['results'][0]['geometry']['location']['lat'])
            lng = str(result['results'][0]['geometry']['location']['lng'])
            location_geo.write(geo)
            location_geo.write('%s %s\n' % (lat, lng))
            location_geo.write(location_index.readline())
            location_geo.flush()
        elif result['status'] == 'OVER_QUERY_LIMIT':
            time.sleep(2)
            attempts += 1
            continue
        elif result['status'] == 'ZERO_RESULTS':
            location_index.readline()
        else:
            print "error: " + result['status'] + "\n"
            break
        attempts = 0
    else:
        location_geo_new.write(geo)
        location_geo_new.write(location_index.readline())
        location_geo_new.flush()
    geo = location_index.readline()
location_index.close()
location_geo.close()
location_geo_new.close()