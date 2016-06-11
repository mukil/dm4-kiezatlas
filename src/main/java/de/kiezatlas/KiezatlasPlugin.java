package de.kiezatlas;

import de.deepamehta.core.Association;
import de.deepamehta.plugins.accesscontrol.AccessControlService;
import de.deepamehta.plugins.geomaps.GeomapsService;
import de.deepamehta.plugins.facets.model.FacetValue;
import de.deepamehta.plugins.facets.FacetsService;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Cookies;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.service.event.PreSendTopicListener;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.plugins.geomaps.model.GeoCoordinate;
import de.deepamehta.plugins.time.TimeService;
import de.deepamehta.plugins.workspaces.WorkspacesService;
import java.io.InputStream;

import javax.ws.rs.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;
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
    private static final String KA2_GEO_OBJECT_URI_PREFIX = "de.kiezatlas.topic.";
    private static final String GEO_OBJECT_OWNER_PROPERTY = "de.kiezatlas.owner";
    private static final String GEO_OBJECT_KEYWORD_PROPERTY = "de.kiezatlas.key.";

    // Website-Geomap association
    private static final String WEBSITE_GEOMAP = "dm4.core.association";
    private static final String ROLE_TYPE_WEBSITE = "dm4.core.default";     // Note: used for both associations
    private static final String ROLE_TYPE_GEOMAP = "dm4.core.default";
    // Website-Facet Types association
    private static final String WEBSITE_FACET_TYPES = "dm4.core.association";
    private static final String ROLE_TYPE_FACET_TYPE = "dm4.core.default";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    @Inject private GeomapsService geomapsService;
    @Inject private FacetsService facetsService;
    @Inject private TimeService timeService;
    @Inject private AccessControlService accessControlService;
    @Inject private WorkspacesService workspaceService;

    private Logger logger = Logger.getLogger(getClass().getName());

    // -------------------------------------------------------------------------------------------------- Public Methods



    /**
     * Fetches a simple page rendering objects for kiez-administrators.
     * @return
     */
    @GET
    @Path("/administration")
    @Produces(MediaType.TEXT_HTML)
    public InputStream getKiezatlasAdministrationPage() {
        String username = accessControlService.getUsername();
        if (username != null && !username.equals("admin")) throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        return getStaticResource("web/kiez-administration.html");
    }



    // **********************
    // *** Plugin Service ***
    // **********************

    @POST
    @Path("/create/{siteName}/{siteUri}")
    @Transactional
    @Override
    public Topic createWebsite(@PathParam("siteName") String siteName, @PathParam("siteUri") String siteUri) {
        Topic websiteTopic = dms.getTopic("uri", new SimpleValue(siteUri));
        if (websiteTopic == null) {
            logger.info("Creating Kiezatlas Website \"" + siteName + " with siteUri=\"" + siteUri + "\"");
            websiteTopic = dms.createTopic(new TopicModel(siteUri, WEBSITE, new ChildTopicsModel()
                .put("ka2.website.title", siteName)));
        } else {
            logger.info("Kiezatlas Website with siteUri=\"" + siteUri + "\" already exists");
        }
        return websiteTopic;
    }

    @POST
    @Path("/add/{geoObjectId}/{siteId}")
    @Transactional
    @Override
    public Association addGeoObjectToWebsite(@PathParam("geoObjectId") long geoObjectId, @PathParam("siteId") long siteId) {
        Topic geoObject = dms.getTopic(geoObjectId);
        Association relation = null;
        if (!hasSiteAssociation(geoObject, siteId)) {
            logger.info("Adding Geo Object \"" + geoObject.getSimpleValue() + "\" to Site Topic: " + siteId);
            relation = dms.createAssociation(new AssociationModel("dm4.core.association",
                new TopicRoleModel(geoObjectId, "dm4.core.default"), new TopicRoleModel(siteId, "dm4.core.default")));
        } else {
            logger.info("Skipping adding Topic to Site, Association already EXISTS");
        }
        return relation;
    }

    @GET
    @Path("/geomap/{geomap_id}")
    @Override
    public Topic getWebsite(@PathParam("geomap_id") long geomapId) {
        try {
            return dms.getTopic(geomapId).getRelatedTopic(WEBSITE_GEOMAP, ROLE_TYPE_WEBSITE, ROLE_TYPE_GEOMAP,
                WEBSITE);
        } catch (Exception e) {
            throw new RuntimeException("Finding the geomap's website topic failed (geomapId=" + geomapId + ")", e);
        }
    }

    @GET
    @Path("/{website_id}/facets")
    @Override
    public ResultList<RelatedTopic> getFacetTypes(@PathParam("website_id") long websiteId) {
        try {
            return dms.getTopic(websiteId).getRelatedTopics(WEBSITE_FACET_TYPES, ROLE_TYPE_WEBSITE,
                ROLE_TYPE_FACET_TYPE, "dm4.core.topic_type", 0);
        } catch (Exception e) {
            throw new RuntimeException("Finding the website's facet types failed (websiteId=" + websiteId + ")", e);
        }
    }

    @GET
    @Path("/criteria")
    @Override
    public List<Topic> getAllCriteria() {
        List<Topic> criteria = dms.getTopics("uri", new SimpleValue("ka2.criteria.*"));
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
        return dms.getTopic(categoryId).getRelatedTopics("dm4.core.aggregation", "dm4.core.child", "dm4.core.parent",
                GEO_OBJECT, 0).getItems();
    }

    /** Used by famportal editorial tool. */
    @GET
    @Path("/geoobject")
    @Override
    public GeoObjects searchGeoObjectNames(@QueryParam("search") String searchTerm, @QueryParam("clock") long clock) {
        GeoObjects result = new GeoObjects(clock);
        for (Topic geoObjectName : dms.searchTopics("*" + searchTerm + "*", GEO_OBJECT_NAME)) {
            result.add(getGeoObject(geoObjectName));
        }
        return result;
    }

    @GET
    @Path("/einrichtungen/missing-bezirk")
    public List<SocialInstitutionObject> getInstitutionsMissingBezirk() {
        logger.info("Fetching Social Institutions without a BEZIRK assignment");
        List<SocialInstitutionObject> results = new ArrayList<SocialInstitutionObject>();
        List<Topic> topics = dms.getTopics("type_uri", new SimpleValue("ka2.geo_object"));
        for (Topic geoObject : topics) {
            SocialInstitutionObject institution = new SocialInstitutionObject();
            institution.setName(geoObject.getSimpleValue().toString()); // ### load child topic "ka2.geo_object.name"
            Topic bezirk = getFacettedBezirkChildTopic(geoObject);
            if (bezirk == null) {
                logger.warning("> Bezirks Relation is missing for " + geoObject.getSimpleValue());
                institution.setCreated(timeService.getCreationTime(geoObject.getId()));
                institution.setLastModified(timeService.getModificationTime(geoObject.getId()));
                institution.setUri(geoObject.getUri());
                results.add(institution);
            }
        }
        return results;
    }

    @GET
    @Path("/einrichtungen/missing-link/")
    public List<SocialInstitutionObject> getInstitutionsMissingBacklink() {
        logger.info("Fetching Social Institutions without a BEZIRK & BEZIRKSREGION assignment");
        List<SocialInstitutionObject> results = new ArrayList<SocialInstitutionObject>();
        List<Topic> topics = dms.getTopics("type_uri", new SimpleValue("ka2.geo_object"));
        for (Topic geoObject : topics) {
            SocialInstitutionObject institution = new SocialInstitutionObject();
            institution.setName(geoObject.getSimpleValue().toString()); // ### load child topic "ka2.geo_object.name"
            Topic bezirk = getFacettedBezirkChildTopic(geoObject);
            Topic bezirksregion = getFacettedBezirksregionChildTopic(geoObject);
            if (bezirk == null && bezirksregion == null) {
                logger.warning("> Bezirks and Bezirksregion Relation is missing for " + geoObject.getSimpleValue());
                institution.setCreated(timeService.getCreationTime(geoObject.getId()));
                institution.setLastModified(timeService.getModificationTime(geoObject.getId()));
                institution.setUri(geoObject.getUri());
                results.add(institution);
            }
        }
        return results;
    }

    // Note: Fetch and build up administrative response objects
    @GET
    @Path("/einrichtungen/{bezirksTopicId}")
    public List<SocialInstitutionObject> getSiteInstitutions(@PathParam("bezirksTopicId") long topicId) {
        logger.info("Loading Social Institutions related to super Topic " + topicId);
        List<SocialInstitutionObject> results = new ArrayList<SocialInstitutionObject>();
        Topic superTopic = dms.getTopic(topicId);
        ResultList<RelatedTopic> geoObjects = superTopic.getRelatedTopics("dm4.core.aggregation",
            "dm4.core.child", "dm4.core.parent", "ka2.geo_object", 0);
        int missingMailboxes = 0;
        for (RelatedTopic geoObject : geoObjects) {
            SocialInstitutionObject institution = new SocialInstitutionObject();
            geoObject.loadChildTopics("dm4.contacts.address");
            RelatedTopic address = geoObject.getChildTopics().getTopic("dm4.contacts.address");
            Topic contactTopic = getFacettedContactChildTopic(geoObject);
            String contactValue = "k.A.";
            boolean missesMailbox = false;
            if (contactTopic != null) {
                contactTopic.loadChildTopics();
                Topic eMail = contactTopic.getChildTopics().getTopic("ka2.kontakt.email");
                if (eMail != null && !eMail.getSimpleValue().toString().isEmpty()) {
                    contactValue = eMail.getSimpleValue().toString();
                } else {
                    missesMailbox = true;
                    Topic phone = contactTopic.getChildTopics().getTopic("ka2.kontakt.telefon");
                    if (phone != null && !phone.getSimpleValue().toString().isEmpty()) {
                        contactValue = phone.getSimpleValue().toString();
                    }
                }
            }
            GeoCoordinate coordinates = geomapsService.getGeoCoordinate(address);
            if (coordinates != null) {
                institution.setGeoCoordinate(coordinates);
            } else {
                institution.addClassification("no-coordinates");
                logger.info("> Geo Coordinates unavailable on \"" + geoObject.getSimpleValue().toString() + "\", Address: " + address.getSimpleValue());
            }
            if (missesMailbox) {
                // logger.info("> Identified missing Email Address at " + geoObject.getSimpleValue().toString());
                missingMailboxes++;
                institution.addClassification("no-email");
            }
            Topic bezirksregion = getFacettedBezirksregionChildTopic(geoObject);
            if (bezirksregion == null) {
                logger.warning("> No Bezirksregion set on \"" + geoObject.getSimpleValue().toString() + "\"");
                institution.addClassification("no-bezirksregion");
            }
            institution.setName(geoObject.getSimpleValue().toString()); // ### load child topic "ka2.geo_object.name"
            institution.setContact(contactValue);
            institution.setAddress(address.getSimpleValue().toString());
            institution.setCreated(timeService.getCreationTime(geoObject.getId()));
            institution.setLastModified(timeService.getModificationTime(geoObject.getId()));
            results.add(institution);
        }
        logger.info("> Identified " + missingMailboxes +" Geo Objects without an "
                + "Email-Address in the group of \"" + superTopic.getSimpleValue().toString()
            + "\" (" + geoObjects.getSize() + ")");
        return results;
    }

    @PUT
    @Path("/geoobject/attribution/{topicId}/{owner}")
    @Transactional
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createGeoObjectAttribution(@PathParam("topicId") long id,
            @PathParam("owner") String owner, String key) {
        Topic geoObject = dms.getTopic(id);
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
    }

    @GET
    @Path("/category/objects")
    @Override
    public GroupedGeoObjects searchCategories(@QueryParam("search") String searchTerm,
            @QueryParam("clock") long clock) {
        GroupedGeoObjects result = new GroupedGeoObjects(clock);
        for (Topic criteria : getAllCriteria()) {
            for (Topic category : dms.searchTopics("*" + searchTerm + "*", criteria.getUri())) {
                result.add(criteria, category, getGeoObjectsByCategory(category.getId()));
            }
        }
        return result;
    }

    @Override
    public GeoCoordinate getGeoCoordinateByGeoObject(Topic geoObject) {
        Topic addressTopic = geoObject.getChildTopics().getTopic(GEO_OBJECT_ADDRESS);
        if (addressTopic != null) return geomapsService.getGeoCoordinate(addressTopic);
        logger.warning("GeoCoordinate could not be determined becuase no Address is related to Geo Object: "
            + geoObject.getSimpleValue());
        return null;
    }

    /**
     * Returns the Geo Coordinate topic (including its child topics) of a geo-facetted topic (e.g. an Address),
     * or <code>null</code> if no geo coordinate is stored.
     */
    @Override
    public Topic getGeoCoordinateTopic(Topic addressTopic) {
        Topic geoCoordTopic = facetsService.getFacet(addressTopic, "dm4.geomaps.geo_coordinate_facet");
        return geoCoordTopic != null ? geoCoordTopic.loadChildTopics() : null;
    }

    @Override
    public String getGeoObjectAttribution(Topic geoObject) {
        return (String) geoObject.getProperty(GEO_OBJECT_OWNER_PROPERTY) + ":"
            + (String) geoObject.getProperty(GEO_OBJECT_KEYWORD_PROPERTY);
    }

    /** This facet depends/just exists after installation of the dm4-kiezatlas-etl plugin. */
    @Override
    public Topic getFacettedBezirksregionChildTopic(Topic facettedTopic) {
        // Note: Untested
        return facetsService.getFacet(facettedTopic, "ka2.bezirksregion.facet");
    }

    /** This facet depends/just exists after installation of the dm4-kiezatlas-etl plugin. */
    @Override
    public Topic getImageFileFacetByGeoObject(Topic geoObject) {
        return facetsService.getFacet(geoObject, "ka2.bild.facet");
    }

    @Override
    public ResultList<RelatedTopic> getParentRelatedAggregatedGeoObjects(Topic bezirksFacet) {
        return bezirksFacet.getRelatedTopics("dm4.core.aggregation", "dm4.core.child",
            "dm4.core.parent", "ka2.geo_object", 0);
    }

    @Override
    public void updateImageFileFacet(Topic geoObject, String imageFilePath) {
        facetsService.updateFacet(geoObject, "ka2.bild.facet", new FacetValue("ka2.bild.pfad").put(imageFilePath));
    }

    // ********************************
    // *** Listener Implementations ***
    // ********************************



    @Override
    public void preSendTopic(Topic topic) {
        if (!topic.getTypeUri().equals(GEO_OBJECT)) {
            return;
        }
        //
        ResultList<RelatedTopic> facetTypes = getFacetTypes();
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
        ResultList<RelatedTopic> facetTypes = getFacetTypes();
        if (facetTypes == null) {
            return;
        }
        //
        updateFacets(topic, facetTypes, newModel);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    private Topic getFacettedContactChildTopic(Topic facettedTopic) {
        return facettedTopic.getRelatedTopic("dm4.core.composition", "dm4.core.parent",
            "dm4.core.child", "ka2.kontakt");
    }

    private Topic getFacettedBezirkChildTopic(Topic facettedTopic) {
        return facettedTopic.getRelatedTopic("dm4.core.aggregation", "dm4.core.parent",
            "dm4.core.child", "ka2.bezirk");
    }

    private boolean hasSiteAssociation(Topic geoObject, long siteId) {
        ResultList<RelatedTopic> sites = geoObject.getRelatedTopics("dm4.core.association", "dm4.core.default",
            "dm4.core.default", WEBSITE, 0);
        for (RelatedTopic site : sites) {
            if (site.getId() == siteId) return true;
        }
        return false;
    }

    // === Enrich with facets ===

    private void enrichWithFacets(Topic geoObject, ResultList<RelatedTopic> facetTypes) {
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
        ResultList<RelatedTopic> facetValues = facetsService.getFacets(geoObject, facetTypeUri);
        logger.info("### Enriching geo object " + geoObject.getId() + " with its \"" + facetTypeUri +
            "\" facet values (" + facetValues + ")");
        String childTypeUri = getChildTypeUri(facetTypeUri);
        // Note: we set the facet values at once (using put()) instead of iterating (and using add()) as after an geo
        // object update request the facet values are already set. Using add() would result in having the values twice.
        geoObject.getChildTopics().getModel().put(childTypeUri, DeepaMehtaUtils.toTopicModels(facetValues));
    }



    // === Update facets ===

    private void updateFacets(Topic geoObject, ResultList<RelatedTopic> facetTypes, TopicModel newModel) {
        for (Topic facetType : facetTypes) {
            String facetTypeUri = facetType.getUri();
            String childTypeUri = getChildTypeUri(facetTypeUri);
            if (!isMultiFacet(facetTypeUri)) {
                TopicModel facetValue = newModel.getChildTopicsModel().getTopic(childTypeUri);
                logger.info("### Storing facet of type \"" + facetTypeUri + "\" for geo object " + geoObject.getId() +
                    " (facetValue=" + facetValue + ")");
                FacetValue value = new FacetValue(childTypeUri).put(facetValue);
                facetsService.updateFacet(geoObject, facetTypeUri, value);
            } else {
                List<RelatedTopicModel> facetValues = newModel.getChildTopicsModel().getTopics(childTypeUri);
                logger.info("### Storing facets of type \"" + facetTypeUri + "\" for geo object " + geoObject.getId() +
                    " (facetValues=" + facetValues + ")");
                FacetValue value = new FacetValue(childTypeUri).put(facetValues);
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
    private ResultList<RelatedTopic> getFacetTypes() {
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

    private Topic getGeoObject(Topic geoObjectName) {
        return geoObjectName.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent",
            GEO_OBJECT);   // ### TODO: Core API should provide type-driven navigation
    }

    private boolean isGeomap(long topicmapId) {
        Topic topicmap = dms.getTopic(topicmapId);
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
        return dms.getTopicType(facetTypeUri).getAssocDefs().iterator().next();
    }
}
