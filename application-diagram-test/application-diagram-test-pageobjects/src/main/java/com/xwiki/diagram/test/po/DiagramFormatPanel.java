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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The format panel displayed on the right side of the diagram editor (Diagram / Style / Text / Arrange tabs).
 *
 * @version $Id$
 * @since 2.1.4
 */
public class DiagramFormatPanel extends BaseElement
{
    private static final String PANEL = "//div[contains(@class, 'geFormatContainer')]";

    private static final String CHECKBOX = PANEL + "//input[@type = 'checkbox' and (following-sibling::*[1]"
        + "[normalize-space(.) = '%1$s'] or normalize-space(..) = '%1$s' or starts-with(normalize-space(..), '%1$s'))]";

    private static final String BUTTON = PANEL + "//a[starts-with(@title, '%s')]";

    private static final String POPUP_MENU_ROWS = "table.mxPopupMenu tr.mxPopupMenuItem";

    /**
     * Selects a tab of the format panel.
     *
     * @param name the tab name (e.g. "Style", "Text", "Arrange")
     */
    public void openTab(String name)
    {
        WebElement tab = getDriver().findElement(
            By.xpath(String.format("%s//div[not(*) and normalize-space(.) = '%s']", PANEL, name)));
        DiagramEditor.click(tab);
    }

    /**
     * Checks or un-checks one of the check boxes from the panel (e.g. "Fill", "Gradient", "Line", "Shadow",
     * "Background Color").
     *
     * @param label the check box label
     * @param checked whether the check box should be checked
     */
    public void setChecked(String label, boolean checked)
    {
        WebElement checkbox = getCheckbox(label);
        if (checkbox.isSelected() != checked) {
            DiagramEditor.click(checkbox);
        }
    }

    /**
     * Changes the color associated with one of the check boxes from the panel (e.g. "Fill", "Line", "Gradient",
     * "Gradient") using the color window.
     *
     * @param label the check box label
     * @param hexColor the color in hexadecimal format, without the leading # (e.g. "FF0000")
     */
    public void setColor(String label, String hexColor)
    {
        WebElement checkbox = getCheckbox(label);
        WebElement colorButton = checkbox.findElement(By.xpath("..//*[contains(@class, 'geColorBtn')]"));
        DiagramEditor.click(colorButton);
        // The color window is not modal and it applies the color as soon as it is entered (on Enter).
        By colorWindowLocator = By.cssSelector("div.mxWindow.geColorWindow");
        getDriver().waitUntilCondition(driver -> getVisibleColorWindow(colorWindowLocator) != null);
        WebElement colorWindow = getVisibleColorWindow(colorWindowLocator);
        // The first text input holds the light color code (the second one holds the dark color code).
        WebElement colorInput = colorWindow.findElement(By.xpath(".//input[not(@type) or @type = 'text']"));
        DiagramEditor.click(colorInput);
        selectAllAndType(hexColor);
        DiagramEditor.actions().sendKeys(Keys.ENTER).perform();
        // Close the color window, if it's still open.
        if (getVisibleColorWindow(colorWindowLocator) != null) {
            colorWindow.findElements(By.cssSelector("img[title='Close']")).stream().filter(WebElement::isDisplayed)
                .findFirst().ifPresent(DiagramEditor::click);
        }
        getDriver().waitUntilCondition(driver -> getVisibleColorWindow(colorWindowLocator) == null);
    }

    private WebElement getVisibleColorWindow(By locator)
    {
        return getDriver().findElementsWithoutWaiting(locator).stream().filter(WebElement::isDisplayed).findFirst()
            .orElse(null);
    }

    /**
     * Changes the gradient direction.
     *
     * @param direction the gradient direction (e.g. "north", "east", "south", "west", "radial")
     */
    public void setGradientDirection(String direction)
    {
        new Select(getCheckbox("Gradient").findElement(By.xpath("..//select"))).selectByValue(direction);
    }

    /**
     * Selects a line pattern using the pattern drop down.
     *
     * @param index the index of the pattern in the drop down (0 is solid, 1 is dashed, 2 is dotted)
     */
    public void setLinePattern(int index)
    {
        clickButton("Pattern");
        List<WebElement> rows = getDriver().findElementsWithoutWaiting(By.cssSelector(POPUP_MENU_ROWS)).stream()
            .filter(WebElement::isDisplayed).collect(Collectors.toList());
        DiagramEditor.actions().moveToElement(rows.get(index)).click().perform();
    }

    /**
     * Changes the line width.
     *
     * @param width the line width, in points
     */
    public void setLineWidth(int width)
    {
        setNumber(getDisplayedElement(PANEL + "//input[@title = 'Line Width' or @title = 'Linewidth']"), width);
    }

    /**
     * Clicks one of the buttons of the panel, identified by its tool tip (e.g. "Bold", "Italic", "Underline", "Left",
     * "Center", "Right", "Top", "Middle", "Bottom").
     *
     * @param title the beginning of the button tool tip
     */
    public void clickButton(String title)
    {
        WebElement button = getDriver().findElementsWithoutWaiting(By.xpath(String.format(BUTTON, title))).stream()
            .filter(WebElement::isDisplayed).findFirst().orElseThrow();
        DiagramEditor.click(button);
    }

    /**
     * Changes the font family of the selected text shape, from the Text tab.
     *
     * @param fontFamily the font family, as displayed in the font family drop down
     */
    public void setFontFamily(String fontFamily)
    {
        clickButton("Font Family");
        WebElement font = getDriver().findElementsWithoutWaiting(By.cssSelector(POPUP_MENU_ROWS)).stream()
            .filter(row -> row.isDisplayed() && fontFamily.equals(row.getText().trim())).findFirst().orElseThrow();
        DiagramEditor.actions().moveToElement(font).click().perform();
    }

    /**
     * Changes the font size of the selected text shape, from the Text tab.
     *
     * @param size the font size, in pixels
     */
    public void setFontSize(int size)
    {
        // The font size input is displayed right after the text direction button.
        setNumber(getDisplayedElement(PANEL + "//a[@title = 'Vertical']/following::input[1]"), size);
    }

    /**
     * Opens the dialog used to change the background image of the diagram, from the Diagram tab (displayed when
     * nothing is selected).
     *
     * @return the background image dialog
     */
    public BackgroundImageDialog openBackgroundImageDialog()
    {
        WebElement button = getDriver().findElementsWithoutWaiting(
            By.xpath(PANEL + "//button[normalize-space(.) = 'Change...']")).stream().filter(WebElement::isDisplayed)
            .findFirst().orElseThrow();
        DiagramEditor.click(button);
        getDriver().waitUntilElementIsVisible(By.cssSelector(".geDialog"));
        return new BackgroundImageDialog(getDriver().findElement(By.cssSelector(".geDialog")));
    }

    private WebElement getDisplayedElement(String xpath)
    {
        return getDriver().findElementsWithoutWaiting(By.xpath(xpath)).stream().filter(WebElement::isDisplayed)
            .findFirst().orElseThrow();
    }

    private void setNumber(WebElement input, int value)
    {
        DiagramEditor.click(input);
        selectAllAndType(String.valueOf(value));
        DiagramEditor.actions().sendKeys(Keys.ENTER).perform();
    }

    /**
     * Opens the dialog used to set the diagram background (image and color), from the Diagram tab.
     *
     * @return the background dialog
     */
    public BackgroundImageDialog openBackgroundDialog()
    {
        WebElement button = getCheckbox("Background").findElement(By.xpath("..//button"));
        DiagramEditor.click(button);
        getDriver().waitUntilCondition(driver -> getDriver().findElementsWithoutWaiting(By.cssSelector(".geDialog"))
            .stream().anyMatch(WebElement::isDisplayed));
        List<WebElement> dialogs = getDriver().findElementsWithoutWaiting(By.cssSelector(".geDialog")).stream()
            .filter(WebElement::isDisplayed).collect(Collectors.toList());
        return new BackgroundImageDialog(dialogs.get(dialogs.size() - 1));
    }

    private void selectAllAndType(String text)
    {
        DiagramEditor.actions().keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL)
            .sendKeys(text).perform();
    }

    private WebElement getCheckbox(String label)
    {
        List<WebElement> checkboxes = getDriver().findElementsWithoutWaiting(By.xpath(String.format(CHECKBOX, label)));
        Optional<WebElement> displayedCheckbox = checkboxes.stream().filter(WebElement::isDisplayed).findFirst();
        if (displayedCheckbox.isPresent()) {
            return displayedCheckbox.get();
        }
        // Some of the check boxes are inside a collapsible section (e.g. "Effects") that is collapsed by default.
        for (WebElement checkbox : checkboxes) {
            Optional<WebElement> sectionTitle = checkbox.findElements(By.xpath(
                "ancestor::div[contains(@class, 'geCollapsibleContent')][1]"
                    + "/preceding-sibling::*[contains(@class, 'geCollapsibleTitle')][1]"))
                .stream().filter(WebElement::isDisplayed).findFirst();
            if (sectionTitle.isPresent()) {
                DiagramEditor.click(sectionTitle.get());
                getDriver().waitUntilCondition(driver -> checkbox.isDisplayed());
                return checkbox;
            }
        }
        throw new NoSuchElementException(String.format("Check box [%s] not found.", label));
    }
}
