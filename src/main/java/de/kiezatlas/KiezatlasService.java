package de.kiezatlas;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.accesscontrol.SharingMode;
import de.deepamehta.geomaps.model.GeoCoordinate;
import java.util.List;



public interface KiezatlasService {

    static final String KIEZATLAS_WORKSPACE_NAME = "Kiezatlas";
    static final String KIEZATLAS_WORKSPACE_URI = "de.kiezatlas.workspace";
    static final SharingMode KIEZATLAS_WORKSPACE_SHARING_MODE = SharingMode.PUBLIC;

    static final String WEBSITE         = "ka2.website";
    static final String WEBSITE_TITLE   = "ka2.website.title";
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

    Topic createWebsite(String siteName, String siteUri);

    Association addGeoObjectToWebsite(long geoObjectId, long siteId);

    /** Fetches Geo Coordinate facet related to a Geo Objects topic. */
    GeoCoordinate getGeoCoordinateByGeoObject(Topic geoObject);

    /** Fetches the Geo Coordinate topic related to a Geo Objects Address (!) topic. */
    Topic getGeoCoordinateFacet(Topic address);

    /** Fetches a Geo Object by a Geo Coordinate topic. */
    Topic getGeoObjectByGeoCoordinate(Topic geoCoords);

    /** Fetches the domain topic a Geo Coordinate topic. */
    Topic getDomainTopicByGeoCoordinate(Topic geoCoords);

    List<RelatedTopic> getParentRelatedAggregatedGeoObjects(Topic bezirksFacet);

    Topic getImageFileFacetByGeoObject(Topic facettedTopic);

    void updateImageFileFacet(Topic geoObject, String imageFilePath);

    Topic getFacettedBezirksregionChildTopic(Topic facettedTopic);

    /** String getGeoObjectAttribution(Topic geoObject); */

}
