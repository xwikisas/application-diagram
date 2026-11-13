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
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The dialog used to choose the background image of a diagram.
 *
 * @version $Id$
 * @since 2.1.4
 */
public class BackgroundImageDialog extends BaseElement
{
    private final WebElement container;

    /**
     * @param container the dialog element
     */
    public BackgroundImageDialog(WebElement container)
    {
        this.container = container;
    }

    /**
     * Sets the URL and the size of the background image.
     *
     * @param url the image URL
     * @param width the image width
     * @param height the image height
     */
    public void setImage(String url, int width, int height)
    {
        WebElement urlOption = this.container.findElement(By.cssSelector("input[type='radio'][value='url']"));
        if (!urlOption.isSelected()) {
            // The radio button is styled in a way that prevents WebDriver from clicking it directly.
            getDriver().executeJavascript("arguments[0].click();", urlOption);
        }
        // The first text input is the image URL, followed by the image width and height.
        List<WebElement> inputs = this.container.findElements(By.cssSelector("input[type='text']"));
        inputs.get(0).clear();
        inputs.get(0).sendKeys(url, Keys.TAB);
        // The image size is computed automatically when the image is loaded so we set it after.
        getDriver().waitUntilCondition(driver -> !inputs.get(1).getAttribute("value").isEmpty());
        setValue(inputs.get(1), width);
        setValue(inputs.get(2), height);
    }

    private void setValue(WebElement input, int value)
    {
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), String.valueOf(value));
    }

    /**
     * Applies the changes and closes the dialog.
     */
    public void apply()
    {
        DiagramEditor.clickDialogButton(this.container, "Apply");
        getDriver().waitUntilCondition(driver -> {
            try {
                return !this.container.isDisplayed();
            } catch (StaleElementReferenceException e) {
                // The dialog was removed.
                return true;
            }
        });
    }
}
