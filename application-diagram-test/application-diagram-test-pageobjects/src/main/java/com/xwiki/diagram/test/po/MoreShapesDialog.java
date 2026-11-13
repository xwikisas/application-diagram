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

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The "More Shapes" dialog, used to choose the shape libraries displayed in the side bar of the diagram editor.
 *
 * @version $Id$
 * @since 2.1.4
 */
public class MoreShapesDialog extends BaseElement
{
    private final WebElement container;

    /**
     * Creates a new instance and waits for the dialog to be displayed.
     */
    public MoreShapesDialog()
    {
        getDriver().waitUntilElementIsVisible(By.cssSelector(".geDialog input[type='checkbox']"));
        this.container = getDriver().findElement(By.cssSelector(".geDialog"));
    }

    /**
     * @return the names of the available shape libraries
     */
    public List<String> getLibraries()
    {
        return getDriver().findElementsWithoutWaiting(this.container, By.cssSelector("input[type='checkbox']")).stream()
            .map(checkbox -> checkbox.findElement(By.xpath("..")).getText().trim()).collect(Collectors.toList());
    }

    /**
     * Enables or disables a shape library.
     *
     * @param library the name of the shape library, as displayed in the dialog (e.g. "Cisco", "BPMN 2.0")
     * @param enabled whether the library should be displayed in the side bar
     */
    public void setLibraryEnabled(String library, boolean enabled)
    {
        WebElement checkbox = this.container.findElement(
            By.xpath(String.format(".//input[@type = 'checkbox'][normalize-space(..) = '%s']", library)));
        if (checkbox.isSelected() != enabled) {
            getDriver().scrollTo(checkbox);
            checkbox.click();
        }
    }

    /**
     * Applies the changes and closes the dialog.
     */
    public void apply()
    {
        DiagramEditor.clickDialogButton(this.container, "Apply");
        getDriver().waitUntilElementDisappears(By.cssSelector(".geDialog"));
    }
}
