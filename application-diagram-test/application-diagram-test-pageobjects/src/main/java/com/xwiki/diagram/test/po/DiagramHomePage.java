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
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.DeletePageConfirmationPage;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * The home page of the Diagram application, which lists the diagrams.
 *
 * @version $Id$
 * @since 2.1.4
 */
public class DiagramHomePage extends ViewPage
{
    private static final String LIVE_TABLE_ID = "diagram";

    private static final String LOCATION = "Location";

    private static final String DIAGRAM_TEMPLATE_PROVIDER = "Diagram.DiagramTemplateProvider";

    private final LiveTableElement liveTable = new LiveTableElement(LIVE_TABLE_ID);

    /**
     * Creates a new instance and waits for the diagram list to be loaded.
     */
    public DiagramHomePage()
    {
        this.liveTable.waitUntilReady();
    }

    /**
     * Opens the home page of the Diagram application.
     *
     * @return the home page
     */
    public static DiagramHomePage gotoPage()
    {
        getUtil().gotoPage("Diagram", "WebHome");
        return new DiagramHomePage();
    }

    /**
     * Creates a new diagram using the Create button, the "Diagram" page type and the given title.
     *
     * @param title the diagram title
     * @return the diagram edit page
     */
    public DiagramEditPage createDiagram(String title)
    {
        CreatePagePage createPage = createPage();
        createPage.getDocumentPicker().setTitle(title);
        createPage.setType(DIAGRAM_TEMPLATE_PROVIDER);
        createPage.clickCreate();
        return new DiagramEditPage();
    }

    /**
     * @return the number of diagrams displayed in the list
     */
    public int getDiagramCount()
    {
        return getRows().size();
    }

    /**
     * @return the titles of the listed diagrams (the last element of the diagram location), in the displayed order
     */
    public List<String> getDiagramTitles()
    {
        int columnIndex = this.liveTable.getColumnIndex(LOCATION);
        return getRows().stream().map(row -> {
            List<WebElement> path = row.findElements(By.tagName("td")).get(columnIndex).findElements(By.tagName("li"));
            return path.get(path.size() - 1).getText().trim();
        }).collect(Collectors.toList());
    }

    /**
     * @return the last authors of the listed diagrams, in the displayed order
     */
    public List<String> getLastAuthors()
    {
        return getColumnValues("Last Author");
    }

    /**
     * @return the update dates of the listed diagrams, in the displayed order
     */
    public List<String> getUpdateDates()
    {
        return getColumnValues("Update Date");
    }

    private List<String> getColumnValues(String column)
    {
        int columnIndex = this.liveTable.getColumnIndex(column);
        return getRows().stream().map(row -> row.findElements(By.tagName("td")).get(columnIndex).getText().trim())
            .collect(Collectors.toList());
    }

    private List<WebElement> getRows()
    {
        // Ignore the row displayed when there are no results.
        return getDriver().findElementsWithoutWaiting(By.cssSelector(String.format("#%s-display tr", LIVE_TABLE_ID)))
            .stream().filter(row -> row.findElements(By.tagName("td")).size() > 1).collect(Collectors.toList());
    }

    /**
     * @param location the text to filter the diagram location with
     */
    public void filterByLocation(String location)
    {
        filter(1, location);
    }

    /**
     * @param dateRange the update date range, as {@code <start timestamp>-<end timestamp>} (in milliseconds)
     */
    public void filterByUpdateDate(String dateRange)
    {
        filter(2, dateRange);
    }

    /**
     * @param author the text to filter the last author with
     */
    public void filterByLastAuthor(String author)
    {
        filter(3, author);
    }

    private void filter(int column, String value)
    {
        this.liveTable.filterColumn(String.format("xwiki-livetable-%s-filter-%d", LIVE_TABLE_ID, column), value);
    }

    /**
     * Sorts the diagrams.
     *
     * @param column the column to sort on ("Location", "Update Date" or "Last Author")
     * @param ascending {@code true} for ascending order, {@code false} for descending order
     */
    public void sortBy(String column, boolean ascending)
    {
        String expectedOrder = ascending ? "asc" : "desc";
        // Clicking the column header sorts the column, or reverses the order if the column is already sorted.
        for (int i = 0; i < 2 && !isSortedBy(column, expectedOrder); i++) {
            this.liveTable.sortBy(column);
        }
        if (!isSortedBy(column, expectedOrder)) {
            throw new IllegalStateException(String.format("Failed to sort by [%s] %s.", column, expectedOrder));
        }
    }

    private boolean isSortedBy(String column, String order)
    {
        WebElement header = getDriver().findElement(By.xpath(String.format("//table[@id = '%s']//th[contains(@class, "
            + "'xwiki-livetable-display-header-text') and normalize-space(.) = '%s']", LIVE_TABLE_ID, column)));
        List<String> classes = List.of(header.getAttribute("class").split("\\s+"));
        return classes.contains("selected") && classes.contains(order);
    }

    /**
     * Deletes a diagram using the Delete action from the diagram list.
     *
     * @param title the title of the diagram to delete
     */
    public void deleteDiagram(String title)
    {
        WebElement deleteLink = getRows().get(getDiagramTitles().indexOf(title))
            .findElement(By.cssSelector("a.actiondelete"));
        deleteLink.click();
        // Confirm the deletion.
        new DeletePageConfirmationPage().confirmDeletePage().waitUntilFinished();
    }
}
