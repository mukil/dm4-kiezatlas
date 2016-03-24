package de.kiezatlas;

import de.deepamehta.core.JSONEnabled;
import de.deepamehta.plugins.geomaps.model.GeoCoordinate;
import org.codehaus.jettison.json.JSONArray;

import org.codehaus.jettison.json.JSONObject;



/**
 * A data transfer object as returned by the Kiezatlas Institution API service.
 */
public class SocialInstitutionObject implements JSONEnabled {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private JSONObject json = new JSONObject();

    // -------------------------------------------------------------------------------------------------- Public Methods

    @Override
    public JSONObject toJSON() {
        return json;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    void setName(String name) {
        try {
            json.put("name", name);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }

    void setBezirk(String bezirk) {
        try {
            json.put("bezirk", bezirk);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }

    void setGeoCoordinate(GeoCoordinate geoCoord) {
        try {
            JSONObject geolocation = new JSONObject();
            geolocation.put("lon", geoCoord.lon);
            geolocation.put("lat", geoCoord.lat);
            //
            json.put("geolocation", geolocation);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }

    void setDistanceInMeter(String meter) {
        try {
            json.put("distanz", meter);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }

    void setUri(String uri) {
        try {
            json.put("uri", uri);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }
    
    void setAddress(String address) {
        try {
            json.put("anschrift", address);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }

    void setAddresses(JSONArray addresses) {
        try {
            json.put("anschriften", addresses);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }
    
    void setOpeningHours(String openingHours) {
        try {
            json.put("oeffnungszeiten", openingHours);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }

    void setContact(String contact) {
        try {
            json.put("kontakt", contact);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }

    void setCreated(long timestamp) {
        try {
            json.put("created", timestamp);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }

    void setLastModified(long timestamp) {
        try {
            json.put("modified", timestamp);
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }

    void addClassification(String className) {
        try {
            if (json.has("class")) {
                json.put("class", json.get("class") + " " + className);
            } else {
                json.put("class", className);   
            }
        } catch (Exception e) {
            throw new RuntimeException("Constructing a SocialInstitutionObject failed", e);
        }
    }

}
