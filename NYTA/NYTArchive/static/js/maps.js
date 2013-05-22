/**
 * Author: Jiankai Dang
 * Date: 4/24/13
 * Time: 4:24 PM
 */
var map;
var geocoder;
function initialize() {
    var mapOptions = {
        zoom: 3,
        streetViewControl: false,
        mapTypeControl: false,
        mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    map = new google.maps.Map(document.getElementById('map-canvas'),
        mapOptions);
    geocoder = new google.maps.Geocoder();
    var myloc = new google.maps.Marker({
        clickable: false,
        icon: new google.maps.MarkerImage('http://maps.gstatic.com/mapfiles/mobile/mobileimgs2.png',
            new google.maps.Size(22, 22),
            new google.maps.Point(0, 18),
            new google.maps.Point(11, 11)),
        shadow: null,
        zIndex: 999,
        map: map
    });
    // Try HTML5 geolocation
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function (position) {
            var pos = new google.maps.LatLng(position.coords.latitude,
                position.coords.longitude);
            map.setCenter(pos);
//            myloc.setPosition(pos);
            geo();
        }, function () {
            handleNoGeolocation(true);
        });
    }
}
google.maps.event.addDomListener(window, 'load', initialize);
function geo() {
    $.get("/geo/", function (data) {
        var heatmapData = [];
        $.each(data, function (location, geo) {
            heatmapData.push(new google.maps.LatLng(geo[0], geo[1]));
        });
        new google.maps.visualization.HeatmapLayer({
            data: heatmapData,
            dissipating: false
        }).setMap(map);
    });
}