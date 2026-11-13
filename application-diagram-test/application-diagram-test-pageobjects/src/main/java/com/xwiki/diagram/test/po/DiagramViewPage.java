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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The view mode of a page that displays one or more diagrams (a diagram page or a page using the diagram macros).
 *
 * @version $Id$
 * @since 2.0
 */
public class DiagramViewPage extends ViewPage
{
    private static final int LINK_UPDATE_TIMEOUT = 60;

    private static final By DIAGRAMS = By.cssSelector("#xwikicontent .diagram");

    /**
     * Creates a new instance and waits for the diagrams to be rendered.
     */
    public DiagramViewPage()
    {
        getDriver().waitUntilCondition(driver -> {
            List<WebElement> diagrams = getDriver().findElementsWithoutWaiting(DIAGRAMS);
            return !diagrams.isEmpty() && diagrams.stream()
                .allMatch(diagram -> diagram.getAttribute("class").contains("diagram-initialized"));
        });
    }

    /**
     * Reloads the current diagram page until its first diagram has a link that contains the given text. This is useful
     * when the diagram is updated in the background (e.g. after renaming a page that the diagram links to).
     *
     * @param reference the diagram page reference
     * @param linkTarget the text expected in one of the diagram link URLs
     * @return the diagram page
     */
    public static DiagramViewPage waitUntilLinkTargets(EntityReference reference, String linkTarget)
    {
        getUtil().getDriver().waitUntilCondition(driver -> {
            getUtil().gotoPage(reference);
            return new DiagramViewPage().getDiagram().getLinks().stream().anyMatch(link -> link.contains(linkTarget));
        }, LINK_UPDATE_TIMEOUT);
        return new DiagramViewPage();
    }

    /**
     * @return the diagrams displayed on the page
     */
    public List<DiagramViewer> getDiagrams()
    {
        return getDriver().findElementsWithoutWaiting(DIAGRAMS).stream().map(DiagramViewer::new)
            .collect(Collectors.toList());
    }

    /**
     * @return the first diagram displayed on the page
     */
    public DiagramViewer getDiagram()
    {
        return getDiagrams().get(0);
    }
}
