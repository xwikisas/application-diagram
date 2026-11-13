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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.editor.EditPage;

/**
 * The edit mode of a diagram page.
 *
 * @version $Id$
 * @since 2.1.4
 */
public class DiagramEditPage extends EditPage
{
    private static final int SAVE_TIMEOUT = 60;

    @FindBy(id = "xwikidoctitleinput")
    private WebElement titleInput;

    private final DiagramEditor editor;

    /**
     * Creates a new instance and waits for the diagram editor to be ready.
     */
    public DiagramEditPage()
    {
        this.editor = new DiagramEditor();
    }

    /**
     * Opens the given diagram page in edit mode.
     *
     * @param reference the diagram page reference
     * @return the diagram edit page
     */
    public static DiagramEditPage gotoPage(EntityReference reference)
    {
        getUtil().gotoPage(reference, "edit");
        return new DiagramEditPage();
    }

    /**
     * @return the diagram editor
     */
    public DiagramEditor getDiagramEditor()
    {
        return this.editor;
    }

    /**
     * @param title the new diagram page title
     */
    public void setTitle(String title)
    {
        this.titleInput.clear();
        this.titleInput.sendKeys(title);
    }

    /**
     * Saves the diagram and waits for the view mode to be loaded. Saving a diagram takes more time than saving a
     * regular page because the diagram is also exported as images that are attached to the page.
     *
     * @return the diagram view page
     */
    @Override
    @SuppressWarnings("unchecked")
    public DiagramViewPage clickSaveAndView()
    {
        getSaveAndViewButton().click();
        getDriver().waitUntilCondition(driver -> driver.getCurrentUrl().contains("/view/"), SAVE_TIMEOUT);
        return new DiagramViewPage();
    }
}
