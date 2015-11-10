package de.kiezatlas.migrations;

import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Topic;
import de.deepamehta.plugins.accesscontrol.AccessControlService;
import de.deepamehta.plugins.workspaces.WorkspacesService;


public class Migration3 extends Migration {

    @Inject
    private WorkspacesService workspaceService;

    @Inject
    private AccessControlService accessControlService;

    @Override
    public void run() {
        // Assign all our topic types to the "System" workspace so "admin" can edit these definitions
        Topic system = workspaceService.getWorkspace(accessControlService.SYSTEM_WORKSPACE_URI);
        TopicType geoObject = dms.getTopicType("ka2.geo_object");
        TopicType geoObjectName = dms.getTopicType("ka2.geo_object.name");
        TopicType website = dms.getTopicType("ka2.website");
        TopicType websiteTitle = dms.getTopicType("ka2.website.title");
        workspaceService.assignTypeToWorkspace(geoObject, system.getId());
        workspaceService.assignTypeToWorkspace(geoObjectName, system.getId());
        workspaceService.assignTypeToWorkspace(website, system.getId());
        workspaceService.assignTypeToWorkspace(websiteTitle, system.getId());
    }
}
