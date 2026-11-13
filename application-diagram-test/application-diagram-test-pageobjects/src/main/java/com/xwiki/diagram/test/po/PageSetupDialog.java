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

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The File > Page Setup... dialog of the diagram editor.
 *
 * @version $Id$
 * @since 2.1.4
 */
public class PageSetupDialog extends BaseElement
{
    private final WebElement container;

    /**
     * @param container the dialog element
     */
    public PageSetupDialog(WebElement container)
    {
        this.container = container;
    }

    /**
     * @param paperSize the paper size, as displayed in the paper size drop down (e.g. "A4 (210 mm x 297 mm)")
     */
    public void setPaperSize(String paperSize)
    {
        new Select(this.container.findElement(By.tagName("select"))).selectByVisibleText(paperSize);
    }

    /**
     * @param landscape {@code true} for landscape orientation, {@code false} for portrait
     */
    public void setLandscape(boolean landscape)
    {
        this.container.findElement(
            By.cssSelector(String.format("input[type='radio'][value='%s']", landscape ? "landscape" : "portrait")))
            .click();
    }

    /**
     * @param gridSize the grid size
     */
    public void setGridSize(int gridSize)
    {
        WebElement input = this.container.findElement(By.cssSelector("input[type='number']"));
        DiagramEditor.click(input);
        DiagramEditor.actions().keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL)
            .sendKeys(String.valueOf(gridSize), Keys.TAB).perform();
    }

    /**
     * Opens the dialog used to choose the background image.
     *
     * @return the background image dialog
     */
    public BackgroundImageDialog openBackgroundImageDialog()
    {
        DiagramEditor.clickDialogButton(this.container, "Change...");
        getDriver().waitUntilCondition(driver -> getVisibleDialogs().size() > 1);
        List<WebElement> dialogs = getVisibleDialogs();
        return new BackgroundImageDialog(dialogs.get(dialogs.size() - 1));
    }

    /**
     * Applies the changes and closes the dialog.
     */
    public void apply()
    {
        DiagramEditor.clickDialogButton(this.container, "Apply");
        getDriver().waitUntilCondition(driver -> getVisibleDialogs().isEmpty());
    }

    private List<WebElement> getVisibleDialogs()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(".geDialog")).stream()
            .filter(WebElement::isDisplayed).collect(java.util.stream.Collectors.toList());
    }
}
