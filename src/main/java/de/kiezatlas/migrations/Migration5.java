package de.kiezatlas.migrations;

import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Migration;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.service.accesscontrol.SharingMode;
import de.deepamehta.plugins.accesscontrol.AccessControlService;
import de.deepamehta.plugins.workspaces.WorkspacesService;

/**
 * Introduces the Public "Reports" Workspace (as of 4.7).
 **/
public class Migration5 extends Migration {

    // Reporting Workspace for instances of type ka2.geo_object.reporting
    static final String REPORTS_WORKSPACE_NAME = "Reporting";
    static final String REPORTS_WORKSPACE_URI = "de.kiezatlas.reporting_ws";
    static final SharingMode REPORTS_WORKSPACE_SHARING_MODE = SharingMode.CONFIDENTIAL;
    //
    static final String KIEZATLAS_WORKSPACE_URI = "de.kiezatlas.workspace";

    @Inject
    private WorkspacesService workspaceService;

    @Inject
    private AccessControlService accessControlService;

    @Override
    public void run() {
        // Create Reporting Workspace
        Topic reports = workspaceService.createWorkspace(REPORTS_WORKSPACE_NAME,
                REPORTS_WORKSPACE_URI, REPORTS_WORKSPACE_SHARING_MODE);
        accessControlService.setWorkspaceOwner(reports, "admin");
        // Assign Types to Kiezatlas Workspace
        Topic kiezatlasWs = workspaceService.getWorkspace(KIEZATLAS_WORKSPACE_URI);
        TopicType needsUpdate = dms.getTopicType("ka2.geo_object.needs_update");
        TopicType reporting = dms.getTopicType("ka2.geo_object.reporting");
        TopicType reportedNote = dms.getTopicType("ka2.geo_object.note_reported");
        workspaceService.assignTypeToWorkspace(needsUpdate, kiezatlasWs.getId());
        workspaceService.assignTypeToWorkspace(reporting, kiezatlasWs.getId());
        workspaceService.assignTypeToWorkspace(reportedNote, kiezatlasWs.getId());
        //
        TopicType geoObject = dms.getTopicType("ka2.geo_object");
        geoObject.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def", geoObject.getUri(),
            reporting.getUri(), "dm4.core.one", "dm4.core.many"));
    }
}
