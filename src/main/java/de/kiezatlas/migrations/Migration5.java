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
 * Home of all Kiezatlas related Topic Types.
 * Also home of all Facet, Category Types and Categories introduced with dm4-kiezatlas-etl.
 * */

public class Migration5 extends Migration {

    static final String REPORTS_WORKSPACE_NAME = "Reporting";
    static final String REPORTS_WORKSPACE_URI = "de.kiezatlas.reporting_ws";
    static final SharingMode REPORTS_WORKSPACE_SHARING_MODE = SharingMode.CONFIDENTIAL;

    @Inject
    private WorkspacesService workspaceService;

    @Inject
    private AccessControlService accessControlService;

    @Override
    public void run() {
        Topic reports = workspaceService.createWorkspace(REPORTS_WORKSPACE_NAME,
                REPORTS_WORKSPACE_URI, REPORTS_WORKSPACE_SHARING_MODE);
        accessControlService.setWorkspaceOwner(reports, "admin");
        //
        TopicType needsUpdate = dms.getTopicType("ka2.geo_object.needs_update");
        workspaceService.assignTypeToWorkspace(needsUpdate, reports.getId());
        //
        TopicType geoObject = dms.getTopicType("ka2.geo_object");
        geoObject.addAssocDef(new AssociationDefinitionModel("dm4.core.composition_def", geoObject.getUri(),
            needsUpdate.getUri(), "dm4.core.one", "dm4.core.one"));
    }
}
