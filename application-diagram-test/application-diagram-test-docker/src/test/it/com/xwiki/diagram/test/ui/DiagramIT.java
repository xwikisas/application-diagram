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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import com.xwiki.diagram.test.po.DiagramMacro;
import com.xwiki.diagram.test.po.DiagramMacroPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest
public class DiagramIT
{
    @BeforeEach
    void beforeEach(TestUtils testUtils)
    {

        testUtils.loginAsSuperAdmin();
        DocumentReference documentReference = new DocumentReference("xwiki", "Main", "DiagramTest");
        DocumentReference documentReference2 = new DocumentReference("xwiki", "Main", "DiagramTestRenamed");
        testUtils.deletePage(documentReference);
        testUtils.deletePage(documentReference2);
    }

    @Test
    @Order(1)
    void diagramMacroTest(TestUtils setup, TestReference testReference)
    {

        createDiagram(setup);
        setup.createPage("Main", "PageWithNoDiagram", "normal page with no diagrams", "PageWithNoDiagram");

        setup.createPage(testReference, getMacroContent("diagramPage.vm"), "PageWithDiagramsTest");
        DiagramMacroPage page = new DiagramMacroPage();

        DiagramMacro d0 = page.getDiagram(0);
        assertTrue(d0.isCreateButton());
        assertTrue(d0.getCreateButtonURL().contains("template=Diagram"));

        DiagramMacro d1 = page.getDiagram(1);
        assertTrue(d1.isCreateButton());
        assertTrue(d1.getCreateButtonURL().contains("template=Diagram"));

        DiagramMacro d2 = page.getDiagram(2);
        assertTrue(d2.isCachedDiagram());
        assertTrue(d2.hasSVG());
        assertTrue(d2.getDiagramEditLink().contains("DiagramTest"));
        assertEquals("DiagramTest", d2.getDiagramName());
        assertTrue(d2.getDiagramReference().contains("DiagramTest"));

        DiagramMacro d3 = page.getDiagram(3);
        assertTrue(d3.isCachedDiagram());
        assertEquals("PageWithNoDiagram", d3.getDiagramName());
        assertTrue(d3.hasWarningMessage());
        assertEquals("The specified page is not a diagram.", d3.getWarningMessage());

        DiagramMacro d4 = page.getDiagram(4);
        assertTrue(d4.isCachedDiagram());
        assertEquals("PageWithNoDiagram", d4.getDiagramName());
        assertTrue(d4.hasWarningMessage());
        assertEquals("The specified page is not a diagram.", d4.getWarningMessage());

        DiagramMacro d5 = page.getDiagram(5);
        assertTrue(d5.isNotCached());
        assertTrue(d5.getModelReference().contains("DiagramTest"));
    }

    @Test
    @Order(2)
    void renamedDiagramTest(TestUtils setup)
    {

        createDiagram(setup);
        DocumentReference docRef = new DocumentReference("xwiki", "Main", "PageWithDiagramsTestRenamed");
        setup.createPage(docRef, getMacroContent("renamedDiagramPage.vm"), "PageWithDiagramsTestRenamed");

        DocumentReference documentReference = new DocumentReference("xwiki", "Main", "DiagramTest");

        DiagramMacroPage page = new DiagramMacroPage();

        DiagramMacro d0 = page.getDiagram(0);
        assertTrue(d0.isCachedDiagram());
        assertTrue(d0.hasSVG());
        assertTrue(d0.getDiagramEditLink().contains("DiagramTest"));
        assertEquals("DiagramTest", d0.getDiagramName());
        assertTrue(d0.getDiagramReference().contains("DiagramTest"));

        DiagramMacro d1 = page.getDiagram(1);
        assertTrue(d1.isNotCached());
        assertTrue(d1.getModelReference().contains("DiagramTest"));

        setup.gotoPage(documentReference);
        ViewPage viewPage = setup.gotoPage(documentReference);
        RenamePage renamePage = viewPage.rename();
        renamePage.getDocumentPicker().setTitle("DiagramTestRenamed");
        renamePage.clickRenameButton();

        setup.gotoPage(docRef);
        DiagramMacroPage renamedPage = new DiagramMacroPage();

        DiagramMacro d2 = renamedPage.getDiagram(0);
        assertTrue(d2.isCachedDiagram());
        assertTrue(d2.hasSVG());
        System.out.println(d2.getDiagramEditLink());
        assertTrue(d2.getDiagramEditLink().contains("DiagramTestRenamed"));
        assertEquals("DiagramTestRenamed", d2.getDiagramName());
        assertTrue(d2.getDiagramReference().contains("DiagramTestRenamed"));

        DiagramMacro d3 = renamedPage.getDiagram(1);
        assertTrue(d3.isNotCached());
        System.out.println(d3.getModelReference());
        assertTrue(d3.getModelReference().contains("xwiki:Main.DiagramTestRenamed"));
    }

    private void createDiagram(TestUtils setup)
    {

        ViewPage vp = setup.createPage("Main", "DiagramTest", getMacroContent("diagram.xml"), "DiagramTest");
        ObjectEditPage objectEditPage = vp.editObjects();
        objectEditPage.addObject("Diagram.DiagramClass");
        objectEditPage.clickSaveAndView();

        InlinePage inlinePage = vp.editInline();
        inlinePage.clickSaveAndView();
    }

    private String getMacroContent(String filename)
    {
        try (InputStream inputStream = getClass().getResourceAsStream("/diagramContent/" + filename)) {
            if (inputStream == null) {
                throw new RuntimeException("Failed to load " + filename + " from resources.");
            }

            return new BufferedReader(new InputStreamReader(inputStream)).lines()
                .filter(line -> !line.trim().startsWith("##")).collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read macro file: " + filename, e);
        }
    }
}
