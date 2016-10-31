package de.kiezatlas;

import de.deepamehta.core.Association;
import de.deepamehta.accesscontrol.AccessControlService;
import de.deepamehta.geomaps.GeomapsService;
import de.deepamehta.facets.FacetsService;
import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.facets.FacetValueModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Cookies;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreSendTopicListener;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.geomaps.model.GeoCoordinate;
import de.deepamehta.time.TimeService;
import de.deepamehta.workspaces.WorkspacesService;

import javax.ws.rs.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;


/**
 * Kiezatlas 2 Plugin Service Implementation.
 * @author jri & mukil
 */
@Path("/site")
@Consumes("application/json")
@Produces("application/json")
public class KiezatlasPlugin extends PluginActivator implements KiezatlasService, PostUpdateTopicListener,
                                                                                  PreSendTopicListener {

    // ------------------------------------------------------------------------------------------------------- Constants

    // The URIs of KA2 Geo Object topics have this prefix.
    // The remaining part of the URI is the original KA1 topic id.
    public static final String KA2_GEO_OBJECT_URI_PREFIX = "de.kiezatlas.topic.";
    public static final String GEO_OBJECT_OWNER_PROPERTY = "de.kiezatlas.owner";
    public static final String GEO_OBJECT_KEYWORD_PROPERTY = "de.kiezatlas.key.";

    // Website-Geomap association
    private static final String WEBSITE_GEOMAP = "dm4.core.association";
    private static final String ROLE_TYPE_WEBSITE = "dm4.core.default";     // Note: used for both associations
    private static final String ROLE_TYPE_GEOMAP = "dm4.core.default";
    // Website-Facet Types association
    private static final String WEBSITE_FACET_TYPES = "dm4.core.association";
    private static final String ROLE_TYPE_FACET_TYPE = "dm4.core.default";
    // Website-Geo Object association
    private static final String WEBSITE_GEOOBJECT = "dm4.core.association";
    private static final String ROLE_TYPE_GEOOBJECT = "dm4.core.parent";
    private static final String ROLE_TYPE_SITE = "dm4.core.child";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject private GeomapsService geomapsService;
    @Inject private FacetsService facetsService;
    @Inject private TimeService timeService;
    @Inject private AccessControlService accessControlService;
    @Inject private WorkspacesService workspaceService;

    private Logger logger = Logger.getLogger(getClass().getName());

    Topic kiezatlasWorkspace = null;

    // -------------------------------------------------------------------------------------------------- Public Methods



    /** ------------------------------------------ Plugin Service --------------------------------------------- */

    /**
     * Useful to create a new or load an existing "Site" topic (by its uri).
     * @param siteName
     * @param siteUri
     * @return A topic of type "ka2.website".
     */
    @POST
    @Path("/create/{siteName}/{siteUri}")
    @Transactional
    public Topic createWebsite(@PathParam("siteName") String siteName, @PathParam("siteUri") String siteUri) {
        isAuthorized();
        return createKiezatlasWebsite(siteName, siteUri);
    }

    /**
     * Useful to create a standard association between a "Geo object" topic and a "Site" topic.
     * @param geoObjectId
     * @param siteId
     * @return Association representing the relation between a Geo Object and a Site.
     */
    @DELETE
    @Path("/{geoObjectId}/{siteId}")
    @Transactional
    public void removeGeoObjectFromWebsite(@PathParam("geoObjectId") long geoObjectId,
                                           @PathParam("siteId") long siteId) {
        isAuthorized();
        Topic geoObject = dm4.getTopic(geoObjectId);
        Topic siteTopic = dm4.getTopic(siteId);
        removeGeoObjectFromWebsite(geoObject, siteTopic);
    }

    /**
     * Useful to create a standard association between a "Geo object" topic and a "Site" topic.
     * @param geoObjectId
     * @param siteId
     * @return Association representing the relation between a Geo Object and a Site.
     */
    @POST
    @Path("/{geoObjectId}/{siteId}")
    @Transactional
    public Association addGeoObjectToWebsite(@PathParam("geoObjectId") long geoObjectId,
            @PathParam("siteId") long siteId) {
        isAuthorized();
        Topic geoObject = dm4.getTopic(geoObjectId);
        Topic siteTopic = dm4.getTopic(siteId);
        return addGeoObjectToWebsite(geoObject, siteTopic);
    }

    @GET
    @Path("/geomap/{geomap_id}")
    @Override
    public Topic getWebsite(@PathParam("geomap_id") long geomapId) {
        try {
            return dm4.getTopic(geomapId).getRelatedTopic(WEBSITE_GEOMAP, ROLE_TYPE_WEBSITE, ROLE_TYPE_GEOMAP,
                WEBSITE);
        } catch (Exception e) {
            throw new RuntimeException("Finding the geomap's website topic failed (geomapId=" + geomapId + ")", e);
        }
    }

    @GET
    @Path("/{website_id}/facets")
    @Override
    public List<RelatedTopic> getFacetTypes(@PathParam("website_id") long websiteId) {
        try {
            return dm4.getTopic(websiteId).getRelatedTopics(WEBSITE_FACET_TYPES, ROLE_TYPE_WEBSITE,
                ROLE_TYPE_FACET_TYPE, "dm4.core.topic_type");
        } catch (Exception e) {
            throw new RuntimeException("Finding the website's facet types failed (websiteId=" + websiteId + ")", e);
        }
    }

    @GET
    @Path("/website/{siteId}/objects")
    @Override
    public List<RelatedTopic> getGeoObjectsBySite(@PathParam("siteId") long siteId) {
        try {
            Topic site = dm4.getTopic(siteId);
            List<RelatedTopic> geoObjects = null;
            if (site.getTypeUri().equals("ka2.website")) {
                geoObjects = site.getRelatedTopics(WEBSITE_GEOOBJECT, ROLE_TYPE_SITE, ROLE_TYPE_GEOOBJECT, GEO_OBJECT);
            }
            return geoObjects;
        } catch (Exception e) {
            throw new RuntimeException("Fetching the website's geo objects failed (siteId=" + siteId + ")", e);
        }
    }

    @GET
    @Path("/criteria")
    @Override
    public List<Topic> getAllCriteria() {
        List<Topic> criteria = dm4.getTopicsByValue("uri", new SimpleValue("ka2.criteria.*"));
        // remove facet types
        Iterator<Topic> i = criteria.iterator();
        while (i.hasNext()) {
            Topic crit = i.next();
            if (crit.getUri().endsWith(".facet")) {
                i.remove();
            }
        }
        //
        return criteria;
    }

    @GET
    @Path("/geomap/{geomap_id}/objects")
    @Override
    public List<Topic> getGeoObjects(@PathParam("geomap_id") long geomapId) {
        try {
            return fetchGeoObjects(geomapId);
        } catch (Exception e) {
            throw new RuntimeException("Fetching the geomap's geo objects failed (geomapId=" + geomapId + ")", e);
        }
    }

    @GET
    @Path("/category/{id}/objects")
    @Override
    public List<RelatedTopic> getGeoObjectsByCategory(@PathParam("id") long categoryId) {
        return dm4.getTopic(categoryId).getRelatedTopics("dm4.core.aggregation", "dm4.core.child", "dm4.core.parent",
                GEO_OBJECT);
    }

    /** Used by famportal editorial tool. */
    @GET
    @Path("/geoobject")
    @Override
    public GeoObjects searchGeoObjectNames(@QueryParam("search") String searchTerm, @QueryParam("clock") long clock) {
        GeoObjects result = new GeoObjects(clock);
        for (Topic geoObjectName : dm4.searchTopics("*" + searchTerm + "*", GEO_OBJECT_NAME)) {
            result.add(getGeoObjectByNameTopic(geoObjectName));
        }
        return result;
    }

    /** @PUT
    @Path("/geoobject/attribution/{topicId}/{owner}")
    @Transactional
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createGeoObjectAttribution(@PathParam("topicId") long id,
                                               @PathParam("owner") String owner, String key) {
        checkAuthorization();
        Topic geoObject = dm4.getTopic(id);
        if (geoObject != null && !owner.isEmpty() && !key.isEmpty()) {
            String value = owner.trim();
            String keyValue = key.trim();
            try {
                String existingValue = (String) geoObject.getProperty(GEO_OBJECT_OWNER_PROPERTY);
                logger.warning("Values already set: Updating not allowed, owner=" + existingValue);
                return Response.status(405).build();
            } catch (Exception e) {  // ### org.neo4j.graphdb.NotFoundException
                geoObject.setProperty(GEO_OBJECT_OWNER_PROPERTY, value, true); // ### addToIndex=true?
                geoObject.setProperty(GEO_OBJECT_KEYWORD_PROPERTY, keyValue, false);
                logger.info("### Equipped \"" + geoObject.getSimpleValue() + "\" with owner=\"" + owner + "\" and " +
                        "key=\"" + key + "\"");
                return Response.status(200).build();
            }
        } else if (owner.isEmpty()){
            logger.warning("Owner and/or key empty - Not allowed");
            return Response.status(405).build();
        }
        return Response.status(404).build();
    } **/

    @GET
    @Path("/category/objects")
    @Override
    public GroupedGeoObjects searchCategories(@QueryParam("search") String searchTerm,
            @QueryParam("clock") long clock) {
        GroupedGeoObjects result = new GroupedGeoObjects(clock);
        for (Topic criteria : getAllCriteria()) {
            for (Topic category : dm4.searchTopics("*" + searchTerm + "*", criteria.getUri())) {
                result.add(criteria, category, getGeoObjectsByCategory(category.getId()));
            }
        }
        return result;
    }

    @GET
    @Path("/workspace")
    public long getKiezatlasWorkspaceId() {
        if (kiezatlasWorkspace != null) return kiezatlasWorkspace.getId();
        kiezatlasWorkspace = dm4.getTopicByUri(KIEZATLAS_WORKSPACE_URI);
        return kiezatlasWorkspace.getId();
    }

    @Override
    public Topic createKiezatlasWebsite(String siteName, String siteUri) {
        Topic websiteTopic = dm4.getTopicByValue("uri", new SimpleValue(siteUri));
        if (websiteTopic == null) {
            logger.info("CREATING Kiezatlas Website \"" + siteName + " with siteUri=\"" + siteUri + "\"");
            websiteTopic = dm4.createTopic(mf.newTopicModel(siteUri, WEBSITE, mf.newChildTopicsModel()
                .put("ka2.website.title", siteName)));
        } else {
            logger.info("Kiezatlas Website with siteUri=\"" + siteUri + "\" already exists");
        }
        return websiteTopic;
    }

    @Override
    public Association addGeoObjectToWebsite(Topic geoObject, Topic website) {
        Association relation = null;
        if (geoObject.getTypeUri().equals(GEO_OBJECT) && website.getTypeUri().equals(WEBSITE)) {
            if (!isAssignedToWebsite(geoObject, website.getId())) {
                logger.info("ADDING Geo Object \"" + geoObject.getSimpleValue()
                    + "\" to Site \"" + website.getSimpleValue() + "\"");
                relation = dm4.createAssociation(mf.newAssociationModel(WEBSITE_GEOOBJECT,
                    mf.newTopicRoleModel(geoObject.getId(), ROLE_TYPE_GEOOBJECT),
                    mf.newTopicRoleModel(website.getId(), ROLE_TYPE_SITE)));
            } else {
                logger.info("Skipping adding Geo Object to Site, Assignment already EXISTS");
            }
        }
        return relation;
    }

    @Override
    public void removeGeoObjectFromWebsite(Topic geoObject, Topic website) {
        if (geoObject.getTypeUri().equals(GEO_OBJECT) && website.getTypeUri().equals(WEBSITE)) {
            if (isAssignedToWebsite(geoObject, website.getId())) {
                logger.info("REMOVING Geo Object \"" + geoObject.getSimpleValue()
                    + "\" from Site \"" + website.getSimpleValue() + "\"");
                Association assignment = dm4.getAssociation(WEBSITE_GEOOBJECT, geoObject.getId(), website.getId(),
                    ROLE_TYPE_GEOOBJECT, ROLE_TYPE_SITE);
                assignment.delete();
            } else {
                logger.info("Skipping removal of Geo Object from Site, Assignment does not EXIST");
            }
        }
    }

    /** ---------------------------------- Kiezatlas ETL Plugin Service Helper Methods ------------------------- */

    /** Note: This facet depends on the installation of the dm4-kiezatlas-etl plugin. */
    @Override
    public Topic getImageFileFacetByGeoObject(Topic geoObject) {
        return facetsService.getFacet(geoObject, "ka2.bild.facet");
    }

    /** Note: This facet depends on the installation of the dm4-kiezatlas-etl plugin. */
    @Override
    public void updateImageFileFacet(Topic geoObject, String imageFilePath) {
        facetsService.updateFacet(geoObject, "ka2.bild.facet",
            mf.newFacetValueModel("ka2.bild.pfad").put(imageFilePath));
    }

    /** This facet depends/just exists after installation of the dm4-kiezatlas-etl plugin. */
    @Override
    public Topic getFacettedBezirksregionChildTopic(Topic facettedTopic) {
        // Note: Untested
        return facetsService.getFacet(facettedTopic, "ka2.bezirksregion.facet");
    }



    /** -------------------------------------- Kiezatlas Geo Object Helper Methods ----------------------------- */

    @Override
    public GeoCoordinate getGeoCoordinateByGeoObject(Topic geoObject) {
        Topic addressTopic = geoObject.getChildTopics().getTopic(GEO_OBJECT_ADDRESS);
        if (addressTopic != null) return geomapsService.getGeoCoordinate(addressTopic);
        logger.warning("Geo Coordinate could not be determined becuase no Address is related to Geo Object: "
            + geoObject.getSimpleValue());
        return null;
    }

    @Override
    public Topic getGeoObjectByGeoCoordinate(Topic geoCoords) {
        Topic address = null, geoObject = null;
        try {
            address = geoCoords.getRelatedTopic("dm4.core.composition", "dm4.core.child",
                "dm4.core.parent", "dm4.contacts.address");
            if (address != null) {
                geoObject = address.getRelatedTopic("dm4.core.composition", "dm4.core.child",
                    "dm4.core.parent", "ka2.geo_object");
            }
        } catch(RuntimeException ex) {
            logger.warning("Could not load Geo Object for Geo Coordinate (" + geoCoords
                + "), Exception: " + ex.getMessage());
        }
        return geoObject;
    }

    @Override
    public Topic getDomainTopicByGeoCoordinate(Topic geoCoords) {
        Topic geoObject = null;
        try {
            geoObject = geomapsService.getDomainTopic(geoCoords.getId());
        } catch(RuntimeException ex) {
            logger.warning("Could not load Geo Object for Geo Coordinate (" + geoCoords
                + "), Exception: " + ex.getMessage());
        }
        return geoObject;
    }

    /**
     * @retrun A Geo Coordinate topic (including its child topics) of a geo-facetted topic (e.g. an Address),
     * or <code>null</code> if no geo coordinate is stored.
     */
    @Override
    public Topic getGeoCoordinateFacet(Topic addressTopic) {
        Topic geoCoordTopic = facetsService.getFacet(addressTopic, "dm4.geomaps.geo_coordinate_facet");
        return geoCoordTopic != null ? geoCoordTopic.loadChildTopics() : null;
    }

    @Override
    public List<RelatedTopic> getParentRelatedAggregatedGeoObjects(Topic bezirksFacet) {
        return bezirksFacet.getRelatedTopics("dm4.core.aggregation", "dm4.core.child",
            "dm4.core.parent", "ka2.geo_object");
    }

    @Override
    public boolean isAssignedToKiezatlasWebsite(Topic geoObject, Topic website) {
        return isAssignedToWebsite(geoObject, website.getId());
    }

    /**
     * @param geoObject
     * @return A string representing information on the original owner of a kiezatlas 1 einrichtungs topic with two
     * values seperated by a colon "owner:keyword".
    @Override
    public String getGeoObjectAttribution(Topic geoObject) {
        return (String) geoObject.getProperty(GEO_OBJECT_OWNER_PROPERTY) + ":"
            + (String) geoObject.getProperty(GEO_OBJECT_KEYWORD_PROPERTY);
    } **/



    /** ------------------------------------------ Listener Implementations ------------------------------------ */

    @Override
    public void preSendTopic(Topic topic) {
        if (!topic.getTypeUri().equals(GEO_OBJECT)) {
            return;
        }
        //
        List<RelatedTopic> facetTypes = getFacetTypes();
        if (facetTypes == null) {
            return;
        }
        //
        enrichWithFacets(topic, facetTypes);
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel newModel, TopicModel oldModel) {
        if (!topic.getTypeUri().equals(GEO_OBJECT)) {
            return;
        }
        //
        List<RelatedTopic> facetTypes = getFacetTypes();
        if (facetTypes == null) {
            return;
        }
        //
        updateFacets(topic, facetTypes, newModel);
    }



    /* ---------------------------------------------------------- Private Methods ---------------------------------- */

    /**
     * First checks for a valid session and then it checks fo for a "Membership" association between the
     * requesting username and the \"Kiezatlas\" workspace.
     **/
    private void isAuthorized() throws WebApplicationException {
        String username = accessControlService.getUsername();
        if (username == null) throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        if (kiezatlasWorkspace == null) getKiezatlasWorkspaceId();
        if (!accessControlService.isMember(username, kiezatlasWorkspace.getId())) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
    }

    // === Enrich with facets ===

    private void enrichWithFacets(Topic geoObject, List<RelatedTopic> facetTypes) {
        for (Topic facetType : facetTypes) {
            String facetTypeUri = facetType.getUri();
            if (!isMultiFacet(facetTypeUri)) {
                enrichWithSingleFacet(geoObject, facetTypeUri);
            } else {
                enrichWithMultiFacet(geoObject, facetTypeUri);
            }
        }
    }

    // ---

    private void enrichWithSingleFacet(Topic geoObject, String facetTypeUri) {
        Topic facetValue = facetsService.getFacet(geoObject, facetTypeUri);
        // Note: facetValue is null in 2 cases:
        // 1) The geo object has just been created (no update yet)
        // 2) The geo object has been created outside a geomap and then being revealed in a geomap.
        if (facetValue == null) {
            logger.info("### Enriching geo object " + geoObject.getId() + " with its \"" + facetTypeUri +
                "\" facet value ABORTED -- no such facet in DB");
            return;
        }
        //
        logger.info("### Enriching geo object " + geoObject.getId() + " with its \"" + facetTypeUri +
            "\" facet value (" + facetValue + ")");
        geoObject.getChildTopics().getModel().put(facetValue.getTypeUri(), facetValue.getModel());
    }

    private void enrichWithMultiFacet(Topic geoObject, String facetTypeUri) {
        List<RelatedTopic> facetValues = facetsService.getFacets(geoObject, facetTypeUri);
        logger.info("### Enriching geo object " + geoObject.getId() + " with its \"" + facetTypeUri +
            "\" facet values (" + facetValues + ")");
        String childTypeUri = getChildTypeUri(facetTypeUri);
        // Note: we set the facet values at once (using put()) instead of iterating (and using add()) as after an geo
        // object update request the facet values are already set. Using add() would result in having the values twice.
        geoObject.getChildTopics().getModel().put(childTypeUri, DeepaMehtaUtils.<RelatedTopicModel>toModelList(facetValues));
    }



    // === Update facets ===

    private void updateFacets(Topic geoObject, List<RelatedTopic> facetTypes, TopicModel newModel) {
        for (Topic facetType : facetTypes) {
            String facetTypeUri = facetType.getUri();
            String childTypeUri = getChildTypeUri(facetTypeUri);
            if (!isMultiFacet(facetTypeUri)) {
                TopicModel facetValue = newModel.getChildTopicsModel().getTopic(childTypeUri);
                logger.info("### Storing facet of type \"" + facetTypeUri + "\" for geo object " + geoObject.getId() +
                    " (facetValue=" + facetValue + ")");
                FacetValueModel value = mf.newFacetValueModel(childTypeUri).put(facetValue);
                facetsService.updateFacet(geoObject, facetTypeUri, value);
            } else {
                List<? extends RelatedTopicModel> facetValues = newModel.getChildTopicsModel().getTopics(childTypeUri);
                logger.info("### Storing facets of type \"" + facetTypeUri + "\" for geo object " + geoObject.getId() +
                    " (facetValues=" + facetValues + ")");
                FacetValueModel value = mf.newFacetValueModel(childTypeUri).put(facetValues);
                facetsService.updateFacet(geoObject, facetTypeUri, value);
            }
        }
    }



    // === Helper ===

    /**
     * Returns the facet types for the current topicmap, or null if the facet types can't be determined.
     * There can be several reasons for the latter:
     *   a) there is no "current topicmap". This can be the case with 3rd-party clients.
     *   b) the current topicmap is not a geomap.
     *   c) the geomap is not part of a Kiezatlas Website.
     *
     * @return  The facet types (as a result set, may be empty), or <code>null</code>.
     */
    private List<RelatedTopic> getFacetTypes() {
        Cookies cookies = Cookies.get();
        if (!cookies.has("dm4_topicmap_id")) {
            logger.fine("### Finding geo object facet types ABORTED -- topicmap is unknown (no \"dm4_topicmap_id\" " +
                "cookie was sent)");
            return null;
        }
        //
        long topicmapId = cookies.getLong("dm4_topicmap_id");
        if (!isGeomap(topicmapId)) {
            logger.fine("### Finding geo object facet types for topicmap " + topicmapId + " ABORTED -- not a geomap");
            return null;
        }
        //
        Topic website = getWebsite(topicmapId);
        if (website == null) {
            logger.info("### Finding geo object facet types for geomap " + topicmapId + " ABORTED -- not part of a " +
                "Kiezatlas website");
            return null;
        }
        //
        logger.info("### Finding geo object facet types for geomap " + topicmapId);
        return getFacetTypes(website.getId());
    }

    private List<Topic> fetchGeoObjects(long geomapId) {
        List<Topic> geoObjects = new ArrayList<Topic>();
        for (TopicModel geoCoord : geomapsService.getGeomap(geomapId)) {
            Topic geoObject = geomapsService.getDomainTopic(geoCoord.getId());
            geoObjects.add(geoObject);
            // ### TODO: optimization. Include only name and address in returned geo objects.
            // ### For the moment the entire objects are returned, including composite values and facets.
        }
        return geoObjects;
    }

    // ---

    /** private void checkAuthorization() {
        String username = accessControlService.getUsername();
        if (username == null || username.isEmpty()) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        logger.info("Request Authorized for \"" + username + "\"");
    } */

    private boolean isAssignedToWebsite(Topic geoObject, long siteId) {
        List<RelatedTopic> sites = geoObject.getRelatedTopics(WEBSITE_GEOOBJECT, "dm4.core.parent",
            "dm4.core.child", WEBSITE);
        for (RelatedTopic site : sites) {
            if (site.getId() == siteId) return true;
        }
        return false;
    }

    private Topic getGeoObjectByNameTopic(Topic geoObjectName) {
        return geoObjectName.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent",
            GEO_OBJECT);   // ### TODO: Core API should provide type-driven navigation
    }

    private boolean isGeomap(long topicmapId) {
        Topic topicmap = dm4.getTopic(topicmapId);
        String rendererUri = topicmap.getChildTopics().getString("dm4.topicmaps.topicmap_renderer_uri");
        return rendererUri.equals("dm4.geomaps.geomap_renderer");
    }

    // ---

    // ### FIXME: there is a copy in FacetsPlugin.java
    private boolean isMultiFacet(String facetTypeUri) {
        return getAssocDef(facetTypeUri).getChildCardinalityUri().equals("dm4.core.many");
    }

    // ### FIXME: there is a copy in FacetsPlugin.java
    private String getChildTypeUri(String facetTypeUri) {
        return getAssocDef(facetTypeUri).getChildTypeUri();
    }

    // ### FIXME: there is a copy in FacetsPlugin.java
    private AssociationDefinition getAssocDef(String facetTypeUri) {
        // Note: a facet type has exactly *one* association definition
        return dm4.getTopicType(facetTypeUri).getAssocDefs().iterator().next();
    }
}
