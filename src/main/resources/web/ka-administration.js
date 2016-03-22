
var administration = (function($) {
    
    console.log("Kiezatlas Administration Script loaded")
    
    var api = {} 
    
    /** Model: */
    var items =  []

    /** Functions */
    api.load_geo_objects = function(callback) {
        $.ajax('/site/einrichtungen', {
            type: "GET",
            error: function(e) {
                console.warn("AJAX POST Error", e)
                if (e.status === 200) {
                    items = []
                }
                callback()
            },
            success: function(response) {
                items = response
                console.log('Loaded Einrichtungen', items)
                callback()
            }
        })
    }

    api.render_list = function() {
        var $listing = $('#listing')
        if ($listing.children().length === 0) {
            $listing = $('<ul class="soziale-einrichtungen">')
        }
        $listing.empty()
        for (var aidx in items) {
            var item = items[aidx]
            $listing.append('<li><div class="list-item">' + item.value + "</div></li>")
            console.log("Einrichtung", item)
        }
    }

    api.render_page = function(stats) {
        if (status)
            api.load_geo_objects(api.render_list())
        console.log("Render Page", status)
    }
    
    return api

}($))