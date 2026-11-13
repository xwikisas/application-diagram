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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.diagram.test.po.DiagramCell;
import com.xwiki.diagram.test.po.DiagramEditPage;
import com.xwiki.diagram.test.po.DiagramEditor;
import com.xwiki.diagram.test.po.DiagramExportDialog;
import com.xwiki.diagram.test.po.DiagramModel;
import com.xwiki.diagram.test.po.DownloadedFile;
import com.xwiki.diagram.test.po.PrintPreview;
import com.xwiki.diagram.test.util.DockerDiagramTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the diagram exports: the exports from the diagram editor (File > Export as, File > Print / Export as
 * PDF) and the export of a diagram page from the wiki (HTML export). The exported files are checked, not only the
 * export dialogs.
 *
 * @version $Id$
 * @since 2.1.4
 */
@UITest
class DiagramExportIT
{
    private static final String SPACE = "DiagramExportIT";

    private static final DocumentReference DIAGRAM =
        new DocumentReference("xwiki", List.of(SPACE, "ExportedDiagram"), "WebHome");

    private static final DocumentReference LINK_TARGET =
        new DocumentReference("xwiki", List.of(SPACE, "LinkTarget"), "WebHome");

    private static final String ALPHA = "Alpha shape";

    private static final String BETA = "Beta shape";

    private static final String GAMMA = "Gamma shape";

    private static final String INTERNAL_LINK = "Wiki page link";

    private static final String EXTERNAL_LINK = "Flower";

    private static final String EXTERNAL_URL = "https://en.wikipedia.org/wiki/Flower";

    private static final List<String> SHAPE_IDS = List.of("alpha", "beta", "gamma", "internalLink", "externalLink");

    private static final String SELECTION_ONLY = "Selection Only";

    private static final String PNG_SIGNATURE = "\u0089PNG";

    @BeforeAll
    void setup(TestUtils setup) throws Exception
    {
        setup.createAdminUser();
        setup.loginAsSuperAdmin();
        setup.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);
        setup.loginAsAdmin();

        // Precondition: a diagram with shapes containing text, a link to a wiki page and a link to an external URL.
        setup.createPage(LINK_TARGET, "The page linked from the diagram.", "Link Target");
        String content = DockerDiagramTestUtils.getResourceContent(List.of("diagramExport", "exportedDiagram.drawio"))
            .replace("${linkTarget}", setup.serializeReference(LINK_TARGET));
        DockerDiagramTestUtils.createDiagram(setup, DIAGRAM, content, "Exported Diagram");
    }

    @BeforeEach
    void beforeEach(TestUtils setup)
    {
        setup.loginAsAdmin();
    }

    /**
     * Manual test 27: Export diagram as PNG.
     */
    @Test
    @Order(1)
    void exportAsPNG()
    {
        DiagramEditPage editPage = DiagramEditPage.gotoPage(DIAGRAM);
        DiagramEditor editor = editPage.getDiagramEditor();

        DiagramExportDialog dialog = editor.openExportDialog("PNG...");
        assertTrue(dialog.getOptions().containsAll(List.of(SELECTION_ONLY, "Transparent Background", "Shadow",
            "Grid", "Include a copy of my diagram")), dialog.getOptions().toString());
        DownloadedFile png = dialog.export("full-diagram.png");
        assertEquals("full-diagram.png", png.getName());
        assertTrue(isPNG(png.getContent()), "Not a PNG image");
        BufferedImage fullImage = readImage(png);
        // The PNG contains a copy of the diagram (option checked by default), so it can be edited again in draw.io.
        assertTrue(png.getText().contains("mxfile"), "The PNG image doesn't include the diagram.");

        // Export only the selected shape.
        editor.selectCell("alpha");
        dialog = editor.openExportDialog("PNG...");
        dialog.setOption(SELECTION_ONLY, true);
        BufferedImage selectionImage = readImage(dialog.export("selection.png"));
        assertSmaller(selectionImage, fullImage);

        editPage.clickCancel();
    }

    /**
     * Manual test 25: Export diagram as JPEG.
     */
    @Test
    @Order(2)
    void exportAsJPEG()
    {
        DiagramEditPage editPage = DiagramEditPage.gotoPage(DIAGRAM);
        DiagramEditor editor = editPage.getDiagramEditor();

        DownloadedFile jpeg = editor.openExportDialog("JPEG...").export("full-diagram.jpg");
        assertEquals("full-diagram.jpg", jpeg.getName());
        // JPEG files start with the FF D8 marker.
        assertEquals((byte) 0xFF, jpeg.getContent()[0]);
        assertEquals((byte) 0xD8, jpeg.getContent()[1]);
        BufferedImage fullImage = readImage(jpeg);

        editor.selectCell("alpha");
        DiagramExportDialog dialog = editor.openExportDialog("JPEG...");
        dialog.setOption(SELECTION_ONLY, true);
        assertSmaller(readImage(dialog.export("selection.jpg")), fullImage);

        editPage.clickCancel();
    }

    /**
     * Export diagram as WebP.
     */
    @Test
    @Order(3)
    void exportAsWebP()
    {
        DiagramEditPage editPage = DiagramEditPage.gotoPage(DIAGRAM);

        DownloadedFile webp = editPage.getDiagramEditor().openExportDialog("WebP...").export("full-diagram.webp");
        assertEquals("full-diagram.webp", webp.getName());
        // WebP files are RIFF containers of type WEBP.
        String header = new String(webp.getContent(), 0, 12, StandardCharsets.ISO_8859_1);
        assertTrue(header.startsWith("RIFF") && header.endsWith("WEBP"), header);
        assertTrue(webp.getContent().length > 100);

        editPage.clickCancel();
    }

    /**
     * Manual test 28: Export diagram as SVG (with functional links).
     */
    @Test
    @Order(4)
    void exportAsSVG() throws Exception
    {
        DiagramEditPage editPage = DiagramEditPage.gotoPage(DIAGRAM);
        DiagramEditor editor = editPage.getDiagramEditor();

        DownloadedFile svg = editor.openExportDialog("SVG...").export("full-diagram.svg");
        assertEquals("full-diagram.svg", svg.getName());
        Document document = parseXML(svg.getText());
        assertEquals("svg", document.getDocumentElement().getLocalName());
        String text = document.getDocumentElement().getTextContent();
        for (String label : List.of(ALPHA, BETA, GAMMA, INTERNAL_LINK, EXTERNAL_LINK)) {
            assertTrue(text.contains(label), "Missing label: " + label);
        }
        // The links are functional: the link to the wiki page is converted to an absolute URL.
        List<String> links = getLinks(document);
        assertTrue(links.contains(EXTERNAL_URL), links.toString());
        assertTrue(links.stream().anyMatch(link -> link.startsWith("http") && link.endsWith("/LinkTarget/")),
            links.toString());
        assertFalse(svg.getText().contains("data:xwiki"), "The SVG contains XWiki specific links.");

        // Export only the selected shape, with all the other options enabled.
        editor.selectCell("alpha");
        DiagramExportDialog dialog = editor.openExportDialog("SVG...");
        for (String option : List.of(SELECTION_ONLY, "Transparent Background", "Shadow", "Embed Images")) {
            dialog.setOption(option, true);
        }
        String selectionText = parseXML(dialog.export("selection.svg").getText()).getDocumentElement()
            .getTextContent();
        assertTrue(selectionText.contains(ALPHA));
        assertFalse(selectionText.contains(BETA));
        assertFalse(selectionText.contains(GAMMA));

        editPage.clickCancel();
    }

    /**
     * Manual test 30: Export diagram as XML.
     */
    @Test
    @Order(5)
    void exportAsXML()
    {
        DiagramEditPage editPage = DiagramEditPage.gotoPage(DIAGRAM);
        DiagramEditor editor = editPage.getDiagramEditor();

        DownloadedFile xml = editor.openExportDialog("XML...").export("full-diagram.xml");
        assertEquals("full-diagram.xml", xml.getName());
        // The exported diagram can be imported back (e.g. in draw.io): it contains all the shapes.
        assertEquals(SHAPE_IDS, getShapeIds(DiagramModel.parse(xml.getText())));

        editor.selectCell("alpha");
        DiagramExportDialog dialog = editor.openExportDialog("XML...");
        dialog.setOption(SELECTION_ONLY, true);
        // The exported shapes are copies, with new identifiers.
        assertEquals(List.of(ALPHA), DiagramModel.parse(dialog.export("selection.xml").getText()).getVertices()
            .stream().map(DiagramCell::getValue).collect(Collectors.toList()));

        editPage.clickCancel();
    }

    /**
     * Export diagram as VSDX (Microsoft Visio).
     */
    @Test
    @Order(6)
    void exportAsVSDX() throws IOException
    {
        DiagramEditPage editPage = DiagramEditPage.gotoPage(DIAGRAM);

        DownloadedFile vsdx = editPage.getDiagramEditor().openExportDialog("VSDX (beta)...").export("diagram.vsdx");
        assertTrue(vsdx.getName().endsWith(".vsdx"), vsdx.getName());
        // A VSDX file is a ZIP package containing the Visio document and its pages.
        Map<String, byte[]> files = unzip(vsdx.getContent());
        assertTrue(files.containsKey("visio/document.xml"), files.keySet().toString());
        String pages = files.entrySet().stream().filter(entry -> entry.getKey().startsWith("visio/pages/page"))
            .map(entry -> new String(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining());
        assertTrue(pages.contains(ALPHA) && pages.contains(GAMMA), "The shapes are missing from the VSDX pages.");

        editPage.clickCancel();
    }

    /**
     * Manual tests 26 (Export diagram as PDF) and 53 (Print option): the print dialog options and the print preview.
     */
    @Test
    @Order(7)
    void printPreview()
    {
        DiagramEditPage editPage = DiagramEditPage.gotoPage(DIAGRAM);

        DiagramExportDialog dialog = editPage.getDiagramEditor().openPrintDialog();
        assertTrue(dialog.getOptions().containsAll(List.of(SELECTION_ONLY, "Grid")), dialog.getOptions().toString());
        PrintPreview preview = dialog.preview();
        assertTrue(preview.getPageCount() > 0);
        for (String label : List.of(ALPHA, BETA, GAMMA, INTERNAL_LINK, EXTERNAL_LINK)) {
            assertTrue(preview.getText().contains(label), "Missing label: " + label);
        }

        editPage.clickCancel();
    }

    /**
     * The export of a diagram page from the wiki (HTML export) includes the diagram, as SVG (default) or as PNG image.
     */
    @Test
    @Order(8)
    void exportDiagramPageAsHTML(TestUtils setup) throws Exception
    {
        String pagePath = SPACE + "/ExportedDiagram/WebHome";
        String attachmentPath = "attachment/xwiki/" + pagePath + "/";

        Map<String, byte[]> files = DockerDiagramTestUtils.exportPageAsHTML(setup, pagePath);
        String html = new String(files.get("pages/xwiki/" + pagePath + ".html"), StandardCharsets.UTF_8);
        // The diagram is included as SVG, so that its links are clickable.
        assertTrue(html.contains("<svg"), "The diagram is missing from the exported page.");
        for (String label : List.of(ALPHA, BETA, GAMMA)) {
            assertTrue(html.contains(label), "Missing label: " + label);
        }
        assertTrue(files.containsKey(attachmentPath + "diagram.svg"), files.keySet().toString());

        // Export the diagram as PNG image instead.
        setup.updateObject(DIAGRAM, "Diagram.DiagramClass", 0, "exportUsingSVG", "0");
        try {
            files = DockerDiagramTestUtils.exportPageAsHTML(setup, pagePath);
            html = new String(files.get("pages/xwiki/" + pagePath + ".html"), StandardCharsets.UTF_8);
            assertTrue(html.contains("diagram.png"), "The diagram image is missing from the exported page.");
            byte[] png = files.get(attachmentPath + "diagram.png");
            assertNotNull(png, files.keySet().toString());
            assertTrue(isPNG(png));
        } finally {
            setup.updateObject(DIAGRAM, "Diagram.DiagramClass", 0, "exportUsingSVG", "1");
        }
    }

    private boolean isPNG(byte[] content)
    {
        return new String(content, 0, 4, StandardCharsets.ISO_8859_1).equals(PNG_SIGNATURE);
    }

    private BufferedImage readImage(DownloadedFile file)
    {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getContent()));
            assertNotNull(image, "Failed to read the exported image " + file.getName());
            assertTrue(image.getWidth() > 0 && image.getHeight() > 0);
            return image;
        } catch (IOException e) {
            throw new AssertionError("Failed to read the exported image " + file.getName(), e);
        }
    }

    private void assertSmaller(BufferedImage selection, BufferedImage full)
    {
        assertTrue(selection.getWidth() < full.getWidth(),
            String.format("The selection (%d px) should be narrower than the diagram (%d px).", selection.getWidth(),
                full.getWidth()));
    }

    private Document parseXML(String xml) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // Don't load the SVG DTD.
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private List<String> getLinks(Document document)
    {
        List<String> links = new ArrayList<>();
        NodeList anchors = document.getElementsByTagNameNS("*", "a");
        for (int i = 0; i < anchors.getLength(); i++) {
            Element anchor = (Element) anchors.item(i);
            String href = anchor.getAttribute("href");
            if (href.isEmpty()) {
                href = anchor.getAttributeNS("http://www.w3.org/1999/xlink", "href");
            }
            if (!href.isEmpty()) {
                links.add(href);
            }
        }
        return links;
    }

    private List<String> getShapeIds(DiagramModel model)
    {
        return model.getVertices().stream().map(DiagramCell::getId).collect(Collectors.toList());
    }

    private Map<String, byte[]> unzip(byte[] content) throws IOException
    {
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                files.put(entry.getName(), zip.readAllBytes());
            }
        }
        return files;
    }
}
