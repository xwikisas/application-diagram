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
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import com.xwiki.diagram.test.po.DiagramMacro;
import com.xwiki.diagram.test.po.DiagramMacroPage;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@UITest
public class DiagramIT
{
    @BeforeAll
    void beforeAll(TestUtils testUtils)
    {

        testUtils.loginAsSuperAdmin();
        testUtils.deletePage("Main", "DiagramTest");
    }

    @Test
    @Order(1)
    void diagramMacroTest(TestUtils setup, TestReference testReference)
    {
        createDiagram(setup);
        setup.createPage("Main", "PageWithNoDiagram", "normal page with no diagrams", "PageWithNoDiagram");

        setup.createPage(testReference, getMacroContent("diagramPage.vm"), "PageWithDiagramsTest");
        DiagramMacroPage page = new DiagramMacroPage();

        List<DiagramMacro> diagrams = page.getDiagrams();

        assertTrue(diagrams.get(0).isCreateButton());
        assertTrue(diagrams.get(0).getCreateButtonURL().contains("template=Diagram"));

        Assertions.assertTrue(diagrams.get(1).isCreateButton());
        Assertions.assertTrue(diagrams.get(1).getCreateButtonURL().contains("template=Diagram"));

        DiagramMacro d2 = diagrams.get(2);
        assertTrue(d2.isCachedDiagram());
        assertTrue(d2.hasSVG());
        assertEquals("DiagramTest", d2.getDiagramName());
        assertTrue(d2.getDiagramReference().contains("DiagramTest"));

        DiagramMacro d3 = diagrams.get(3);
        assertTrue(d3.isCachedDiagram());
        assertEquals("PageWithNoDiagram", d3.getDiagramName());
        Assertions.assertTrue(d3.hasWarningMessage());
        Assertions.assertEquals("The specified page is not a diagram.", d3.getWarningMessage());

        DiagramMacro d4 = diagrams.get(4);
        Assertions.assertTrue(d4.isCachedDiagram());
        Assertions.assertEquals("PageWithNoDiagram", d4.getDiagramName());
        Assertions.assertTrue(d4.hasWarningMessage());
        Assertions.assertEquals("The specified page is not a diagram.", d4.getWarningMessage());

        DiagramMacro d5 = diagrams.get(5);
        Assertions.assertTrue(d5.isNotCached());
        Assertions.assertTrue(d5.getModelReference().contains("DiagramTest"));
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
