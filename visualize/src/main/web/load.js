$(document).ready(function(){
    var map = L.map('mapid').setView([61.842537299999954,-6.808299299999992], 13);
    L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpejY4NXVycTA2emYycXBndHRqcmZ3N3gifQ.rJcFIG214AriISLbB6B5aw', {
        maxZoom: 18,
        attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
        '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
        'Imagery © <a href="http://mapbox.com">Mapbox</a>',
        id: 'mapbox.streets'
    }).addTo(map);

    // Load generated data.
    $.ajax({
        url: "data.js",
        dataType: "json",
        success: function( result ) {
            for (var wayIdx = 0; wayIdx < result.length; wayIdx++) {
                var nodes = result[wayIdx][1];
                for (var nodeIdx = 0; nodeIdx < nodes.length; nodeIdx++) {
                    var node = nodes[nodeIdx];
                    var point = L.point(node[1], node[2]);
                    L.circleMarker([node[1], node[2]], {radius: 2}).addTo(map);
                }
            }
        }
    });

});


