/**
 * Author: Jiankai Dang
 * Date: 4/24/13
 * Time: 4:24 PM
 */
var map, geocoder, heatmapLayer, searchInput = $("#search-input"), searchForm = $("#search-form").submit(function () {
    heatmapLayer.setMap(searchInput.val() ? null : map);
    searchInput.blur();
    return false;
});
function initialize() {
    var mapOptions = {
        zoom: 3,
        streetViewControl: false,
        mapTypeControl: false,
        mapTypeId: google.maps.MapTypeId.ROADMAP,
        zoomControl: false,
        panControl: false
    };
    map = new google.maps.Map(document.getElementById('map-canvas'),
        mapOptions);
    geocoder = new google.maps.Geocoder();
    // Try HTML5 geolocation
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function (position) {
            var pos = new google.maps.LatLng(position.coords.latitude,
                position.coords.longitude);
            map.setCenter(pos);
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
        heatmapLayer = new google.maps.visualization.HeatmapLayer({
            data: heatmapData,
            dissipating: false
        });
        heatmapLayer.setMap(map);
        searchInput.typeahead({
            source: Object.keys(data),
            updater: function (item) {
                searchInput.val(item);
                searchForm.submit();
                return item;
            }
        });
    });
}