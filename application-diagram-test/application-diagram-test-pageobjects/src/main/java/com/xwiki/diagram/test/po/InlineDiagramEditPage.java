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
package com.xwiki.diagram.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The editor of an inline diagram (a diagram stored as an attachment of a regular page, created with the Inline
 * Diagram macro).
 *
 * @version $Id$
 * @since 2.1.4
 */
public class InlineDiagramEditPage extends BaseElement
{
    private final DiagramEditor editor;

    /**
     * Creates a new instance and waits for the diagram editor to be ready.
     */
    public InlineDiagramEditPage()
    {
        this.editor = new DiagramEditor();
    }

    /**
     * @return the diagram editor
     */
    public DiagramEditor getDiagramEditor()
    {
        return this.editor;
    }

    /**
     * Saves the inline diagram, using the Save button from the editor menu bar.
     */
    public void clickSave()
    {
        clickMenuBarButton("Save");
        waitForNotificationSuccessMessage("Diagram created.");
    }

    /**
     * Goes back to the page that holds the inline diagram, using the Go Back button from the editor menu bar.
     *
     * @return the page that holds the inline diagram
     */
    public ViewPage clickGoBack()
    {
        getDriver().addPageNotYetReloadedMarker();
        clickMenuBarButton("Go Back");
        getDriver().waitUntilPageIsReloaded();
        return new ViewPage();
    }

    private void clickMenuBarButton(String label)
    {
        getDriver().findElement(By.xpath(String.format(
            "//div[contains(@class, 'geMenubar')]/a[contains(@class, 'diagram-custom-menubar-button')"
                + " and normalize-space(.) = '%s']", label))).click();
    }
}
