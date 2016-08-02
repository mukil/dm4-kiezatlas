package de.kiezatlas.migrations;

import de.deepamehta.accesscontrol.AccessControlService;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.workspaces.WorkspacesService;
import de.kiezatlas.KiezatlasService;
import static de.kiezatlas.KiezatlasService.KIEZATLAS_WORKSPACE_NAME;
import static de.kiezatlas.KiezatlasService.KIEZATLAS_WORKSPACE_SHARING_MODE;
import static de.kiezatlas.KiezatlasService.KIEZATLAS_WORKSPACE_URI;


/**
 * Introduces the Public "Kiezatlas" Workspace (as of 4.7).
 * Assigns all our topic types to the "Kiezatlas" workspace so members can edit these type definitions
 * Home of all Kiezatlas related Topic Types.
 * Also home of all Facet, Category Types and Categories introduced with dm4-kiezatlas-etl.
 * */

public class Migration3 extends Migration {

    @Inject
    private WorkspacesService workspaceService;

    @Inject
    private AccessControlService accessControlService;

    @Override
    public void run() {
        Topic kiezatlas = workspaceService.createWorkspace(KIEZATLAS_WORKSPACE_NAME, KIEZATLAS_WORKSPACE_URI,
                KIEZATLAS_WORKSPACE_SHARING_MODE);
        accessControlService.setWorkspaceOwner(kiezatlas, AccessControlService.ADMIN_USERNAME);
        TopicType geoObject = dm4.getTopicType(KiezatlasService.GEO_OBJECT);
        TopicType geoObjectName = dm4.getTopicType(KiezatlasService.GEO_OBJECT_NAME);
        TopicType website = dm4.getTopicType(KiezatlasService.WEBSITE);
        TopicType websiteTitle = dm4.getTopicType(KiezatlasService.WEBSITE_TITLE);
        workspaceService.assignTypeToWorkspace(geoObject, kiezatlas.getId());
        workspaceService.assignTypeToWorkspace(geoObjectName, kiezatlas.getId());
        workspaceService.assignTypeToWorkspace(website, kiezatlas.getId());
        workspaceService.assignTypeToWorkspace(websiteTitle, kiezatlas.getId());
    }
}
