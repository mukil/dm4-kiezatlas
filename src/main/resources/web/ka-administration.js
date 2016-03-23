
var administration = (function($) {
    
    console.log("Kiezatlas Administration Script loaded")
    
    var api = {} 
    
    /** Model: */
    var items =  []

    /** Functions */
    api.load_geo_objects = function(id, callback) {
        $.ajax('/site/einrichtungen/' + id, {
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
        var $listing = $('#listing .soziale-einrichtungen')
            $listing.empty()
        for (var aidx in items) {
            var item = items[aidx]
            $listing.append('<li><div class="list-item"><h3>' + item.value +'</h3>'
                    + '<p><span class="label">' + new Date(item.childs["dm4.time.modified"].value) +'</span></p></div></li>')
            // console.log("Einrichtung", item)
        }
    }

    api.render_menu = function(status) {
        if (status) {
            $('.login').remove()
        }
    }

    api.render_page = function(status) {
        if (status) {
            api.load_geo_objects(7275, api.render_list)
            administration.render_menu(status)
        }
        console.log("Render Page", status)
    }
    
    return api

}($))