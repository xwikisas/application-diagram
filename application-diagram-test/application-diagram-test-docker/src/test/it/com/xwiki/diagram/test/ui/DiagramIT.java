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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import com.xwiki.diagram.test.po.DiagramMacro;
import com.xwiki.diagram.test.po.DiagramMacroPage;

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
    void createDiagram(TestUtils setup)
    {

        ViewPage vp = setup.createPage("Main", "DiagramTest", getMacroContent("diagram.xml"), "DiagramTest");
        ObjectEditPage objectEditPage = vp.editObjects();
        objectEditPage.addObject("Diagram.DiagramClass");
        objectEditPage.clickSaveAndView();

        InlinePage inlinePage = vp.editInline();
        inlinePage.clickSaveAndView();
    }

    @Test
    @Order(2)
    void createPageWithDiagrams(TestUtils setup, TestReference testReference)
    {
        setup.createPage(testReference, getMacroContent("diagramPage.vm"), "PageWithDiagramsTest");
        DiagramMacroPage page = new DiagramMacroPage();
        DiagramMacro diagram0 = page.getDiagram(0);

        WebElement button = page.getCreateButton(0);
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
