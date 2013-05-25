/**
 * Author: Jiankai Dang
 * Date: 4/24/13
 * Time: 4:24 PM
 */
var map, heatmapLayer, searchInput = $("#search-input"), searchForm = $("#search-form").submit(function () {
    $("#error-msg").hide();
    $(".results-container").hide();
    var query = searchInput.val().trim();
    searchInput.blur();
    clearMarkers();
    if (query) {
        heatmapLayer.setMap(null);
        $.get("/search/maps/", {
            query: query,
            page: page
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
            } else {
                var searchResults = $("#search-results").html(result.html).slideDown();
                $.each(data, function (index, data) {
                    var latLng = data[1].latLng, position = new google.maps.LatLng(latLng[0], latLng[1]), marker = new google.maps.Marker({
                        position: position,
                        map: map,
                        title: data[0],
                        animation: google.maps.Animation.DROP
                    });
                    google.maps.event.addListener(marker, 'click', function () {
                        infoWindow && infoWindow.close();
                        bouncingMarker && bouncingMarker.setAnimation(null);
                        infoWindow = new google.maps.InfoWindow({
                            content: searchResults.find("[data-index=" + index + "] td").html()
                        });
                        infoWindow.open(map, marker);
                        marker.setAnimation(google.maps.Animation.BOUNCE);
                        bouncingMarker = marker;
                        map.panTo(bouncingMarker.getPosition());
                        google.maps.event.addListener(infoWindow, 'closeclick', function () {
                            bouncingMarker && bouncingMarker.setAnimation(null);
                        });
                    });
                    markers.push(marker);
                    bounds.extend(position);
                });
            }
            fitBounds(bounds);
        });
        page = 1;
        return false;
    }
    heatmapLayer.setData(heatmapData);
    heatmapLayer.setMap(map);
    return false;
}), heatmapData = [], markers = [], bouncingMarker, page = 1, infoWindow, articlePage = 1;
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
$("#search-results").on("mouseenter", "tr.search-map",function () {
    var marker = markers[$(this).attr("data-index")];
    if (marker != bouncingMarker) {
        infoWindow && infoWindow.close();
        bouncingMarker && bouncingMarker.setAnimation(null);
    }
    bouncingMarker = marker;
    map.panTo(bouncingMarker.getPosition());
    bouncingMarker.setAnimation(google.maps.Animation.BOUNCE);
}).on("mouseleave", "tr.search-map",function () {
        bouncingMarker && bouncingMarker.setAnimation(null);
    }).on("click", ".pagination.search-map a", function () {
        if ($(this).closest("li").is(".active")) {
            return false;
        }
        page = $(this).attr("data-page");
        searchForm.submit();
        return false;
    });
$(document.body).on("click", ".search-location", function () {
    $(".results-container").hide();
    $.get("/search/location/", {
        location: $(this).prev().html(),
        articlePage: articlePage
    }, function (data) {
        $("#search-location").html(data).slideDown();
    });
    return false;
});