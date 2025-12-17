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
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents a Diagram macro and provides access to its attributes.
 *
 * @version $Id$
 * @since 1.22.8
 */
public class DiagramMacro extends BaseElement
{
    private final WebElement diagram;

    public DiagramMacro(WebElement diagram)
    {
        this.diagram = diagram;
    }

    public boolean isCreateButton()
    {
        return diagram.getAttribute("class").contains("diagram-create");
    }

    public boolean hasThumbnail()
    {
        return !diagram.findElements(By.cssSelector(".thumbnail")).isEmpty();
    }

    public boolean hasSvg()
    {
        return !diagram.findElements(By.cssSelector("svg")).isEmpty();
    }

    public boolean isInteractiveSvg()
    {
        WebElement svg = diagram.findElement(By.cssSelector("svg"));
        String style = svg.getAttribute("style");
        return style != null && style.contains("width: 100%");
    }

    public boolean hasDataModel()
    {
        return diagram.getAttribute("data-model") != null;
    }

    public boolean hasToolbar()
    {
        return diagram.getAttribute("data-toolbar") != null;
    }

    public String getCreateButtonTemplateType()
    {
        if (!isCreateButton()) {
            return null;
        }
        return diagram.getAttribute("href");
    }

    public String getEditLink()
    {
        return diagram.findElement(By.cssSelector("a.diagram-edit")).getAttribute("href");
    }

    public String getCaption()
    {
        return diagram.findElement(By.cssSelector(".diagram-title")).getText();
    }

    public String getLink()
    {
        return diagram.findElement(By.cssSelector(".diagram-title")).getAttribute("href");
    }

    public String getReference()
    {
        return diagram.getAttribute("data-reference");
    }

    public boolean hasWarningMessage()
    {
        return !diagram.findElements(By.cssSelector(".box.warningmessage")).isEmpty();
    }

    public String getWarningMessage()
    {
        WebElement box = diagram.findElement(By.cssSelector(".box.warningmessage p"));
        return box.getText();
    }
}
