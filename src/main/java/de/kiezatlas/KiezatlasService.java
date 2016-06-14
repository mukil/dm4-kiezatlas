package de.kiezatlas;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;

import java.util.List;



public interface KiezatlasService {

    static final String WEBSITE         = "ka2.website";
    static final String GEO_OBJECT      = "ka2.geo_object";
    static final String GEO_OBJECT_NAME = "ka2.geo_object.name";
    static final String GEO_OBJECT_ADDRESS = "dm4.contacts.address";

    /**
     * Returns the "Kiezatlas Website" topic the given geomap is assigned to.
     */
    Topic getWebsite(long geomapId);

    /**
     * Returns the facet types assigned to the given Kiezatlas Website.
     */
    List<RelatedTopic> getFacetTypes(long websiteId);

    /**
     * Returns all Kiezatlas criteria existing in the DB. ### Experimental
     * A Kiezatlas criteria is a topic type whose URI starts with <code>ka2.criteria.</code>
     * but does not end with <code>.facet</code>.
     */
    List<Topic> getAllCriteria();

    /**
     * Returns all Geo Objects assigned to the given geomap.
     */
    List<Topic> getGeoObjects(long geomapId);

    /**
     * Returns all Geo Objects assigned to the given category.
     */
    List<RelatedTopic> getGeoObjectsByCategory(long categoryId);

    /**
     * Searches for Geo Objects whose name match the search term (case-insensitive substring search).
     *
     * @param   clock   The logical clock value send back to the client (contained in GeoObjects).
     *                  Allows the client to order asynchronous responses.
     */
    GeoObjects searchGeoObjectNames(String searchTerm, long clock);

    /**
     * Searches for categories that match the search term (case-insensitive substring search)
     * and returns all Geo Objects of those categories, grouped by category.
     *
     * @param   clock   The logical clock value send back to the client (contained in GroupedGeoObjects).
     *                  Allows the client to order asynchronous responses.
     */
    GroupedGeoObjects searchCategories(String searchTerm, long clock);
}
