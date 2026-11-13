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

import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.PointerInput;

/**
 * Mouse and keyboard actions where the mouse moves are instantaneous. By default the mouse goes through all the points
 * between its current position and its target, which opens the diagram editor menus it passes over.
 *
 * @version $Id$
 * @since 2.1.4
 */
public class DiagramActions extends Actions
{
    /**
     * @param driver the web driver
     */
    public DiagramActions(WebDriver driver)
    {
        super(driver);
    }

    @Override
    public Actions moveToElement(WebElement target)
    {
        return moveToElement(target, 0, 0);
    }

    @Override
    public Actions moveToElement(WebElement target, int xOffset, int yOffset)
    {
        return tick(getActivePointer().createPointerMove(Duration.ZERO, PointerInput.Origin.fromElement(target),
            xOffset, yOffset));
    }

    @Override
    public Actions moveByOffset(int xOffset, int yOffset)
    {
        return tick(getActivePointer().createPointerMove(Duration.ZERO, PointerInput.Origin.pointer(), xOffset,
            yOffset));
    }
}
