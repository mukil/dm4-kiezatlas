package de.kiezatlas.migrations;

import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.service.accesscontrol.SharingMode;
import de.deepamehta.accesscontrol.AccessControlService;
import de.deepamehta.workspaces.WorkspacesService;

/**
 * Introduces the Public "Kiezatlas" Workspace (as of 4.7).
 * Assigns all our topic types to the "Kiezatlas" workspace so members can edit these type definitions
 * Home of all Kiezatlas related Topic Types.
 * Also home of all Facet, Category Types and Categories introduced with dm4-kiezatlas-etl.
 * */

public class Migration3 extends Migration {

    static final String KIEZATLAS_WORKSPACE_NAME = "Kiezatlas";
    static final String KIEZATLAS_WORKSPACE_URI = "de.kiezatlas.workspace";
    static final SharingMode KIEZATLAS_WORKSPACE_SHARING_MODE = SharingMode.PUBLIC;

    @Inject
    private WorkspacesService workspaceService;

    @Inject
    private AccessControlService accessControlService;

    @Override
    public void run() {
        Topic kiezatlas = workspaceService.createWorkspace(KIEZATLAS_WORKSPACE_NAME, KIEZATLAS_WORKSPACE_URI,
                KIEZATLAS_WORKSPACE_SHARING_MODE);
        accessControlService.setWorkspaceOwner(kiezatlas, "admin");
        TopicType geoObject = dm4.getTopicType("ka2.geo_object");
        TopicType geoObjectName = dm4.getTopicType("ka2.geo_object.name");
        TopicType website = dm4.getTopicType("ka2.website");
        TopicType websiteTitle = dm4.getTopicType("ka2.website.title");
        workspaceService.assignTypeToWorkspace(geoObject, kiezatlas.getId());
        workspaceService.assignTypeToWorkspace(geoObjectName, kiezatlas.getId());
        workspaceService.assignTypeToWorkspace(website, kiezatlas.getId());
        workspaceService.assignTypeToWorkspace(websiteTitle, kiezatlas.getId());
    }
}
