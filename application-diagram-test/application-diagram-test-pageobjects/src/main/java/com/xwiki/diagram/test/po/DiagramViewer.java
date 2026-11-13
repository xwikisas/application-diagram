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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * A diagram displayed in view mode.
 *
 * @version $Id$
 * @since 2.1.4
 */
public class DiagramViewer extends BaseElement
{
    private static final By TOOLBAR_BUTTONS = By.cssSelector(".xwiki-diagram-drawio-toolbar [title]");

    private final WebElement container;

    /**
     * @param container the diagram container
     */
    public DiagramViewer(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the diagram content
     */
    public DiagramModel getModel()
    {
        return DiagramModel.parse(this.container.getAttribute("data-model"));
    }

    /**
     * @return the names of the diagram pages
     */
    public List<String> getPageNames()
    {
        return DiagramModel.parsePageNames(this.container.getAttribute("data-model"));
    }

    /**
     * @return the text displayed by the diagram (the labels of the displayed page)
     */
    public String getDisplayedText()
    {
        return this.container.findElement(By.tagName("svg")).getText();
    }

    /**
     * @return the page indicator displayed in the tool bar for diagrams with multiple pages (e.g. "1 / 2"), or an empty
     *     string if it's not displayed
     */
    public String getPageIndicator()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(".xwiki-diagram-drawio-toolbar div")).stream()
            .filter(element -> element.isDisplayed() && element.getText().matches("\\d+ / \\d+"))
            .map(WebElement::getText).findFirst().orElse("");
    }

    /**
     * Displays the next diagram page using the "Next Page" button of the tool bar.
     */
    public void goToNextPage()
    {
        hover();
        String indicator = getPageIndicator();
        getDriver().findElementsWithoutWaiting(TOOLBAR_BUTTONS).stream()
            .filter(button -> button.isDisplayed() && "Next Page".equals(button.getAttribute("title"))).findFirst()
            .orElseThrow().click();
        getDriver().waitUntilCondition(driver -> !indicator.equals(getPageIndicator()));
    }

    /**
     * @return the reference of the page that holds the displayed diagram
     */
    public String getSourceReference()
    {
        return this.container.getAttribute("data-reference");
    }

    /**
     * @return whether the diagram is rendered (as SVG)
     */
    public boolean isRendered()
    {
        return !getDriver().findElementsWithoutWaiting(this.container, By.tagName("svg")).isEmpty();
    }

    /**
     * @return the number of images displayed by the diagram
     */
    public int getImageCount()
    {
        return getDriver().findElementsWithoutWaiting(this.container, By.tagName("image")).size();
    }

    /**
     * Moves the mouse over the diagram, which displays the diagram tool bar.
     */
    public void hover()
    {
        getDriver().scrollTo(this.container);
        DiagramEditor.actions().moveToElement(this.container).perform();
        getDriver().waitUntilCondition(driver -> !getToolbarButtons().isEmpty());
    }

    /**
     * @return the tool tips of the visible tool bar buttons (e.g. "Zoom In", "Zoom Out", "Fullscreen")
     */
    public List<String> getToolbarButtons()
    {
        return getDriver().findElementsWithoutWaiting(TOOLBAR_BUTTONS).stream().filter(WebElement::isDisplayed)
            .map(button -> button.getAttribute("title")).collect(Collectors.toList());
    }

    /**
     * Clicks the Edit button of the diagram tool bar.
     *
     * @return the diagram edit page
     */
    public DiagramEditPage clickEdit()
    {
        hover();
        getDriver().findElementsWithoutWaiting(TOOLBAR_BUTTONS).stream()
            .filter(button -> button.isDisplayed() && "Edit".equals(button.getAttribute("title"))).findFirst()
            .orElseThrow().click();
        return new DiagramEditPage();
    }

    /**
     * @return the (URL decoded) targets of the links displayed by the diagram
     */
    public List<String> getLinks()
    {
        return getDriver().findElementsWithoutWaiting(this.container, By.tagName("a")).stream()
            .map(link -> link.getAttribute("href") != null ? link.getAttribute("href")
                : link.getAttribute("xlink:href"))
            .filter(Objects::nonNull).map(link -> URLDecoder.decode(link, StandardCharsets.UTF_8))
            .collect(Collectors.toList());
    }

    /**
     * Clicks on the diagram link with the given text.
     *
     * @param text the link text
     * @return the target page
     */
    public ViewPage clickLink(String text)
    {
        WebElement link = this.container.findElement(By.xpath(String.format(".//*[local-name() = 'a']"
            + "[contains(normalize-space(.), '%s')]", text)));
        getDriver().scrollTo(this.container);
        LinkFollower.follow(() -> DiagramEditor.actions().moveToElement(link).click().perform());
        return new ViewPage();
    }
}
