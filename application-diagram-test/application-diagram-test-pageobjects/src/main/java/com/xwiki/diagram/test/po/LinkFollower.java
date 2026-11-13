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

import java.util.HashSet;
import java.util.Set;

import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Follows links that may open their target in a new browser tab.
 *
 * @version $Id$
 * @since 2.1.4
 */
final class LinkFollower extends BaseElement
{
    private LinkFollower()
    {
    }

    /**
     * Clicks a link and makes sure its target is loaded in the current tab: if the link opens a new tab, the new tab is
     * closed and its URL is loaded in the current tab. This keeps a single tab opened for the following tests.
     *
     * @param click clicks the link
     */
    static void follow(Runnable click)
    {
        XWikiWebDriver driver = getUtil().getDriver();
        String originalWindow = driver.getWindowHandle();
        Set<String> windows = driver.getWindowHandles();
        String originalURL = driver.getCurrentUrl();
        driver.addPageNotYetReloadedMarker();
        click.run();
        driver.waitUntilCondition(
            d -> driver.getWindowHandles().size() > windows.size() || !originalURL.equals(driver.getCurrentUrl()));
        Set<String> newWindows = new HashSet<>(driver.getWindowHandles());
        newWindows.removeAll(windows);
        if (newWindows.isEmpty()) {
            driver.waitUntilPageIsReloaded();
        } else {
            driver.switchTo().window(newWindows.iterator().next());
            driver.waitUntilCondition(d -> !"about:blank".equals(driver.getCurrentUrl()));
            String targetURL = driver.getCurrentUrl();
            driver.close();
            driver.switchTo().window(originalWindow);
            driver.get(targetURL);
        }
    }
}
