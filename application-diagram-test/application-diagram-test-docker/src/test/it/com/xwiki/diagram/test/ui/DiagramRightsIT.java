/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.diagram.test.ui;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.diagram.test.po.DiagramMacroPage;
import com.xwiki.diagram.test.po.DiagramViewPage;
import com.xwiki.diagram.test.po.DiagramViewer;
import com.xwiki.diagram.test.util.DockerDiagramTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests making sure that the diagram edit actions are displayed only to the users that can edit the diagram.
 *
 * @version $Id$
 * @since 2.1.4
 */
@UITest
class DiagramRightsIT
{
    private static final String SPACE = "DiagramRightsIT";

    private static final DocumentReference DIAGRAM =
        new DocumentReference("xwiki", List.of(SPACE, "RestrictedDiagram"), "WebHome");

    private static final String DIAGRAM_REFERENCE = SPACE + ".RestrictedDiagram.WebHome";

    private static final DocumentReference DIAGRAM_MACRO_PAGE =
        new DocumentReference("xwiki", SPACE, "DiagramMacroPage");

    private static final DocumentReference EDITABLE_EMBED_PAGE =
        new DocumentReference("xwiki", SPACE, "EditableEmbedPage");

    private static final DocumentReference READ_ONLY_EMBED_PAGE =
        new DocumentReference("xwiki", SPACE, "ReadOnlyEmbedPage");

    /**
     * A user that can view the diagram but not edit it.
     */
    private static final String READER = "DiagramReader";

    private static final String EDIT = "Edit";

    private static final List<String> VIEW_ACTIONS = List.of("Zoom In", "Zoom Out", "Fullscreen");

    @BeforeAll
    void setup(TestUtils setup) throws Exception
    {
        setup.createAdminUser();
        setup.loginAsSuperAdmin();
        setup.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);
        setup.createUser(READER, READER, null);
        setup.loginAsAdmin();

        DockerDiagramTestUtils.createDiagram(setup, DIAGRAM,
            DockerDiagramTestUtils.getResourceContent(List.of("diagramEditor", "threeShapes.drawio")),
            "Restricted Diagram");
        // The reader can view the diagram but not edit it.
        setup.setRights(DIAGRAM, "", "XWiki." + READER, "edit", false);

        setup.createPage(DIAGRAM_MACRO_PAGE, String.format("{{diagram reference=\"%s\"/}}", DIAGRAM_REFERENCE),
            "Diagram Macro");
        setup.createPage(EDITABLE_EMBED_PAGE,
            String.format("{{embedDiagram diagramSource=\"%s\" enableEdit=\"true\"/}}", DIAGRAM_REFERENCE),
            "Editable Embed");
        setup.createPage(READ_ONLY_EMBED_PAGE,
            String.format("{{embedDiagram diagramSource=\"%s\"/}}", DIAGRAM_REFERENCE), "Read-only Embed");
    }

    /**
     * A user with edit right on the diagram can edit it from the diagram page and from the macros that allow it.
     */
    @Test
    @Order(1)
    void editActionsWithEditRight(TestUtils setup)
    {
        setup.loginAsAdmin();

        assertTrue(getToolbarButtons(setup, DIAGRAM).contains(EDIT));

        setup.gotoPage(DIAGRAM_MACRO_PAGE);
        assertTrue(new DiagramMacroPage().getDiagram(0).hasEditButton());

        assertTrue(getToolbarButtons(setup, EDITABLE_EMBED_PAGE).contains(EDIT));
        // The Embed Diagram macro doesn't allow editing the diagram by default.
        List<String> toolbar = getToolbarButtons(setup, READ_ONLY_EMBED_PAGE);
        assertFalse(toolbar.contains(EDIT), toolbar.toString());
        assertTrue(toolbar.containsAll(VIEW_ACTIONS), toolbar.toString());
    }

    /**
     * A user without edit right on the diagram can view it, but no edit action is displayed, not even when the Embed
     * Diagram macro allows editing.
     */
    @Test
    @Order(2)
    void noEditActionsWithoutEditRight(TestUtils setup)
    {
        setup.login(READER, READER);

        // The diagram page.
        setup.gotoPage(DIAGRAM);
        DiagramViewer diagram = new DiagramViewPage().getDiagram();
        assertTrue(diagram.isRendered());
        assertEquals(3, diagram.getModel().getVertices().size());
        List<String> toolbar = getToolbarButtons(setup, DIAGRAM);
        assertFalse(toolbar.contains(EDIT), toolbar.toString());
        assertTrue(toolbar.containsAll(VIEW_ACTIONS), toolbar.toString());

        // The Diagram macro.
        setup.gotoPage(DIAGRAM_MACRO_PAGE);
        DiagramMacroPage macroPage = new DiagramMacroPage();
        assertTrue(macroPage.getDiagram(0).hasSvg());
        assertFalse(macroPage.getDiagram(0).hasEditButton());

        // The Embed Diagram macros.
        toolbar = getToolbarButtons(setup, EDITABLE_EMBED_PAGE);
        assertFalse(toolbar.contains(EDIT), toolbar.toString());
        assertTrue(toolbar.containsAll(VIEW_ACTIONS), toolbar.toString());
        toolbar = getToolbarButtons(setup, READ_ONLY_EMBED_PAGE);
        assertFalse(toolbar.contains(EDIT), toolbar.toString());

        setup.loginAsAdmin();
    }

    private List<String> getToolbarButtons(TestUtils setup, DocumentReference page)
    {
        setup.gotoPage(page);
        DiagramViewer diagram = new DiagramViewPage().getDiagram();
        diagram.hover();
        return diagram.getToolbarButtons();
    }
}
