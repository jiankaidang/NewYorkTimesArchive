/**
 * Author: Jiankai Dang
 * Date: 4/24/13
 * Time: 4:24 PM
 */
var map, heatmapLayer, searchInput = $("#search-input"), searchForm = $("#search-form").submit(function () {
    $("#error-msg").hide();
    var query = searchInput.val().trim();
    searchInput.blur();
    clearMarkers();
    if (query) {
        heatmapLayer.setMap(null);
        $.get("/search/maps/", {
            query: query
        }, function (result) {
            var data = result.meta;
            if (!data.length) {
                $("#error-query").html(query);
                $("#error-msg").slideDown();
                return;
            }
            var bounds = new google.maps.LatLngBounds();
            if (data.length > 50) {
                var resultHeatmap = [];
                $.each(data, function (location, data) {
                    var latLng = data[1].latLng, position = new google.maps.LatLng(latLng[0], latLng[1]);
                    resultHeatmap.push(position);
                    bounds.extend(position);
                });
                heatmapLayer.setData(resultHeatmap);
                heatmapLayer.setMap(map);
                fitBounds(bounds);
            } else {
                $.each(data, function (index, data) {
                    var latLng = data[1].latLng, position = new google.maps.LatLng(latLng[0], latLng[1]);
                    markers.push(new google.maps.Marker({
                        position: position,
                        map: map,
                        title: data[0],
                        animation: google.maps.Animation.DROP
                    }));
                    bounds.extend(position);
                });
                fitBounds(bounds);
            }
        });
        return false;
    }
    heatmapLayer.setData(heatmapData);
    heatmapLayer.setMap(map);
    return false;
}), heatmapData = [], currentLocation, markers = [];
function initialize() {
    var mapOptions = {
        streetViewControl: false,
        mapTypeControl: false,
        mapTypeId: google.maps.MapTypeId.ROADMAP,
        zoomControl: false,
        panControl: false
    };
    map = new google.maps.Map(document.getElementById('map-canvas'),
        mapOptions);
    geo();
}
google.maps.event.addDomListener(window, 'load', initialize);
function geo() {
    $.get("/geo/", function (data) {
        var bounds = new google.maps.LatLngBounds();
        $.each(data, function (location, data) {
            var geo = data.latLng, position = new google.maps.LatLng(geo[0], geo[1]);
            heatmapData.push(position);
            bounds.extend(position);
        });
        fitBounds(bounds);
        heatmapLayer = new google.maps.visualization.HeatmapLayer({
            data: heatmapData,
            dissipating: false,
            map: map
        });
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
$("#close-error-btn").click(function () {
    $("#error-msg").slideUp();
});
function clearMarkers() {
    $.each(markers, function (index, marker) {
        marker.setMap(null);
    });
    markers = [];
}
function fitBounds(bounds) {
    map.fitBounds(bounds);
    var zoom = map.getZoom();
    zoom = Math.min(zoom, 16);
    zoom = Math.max(zoom, 2);
    map.setZoom(zoom);
    map.panTo(bounds.getCenter());
}