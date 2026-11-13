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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.diagram.test.po.DiagramEditPage;
import com.xwiki.diagram.test.po.DiagramEditor;
import com.xwiki.diagram.test.po.DiagramMacro;
import com.xwiki.diagram.test.po.DiagramMacroPage;
import com.xwiki.diagram.test.po.DiagramViewPage;
import com.xwiki.diagram.test.po.DiagramViewer;
import com.xwiki.diagram.test.po.InlineDiagramEditPage;
import com.xwiki.diagram.test.util.DockerDiagramTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the macros that display diagrams in wiki pages: Diagram, Display, Embed Diagram and Inline Diagram.
 * The macros are inserted using the wiki syntax.
 *
 * @version $Id$
 * @since 2.1.4
 */
@UITest
class DiagramMacrosIT
{
    private static final DocumentReference DIAGRAM =
        new DocumentReference("xwiki", List.of("DiagramMacrosIT", "MacroDiagram"), "WebHome");

    private static final String DIAGRAM_REFERENCE = "DiagramMacrosIT.MacroDiagram.WebHome";

    private static final String DIAGRAM_TITLE = "Macro Diagram";

    @BeforeAll
    void setup(TestUtils setup) throws Exception
    {
        setup.createAdminUser();
        setup.loginAsSuperAdmin();
        setup.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);
        setup.loginAsAdmin();
        // Precondition: a diagram page is already created with the Diagram Application.
        DockerDiagramTestUtils.createDiagram(setup, DIAGRAM,
            DockerDiagramTestUtils.getResourceContent(List.of("diagramEditor", "threeShapes.drawio")), DIAGRAM_TITLE);
    }

    @BeforeEach
    void beforeEach(TestUtils setup)
    {
        setup.loginAsAdmin();
    }

    /**
     * Manual test 16: Diagram Macro.
     */
    @Test
    @Order(1)
    void diagramMacro(TestUtils setup, TestReference testReference)
    {
        setup.createPage(testReference, String.format("{{diagram reference=\"%s\"/}}", DIAGRAM_REFERENCE),
            "Diagram Macro");

        // The diagram is displayed.
        DiagramMacro diagram = new DiagramMacroPage().getDiagram(0);
        assertTrue(diagram.hasThumbnail());
        assertTrue(diagram.hasSvg());
        assertEquals(DIAGRAM_TITLE, diagram.getCaption());

        // The diagram title links to the diagram page.
        DiagramViewPage diagramPage = diagram.clickTitle();
        assertEquals(DIAGRAM_TITLE, diagramPage.getDocumentTitle());
        assertEquals(3, diagramPage.getDiagram().getModel().getVertices().size());

        // The "Edit diagram" button opens the diagram editor.
        setup.gotoPage(testReference);
        DiagramEditPage editPage = new DiagramMacroPage().getDiagram(0).clickEdit();
        assertEquals(3, editPage.getDiagramEditor().getShapeCount());
        assertEquals("xwiki:" + DIAGRAM_REFERENCE, editPage.getMetaDataValue("reference"));
        editPage.clickCancel();
    }

    /**
     * Manual test 18: Display Macro.
     */
    @Test
    @Order(2)
    void displayMacro(TestUtils setup, TestReference testReference)
    {
        setup.createPage(testReference, String.format("{{display reference=\"%s\"/}}", DIAGRAM_REFERENCE),
            "Display Macro");

        DiagramViewer diagram = new DiagramViewPage().getDiagram();
        assertTrue(diagram.isRendered());
        assertEquals(3, diagram.getModel().getVertices().size());
    }

    /**
     * Manual test 21: Embed diagram macro.
     */
    @Test
    @Order(3)
    void embedDiagramMacro(TestUtils setup, TestReference testReference)
    {
        setup.createPage(testReference,
            String.format("{{embedDiagram diagramSource=\"%s\"/}}", DIAGRAM_REFERENCE), "Embed Diagram Macro");

        // The diagram is displayed.
        DiagramViewer diagram = new DiagramViewPage().getDiagram();
        assertTrue(diagram.isRendered());
        assertEquals(3, diagram.getModel().getVertices().size());

        // The tool bar is displayed when moving the mouse over the diagram.
        diagram.hover();
        List<String> toolbar = diagram.getToolbarButtons();
        assertTrue(toolbar.containsAll(List.of("Zoom In", "Zoom Out", "Fullscreen")), toolbar.toString());
    }

    /**
     * Manual test 42: Inline diagram macro.
     */
    @Test
    @Order(4)
    void inlineDiagramMacro(TestUtils setup, TestReference testReference)
    {
        setup.createPage(testReference, "{{inlineDiagram diagramName=\"inlineDiagramTest\"/}}",
            "Inline Diagram Macro");

        // The link to create the inline diagram opens the diagram editor.
        InlineDiagramEditPage inlineEditor = new DiagramMacroPage().clickCreateInlineDiagram();
        DiagramEditor editor = inlineEditor.getDiagramEditor();
        editor.addShape("General", 0);
        editor.addShape("General", 4);
        inlineEditor.clickSave();
        inlineEditor.clickGoBack();

        // The diagram is displayed in the page.
        DiagramViewer diagram = new DiagramViewPage().getDiagram();
        assertTrue(diagram.isRendered());
        assertEquals(2, diagram.getModel().getVertices().size());
    }
}
