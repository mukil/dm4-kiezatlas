package de.kiezatlas;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.accesscontrol.SharingMode;
import de.deepamehta.geomaps.model.GeoCoordinate;
import java.util.List;
import javax.ws.rs.PathParam;



public interface KiezatlasService {

    // Kiezatlas Workspace Definition
    static final String KIEZATLAS_WORKSPACE_NAME                = "Kiezatlas";
    static final String KIEZATLAS_WORKSPACE_URI                 = "de.kiezatlas.workspace";
    static final SharingMode KIEZATLAS_WORKSPACE_SHARING_MODE   = SharingMode.PUBLIC;

    // Kiezatlas Website (see dm4-kiezatlas-website module for later versions).
    static final String WEBSITE                                 = "ka2.website";
    static final String WEBSITE_TITLE                           = "ka2.website.title";

    // Kiezatlas Geo Object Definition
    static final String GEO_OBJECT                              = "ka2.geo_object";
    static final String GEO_OBJECT_NAME                         = "ka2.geo_object.name";

    // Contacts Plugin
    static final String GEO_OBJECT_ADDRESS                      = "dm4.contacts.address";

    // Geomaps Plugin
    static final String GEO_COORDINATE_FACET                    = "dm4.geomaps.geo_coordinate_facet";

    // Kiezatlas ETL Plugin
    static final String IMAGE_FACET                             = "ka2.bild.facet";
    static final String IMAGE_PATH                              = "ka2.bild.pfad";
    static final String BEZIRKSREGION_FACET                     = "ka2.bezirksregion.facet";

    // Kiezatlas 1 Legacy Type & Property Constants
    // The URIs of KA2 Geo Object topics have this prefix.
    // The remaining part of the URI is the original KA1 topic id.
    // public static final String KA2_GEO_OBJECT_URI_PREFIX = "de.kiezatlas.topic.";
    // public static final String GEO_OBJECT_OWNER_PROPERTY = "de.kiezatlas.owner";
    // public static final String GEO_OBJECT_KEYWORD_PROPERTY = "de.kiezatlas.key.";

    /**
     * Returns the "Kiezatlas Website" topic the given geomap is assigned to.
     */
    Topic getWebsite(long geomapId);

    /**
     * Returns the facet types assigned to the given Kiezatlas Website.
     */
    List<RelatedTopic> getFacetTypes(long websiteId);

    /**
     * Returns the facet type definitions assigned to the given Kiezatlas Website.
     */
    List<TopicType> getFacetTopicTypes(long websiteId);

    /**
     * Returns all Kiezatlas criteria existing in the DB. ### Experimental
     * A Kiezatlas criteria is a topic type whose URI starts with <code>ka2.criteria.</code>
     * but does not end with <code>.facet</code>.
     */
    List<Topic> getAllCriteria();

    /**
     * Returns all Geo Objects assigned to the given "Geomap" (a special kind of "Topicmap").
     */
    List<Topic> getGeoObjects(long geomapId);

    /**
     * Returns all "Geo Objects" associated (parent) with the given Kiezatlas Website.
     */
    List<RelatedTopic> getGeoObjectsBySite(@PathParam("siteId") long siteId);

    /**
     * Returns all "Geo Objects" associated (aggregated, parent) with the given category.
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
     * Searches for categories (topics with typeUri="ka2.criteria.*" introduced via dm4-kiezatlas-etl)
     * that match the search term (case-insensitive substring search) and returns all Geo Objects of
     * those categories, grouped by category.
     *
     * @param   clock   The logical clock value send back to the client (contained in GroupedGeoObjects).
     *                  Allows the client to order asynchronous responses.
     */
    GroupedGeoObjects searchCategories(String searchTerm, long clock);

    /**
     * Service method to create unique "Kiezatlas Website" topics.
     * @param siteName
     * @param siteUri
     * @return 
     */
    Topic createKiezatlasWebsite(String siteName, String siteUri);

    /**
     * Creates an assignment between a Kiezatlas Geo Object and the given Kiezatlas Website.
     * 
     * @param geoObject
     * @param website
     * @return 
     */
    Association addGeoObjectToWebsite(Topic geoObject, Topic website);

    /**
     * Removes an assignment between a Kiezatlas Geo Object and the given Kiezatlas Website.
     * 
     * @param geoObject
     * @param website 
     */
    void removeGeoObjectFromWebsite(Topic geoObject, Topic website);

    /** 
     * Utility to check if the given Geo Object is assigned to the given Kiezatlas Website.
     * 
     * @param geoObject
     * @param website
     * @return 
     */
    boolean isAssignedToKiezatlasWebsite(Topic geoObject, Topic website);

    /**
     * Utility method to check if the requesting user is a member of the "Kiezatlas" workspace.
     * 
     * @return 
     */
    boolean isKiezatlasWorkspaceMember();

    /**
     * Fetches the "Geo Coordinate" facet related to the given Geo Object.
     */
    GeoCoordinate getGeoCoordinateByGeoObject(Topic geoObject);

    /**
     * Utility to enrich the given Geo Object with the facets configured for the given Kiezatlas Website.
     * 
     * @param geoObject
     * @param websiteId
     * @return 
     */
    Topic enrichWithFacets(Topic geoObject, long websiteId);

    /**
     * Method to update the facet values configured for the given Kiezatlas Website on the Geo Object.
     * Note: Values for all facets **must* be provided.
     * 
     * @param geoObjectId
     * @param facetTypes
     * @param model 
     */
    void updateFacets(long geoObjectId, List<RelatedTopic> facetTypes, TopicModel model);

    /**
     * Fetches the geo coordinate topic for the given topic.
     * 
     * @return A Geo Coordinate topic (including its child topics) of a geo-facetted topic (e.g. an Address),
     * or <code>null</code> if no geo coordinate is stored.
     */
    Topic getGeoCoordinateFacet(Topic address);

    /** Fetches a Geo Object by a Geo Coordinate topic. */
    Topic getGeoObjectByGeoCoordinate(Topic geoCoords);

    /** Fetches the domain topic a Geo Coordinate topic. */
    Topic getDomainTopicByGeoCoordinate(Topic geoCoords);

    /** Fetches the Geo Object for any of its aggregated childs. */
    List<RelatedTopic> getAggregatingGeoObjects(Topic bezirksFacet);

}
