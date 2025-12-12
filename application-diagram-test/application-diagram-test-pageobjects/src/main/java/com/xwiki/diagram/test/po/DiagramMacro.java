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

public class DiagramMacro extends BaseElement
{
    private final WebElement root;

    public DiagramMacro(WebElement root)
    {
        this.root = root;
    }

    public boolean isCreateButton()
    {
        return root.getAttribute("class").contains("diagram-create");
    }

    public boolean isCachedDiagram()
    {
        return root.getAttribute("class").contains("diagram-container");
    }

    public boolean isNotCached()
    {
        return root.getAttribute("class").equals("diagram");
    }

    public String getCreateButtonURL()
    {
        if (!isCreateButton()) {
            return null;
        }
        return root.getAttribute("href");
    }

    public boolean hasSVG()
    {
        if (!isCachedDiagram()) {
            return false;
        }
        return root.findElement(By.cssSelector("img"))
            .getAttribute("src")
            .contains(".svg");
    }

    public String getDiagramName()
    {
        return root.findElement(By.cssSelector(".diagram-title")).getText();
    }

    public String getDiagramReference()
    {
        return root.findElement(By.cssSelector(".diagram-title")).getAttribute("href");
    }

    public String getModelReference()
    {
        if (!isNotCached()) {
            return null;
        }
        return root.getAttribute("data-reference");
    }

    public boolean hasWarningMessage()
    {
        return !root.findElements(By.cssSelector(".box.warningmessage")).isEmpty();
    }

    public String getWarningMessage()
    {
        WebElement box = root.findElement(By.cssSelector(".box.warningmessage p"));
        return box.getText().trim();
    }

}
