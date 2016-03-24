
var administration = (function($) {
    
    console.log("Kiezatlas Administration Script loaded")
    
    var api = {} 
    
    /** Model: */
    var items =  []
    var district = {id: 7275}

    /** Functions */
    api.load_geo_objects = function(id, callback) {
        api.set_district(id)
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

    api.set_district = function(id) {
        if (id) district.id = id
        $('.district-links .button').removeClass('selected')
        $('#' + district.id).addClass('selected')
    }

    api.render_list = function() {
        var $listing = $('#listing .soziale-einrichtungen')
            $listing.empty()
        for (var aidx in items) {
            var item = items[aidx]
            $listing.append('<li><div class="list-item ' + item.class + '"><h3>' + item.name +'</h3>'
                    + '<p>' + item.anschrift + ', <b>' + item.kontakt + '</b><br/><span class="label">'
                    + new Date(item["dm4.time.modified"]) +'</span></p></div></li>')
            // console.log("Einrichtung", item)
        }
    }

    api.render_bezirk_links = function() {
        restc.load_district_topics(function(districts) {
            var $links = $(".district-links")
                $links.empty()
            for (var i in districts) {
                var district = districts[i]
                $links.append('<a id="'+district.id+'" class="button"'
                    + 'href="javascript:administration.load_geo_objects('
                    + district.id + ', administration.render_list)">'
                    + district.value + '</a>&nbsp;')
            }
            api.set_district()
        })
    }

    api.render_menu = function(status) {
        if (status) {
            $('.login').remove()
        }
    }

    api.render_page = function(status) {
        if (status) {
            api.load_geo_objects(district.id, api.render_list)
            administration.render_menu(status)
            administration.render_bezirk_links()
        }
        console.log("Render Page", status)
    }
    
    return api

}($))