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
package com.xwiki.diagram.test.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.resources.attachments.AttachmentsResource;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.diagram.test.po.DiagramEditPage;

public class DockerDiagramTestUtils
{
    private static final String DIAGRAM_CLASS = "Diagram.DiagramClass";

    private static final int SOLR_INDEXING_TIMEOUT = 60;

    /**
     * Creates a diagram page with the given content. The diagram is saved once with the diagram editor in order to
     * generate the image attachments used to display the diagram.
     *
     * @param setup the test utilities
     * @param reference the diagram page reference
     * @param content the diagram XML
     * @param title the diagram page title
     */
    public static void createDiagram(TestUtils setup, EntityReference reference, String content, String title)
        throws Exception
    {
        setup.rest().savePage(reference, content, Syntax.XWIKI_2_1.toIdString(), title, null);
        setup.addObject(reference, DIAGRAM_CLASS);
        DiagramEditPage.gotoPage(reference).clickSaveAndView();
    }

    public static String getResourceContent(List<String> path)
    {
        if (path == null || path.isEmpty()) {
            return "";
        }

        String resourcePath = "/" + String.join("/", path);

        try (InputStream inputStream = DockerDiagramTestUtils.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Failed to read resource " + resourcePath);
            }

            return new BufferedReader(new InputStreamReader(inputStream)).lines()
                .filter(line -> !line.trim().startsWith("##")).collect(Collectors.joining("\n"));

        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource " + resourcePath, e);
        }
    }

    /**
     * Waits for the Solr indexing queue to be empty.
     *
     * @param setup the test utilities
     * @param queueSizePage a page that displays the size of the Solr indexing queue
     */
    public static void waitForSolrIndexing(TestUtils setup, EntityReference queueSizePage)
    {
        setup.getDriver().waitUntilCondition(driver -> {
            try {
                return "0".equals(setup.executeAndGetBodyAsString(queueSizePage, Map.of("outputSyntax", "plain"))
                    .trim());
            } catch (Exception e) {
                return false;
            }
        }, SOLR_INDEXING_TIMEOUT);
    }

    /**
     * Waits for the content of a page to contain the given text. This is useful when the page is updated in the
     * background (e.g. by a refactoring job).
     *
     * @param setup the test utilities
     * @param reference the page reference
     * @param text the expected text
     * @param timeout the timeout, in seconds
     */
    public static void waitUntilPageContentContains(TestUtils setup, EntityReference reference, String text,
        int timeout)
    {
        setup.getDriver().waitUntilCondition(driver -> {
            try {
                Page page = setup.rest().get(reference);
                return page.getContent().contains(text);
            } catch (Exception e) {
                return false;
            }
        }, timeout);
    }

    /**
     * @param setup the test utilities
     * @param reference the page reference
     * @return the names of the page attachments
     */
    public static List<String> getAttachmentNames(TestUtils setup, EntityReference reference) throws Exception
    {
        Attachments attachments = setup.rest().get(AttachmentsResource.class, reference);
        return attachments.getAttachments().stream().map(Attachment::getName).collect(Collectors.toList());
    }

    /**
     * Exports a page as HTML (Page menu > Export > HTML), like a user would do, and returns the content of the
     * exported ZIP archive.
     *
     * @param setup the test utilities
     * @param pagePath the page path, e.g. {@code Space/Page/WebHome}
     * @return the files from the exported archive, by path
     */
    public static Map<String, byte[]> exportPageAsHTML(TestUtils setup, String pagePath) throws Exception
    {
        // The REST URL prefix is the one that can be reached from the tests (the other one is for the browser).
        String baseURL = TestUtils.RestTestUtils.urlPrefix != null ? TestUtils.RestTestUtils.urlPrefix
            : setup.getBaseURL();
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(
            setup.getInputStream(baseURL, "bin/export/" + pagePath, Map.of("format", "html")))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                files.put(entry.getName(), zip.readAllBytes());
            }
        }
        return files;
    }
}
