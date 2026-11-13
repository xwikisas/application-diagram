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
package com.xwiki.diagram.test.ui;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.diagram.test.po.DiagramEditPage;
import com.xwiki.diagram.test.po.DiagramEditor;
import com.xwiki.diagram.test.po.DiagramHomePage;
import com.xwiki.diagram.test.po.DiagramViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the Diagram application home page: creating, listing, filtering, sorting and deleting diagrams.
 *
 * @version $Id$
 * @since 2.1.4
 */
@UITest
class DiagramAppIT
{
    private static final String FIRST_DIAGRAM = "App Diagram One";

    private static final String SECOND_DIAGRAM = "App Diagram Two";

    private static final String OTHER_AUTHOR = "DiagramAuthor";

    private static final String OTHER_AUTHOR_DIAGRAM = "App Diagram By Author";

    /**
     * A title with accents, spaces and special characters, as suggested by the manual test.
     */
    private static final String SPECIAL_TITLE = "Té£  / stΦéd - There are 5 French accents: the cédille Ç";

    private static final String GENERAL = "General";

    private static final String LOCATION = "Location";

    private static final String UPDATE_DATE = "Update Date";

    private static final String LAST_AUTHOR = "Last Author";

    private static final DocumentReference NAME_STRATEGY_CONFIGURATION =
        new DocumentReference("xwiki", List.of("XWiki", "EntityNameValidation"), "Configuration");

    private static final DocumentReference NAME_STRATEGY_CLASS_REFERENCE =
        new DocumentReference("xwiki", List.of("XWiki", "EntityNameValidation"), "ConfigurationClass");

    private static final String NAME_STRATEGY_CLASS = "XWiki.EntityNameValidation.ConfigurationClass";

    @BeforeAll
    void setup(TestUtils setup)
    {
        setup.createAdminUser();
        setup.loginAsSuperAdmin();
        setup.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);
        setup.createUser(OTHER_AUTHOR, OTHER_AUTHOR, null);
    }

    @BeforeEach
    void beforeEach(TestUtils setup)
    {
        setup.loginAsAdmin();
    }

    /**
     * Manual test 12: Create a new diagram.
     */
    @Test
    @Order(1)
    void createDiagram()
    {
        DiagramEditPage editPage = DiagramHomePage.gotoPage().createDiagram(FIRST_DIAGRAM);
        DiagramEditor editor = editPage.getDiagramEditor();
        editor.addShape(GENERAL, 0);
        editor.addShape(GENERAL, 4);
        assertEquals(2, editor.getShapeCount());

        DiagramViewPage viewPage = editPage.clickSaveAndView();
        assertEquals(FIRST_DIAGRAM, viewPage.getDocumentTitle());
        assertEquals(2, viewPage.getDiagram().getModel().getVertices().size());
        assertTrue(viewPage.getDiagram().isRendered());

        // Create a second diagram, used to test the diagram list.
        editPage = DiagramHomePage.gotoPage().createDiagram(SECOND_DIAGRAM);
        editPage.getDiagramEditor().addShape(GENERAL, 1);
        editPage.clickSaveAndView();

        assertTrue(DiagramHomePage.gotoPage().getDiagramTitles().containsAll(List.of(FIRST_DIAGRAM, SECOND_DIAGRAM)));
    }

    /**
     * Manual test 13: Create a new diagram with naming strategy.
     */
    @Test
    @Order(2)
    void createDiagramWithNamingStrategy(TestUtils setup) throws Exception
    {
        // Precondition: enable the "Preformatted names" strategy with automatic name transformation and without name
        // validation. This is what Administer Wiki > Editing > Name Strategies saves. The administration UI is not
        // part of the test instance so we create the configuration class ourselves.
        setup.rest().savePage(NAME_STRATEGY_CLASS_REFERENCE, "", "");
        setup.addClassProperty(NAME_STRATEGY_CLASS_REFERENCE, "currentStrategy", "String");
        setup.addClassProperty(NAME_STRATEGY_CLASS_REFERENCE, "useTransformation", "Boolean");
        setup.addClassProperty(NAME_STRATEGY_CLASS_REFERENCE, "useValidation", "Boolean");
        setup.rest().savePage(NAME_STRATEGY_CONFIGURATION, "", "");
        setup.addObject(NAME_STRATEGY_CONFIGURATION, NAME_STRATEGY_CLASS, "currentStrategy",
            "SlugEntityNameValidation", "useTransformation", "1", "useValidation", "0");
        try {
            DiagramEditPage editPage = DiagramHomePage.gotoPage().createDiagram(SPECIAL_TITLE);
            editPage.getDiagramEditor().addShape(GENERAL, 0);
            DiagramViewPage viewPage = editPage.clickSaveAndView();

            assertEquals(SPECIAL_TITLE, viewPage.getDocumentTitle());
            assertEquals(1, viewPage.getDiagram().getModel().getVertices().size());
            // The page name (and thus the URL) doesn't contain accents, spaces or special characters anymore.
            String reference = viewPage.getMetaDataValue("reference");
            String pageName = reference.substring("xwiki:Diagram.".length(), reference.length() - ".WebHome".length());
            assertTrue(pageName.matches("[A-Za-z0-9\\-]+"), "Unexpected page name: " + pageName);
        } finally {
            setup.updateObject(NAME_STRATEGY_CONFIGURATION, NAME_STRATEGY_CLASS, 0, "useTransformation", "0");
        }
    }

    /**
     * Manual test 32: Filter the diagrams.
     */
    @Test
    @Order(3)
    void filterDiagrams(TestUtils setup)
    {
        // Create a diagram with another user, to be able to filter by author.
        setup.login(OTHER_AUTHOR, OTHER_AUTHOR);
        DiagramEditPage editPage = DiagramHomePage.gotoPage().createDiagram(OTHER_AUTHOR_DIAGRAM);
        editPage.getDiagramEditor().addShape(GENERAL, 2);
        editPage.clickSaveAndView();
        setup.loginAsAdmin();

        DiagramHomePage homePage = DiagramHomePage.gotoPage();

        // Filter by location.
        homePage.filterByLocation("App Diagram");
        List<String> titles = homePage.getDiagramTitles();
        assertEquals(3, titles.size(), titles.toString());
        homePage.filterByLocation(SECOND_DIAGRAM);
        assertEquals(List.of(SECOND_DIAGRAM), homePage.getDiagramTitles());
        homePage.filterByLocation("");

        // Filter by last author.
        homePage.filterByLastAuthor(OTHER_AUTHOR);
        assertEquals(List.of(OTHER_AUTHOR_DIAGRAM), homePage.getDiagramTitles());
        assertEquals(List.of(OTHER_AUTHOR), homePage.getLastAuthors());
        homePage.filterByLastAuthor("");

        // Filter by update date: all the diagrams were updated today and none was updated a year ago.
        ZoneId zone = ZoneId.systemDefault();
        long startOfToday = LocalDate.now(zone).minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        long endOfToday = LocalDate.now(zone).plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli();
        homePage.filterByLocation("App Diagram");
        homePage.filterByUpdateDate(startOfToday + "-" + endOfToday);
        assertEquals(3, homePage.getDiagramCount());
        long aYearAgo = LocalDate.now(zone).minusYears(1).atStartOfDay(zone).toInstant().toEpochMilli();
        homePage.filterByUpdateDate(aYearAgo + "-" + (aYearAgo + 1000));
        assertEquals(0, homePage.getDiagramCount());
    }

    /**
     * Manual test 61: Sort the diagrams.
     */
    @Test
    @Order(4)
    void sortDiagrams()
    {
        DiagramHomePage homePage = DiagramHomePage.gotoPage();
        // Only consider the diagrams created by this test, which fit on a single page.
        homePage.filterByLocation("App Diagram");

        homePage.sortBy(LOCATION, true);
        List<String> ascending = homePage.getDiagramTitles();
        assertEquals(List.of(OTHER_AUTHOR_DIAGRAM, FIRST_DIAGRAM, SECOND_DIAGRAM), ascending);
        homePage.sortBy(LOCATION, false);
        List<String> descending = homePage.getDiagramTitles();
        List<String> reversed = new ArrayList<>(ascending);
        Collections.reverse(reversed);
        assertEquals(reversed, descending);

        homePage.sortBy(UPDATE_DATE, true);
        assertSorted(homePage.getUpdateDates(), true);
        homePage.sortBy(UPDATE_DATE, false);
        assertSorted(homePage.getUpdateDates(), false);

        homePage.sortBy(LAST_AUTHOR, true);
        List<String> authors = homePage.getLastAuthors();
        assertTrue(authors.indexOf("Admin") < authors.indexOf(OTHER_AUTHOR), authors.toString());
        homePage.sortBy(LAST_AUTHOR, false);
        authors = homePage.getLastAuthors();
        assertTrue(authors.indexOf(OTHER_AUTHOR) < authors.indexOf("Admin"), authors.toString());
    }

    /**
     * Manual test 14: Delete a diagram.
     */
    @Test
    @Order(5)
    void deleteDiagram(TestUtils setup) throws Exception
    {
        DiagramHomePage homePage = DiagramHomePage.gotoPage();
        assertTrue(homePage.getDiagramTitles().contains(OTHER_AUTHOR_DIAGRAM));

        homePage.deleteDiagram(OTHER_AUTHOR_DIAGRAM);

        assertFalse(DiagramHomePage.gotoPage().getDiagramTitles().contains(OTHER_AUTHOR_DIAGRAM));
        assertFalse(setup.rest()
            .exists(new DocumentReference("xwiki", List.of("Diagram", OTHER_AUTHOR_DIAGRAM), "WebHome")));
    }

    private void assertSorted(List<String> values, boolean ascending)
    {
        for (int i = 1; i < values.size(); i++) {
            int comparison = values.get(i - 1).compareToIgnoreCase(values.get(i));
            assertTrue(ascending ? comparison <= 0 : comparison >= 0, "Not sorted: " + values);
        }
    }
}
