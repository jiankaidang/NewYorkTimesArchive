/**
 * Author: Jiankai Dang
 * Date: 4/24/13
 * Time: 4:24 PM
 */
var map;
var geocoder;
function initialize() {
    var mapOptions = {
        zoom: 6,
        streetViewControl: false,
        mapTypeControl: false,
        mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    map = new google.maps.Map(document.getElementById('map-canvas'),
        mapOptions);
    geocoder = new google.maps.Geocoder();
    // Try HTML5 geolocation
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function (position) {
            var pos = new google.maps.LatLng(position.coords.latitude,
                position.coords.longitude);

            var infowindow = new google.maps.InfoWindow({
                map: map,
                position: pos,
                content: 'Location found using HTML5.'
            });

            map.setCenter(pos);
            geo();
        }, function () {
            handleNoGeolocation(true);
        });
    } else {
        // Browser doesn't support Geolocation
        handleNoGeolocation(false);
    }
}

function handleNoGeolocation(errorFlag) {
    if (errorFlag) {
        var content = 'Error: The Geolocation service failed.';
    } else {
        var content = 'Error: Your browser doesn\'t support geolocation.';
    }

    var options = {
        map: map,
        position: new google.maps.LatLng(60, 105),
        content: content
    };

    var infowindow = new google.maps.InfoWindow(options);
    map.setCenter(options.position);
}

google.maps.event.addDomListener(window, 'load', initialize);
function geo() {
    $.get("/geo/", function (data) {
        console.log(data);
        $.each(data, function (location, geo) {
            new google.maps.Marker({
                map: map,
                position: new google.maps.LatLng(geo[0], geo[1]),
                title: location
            });
        });
    });
}