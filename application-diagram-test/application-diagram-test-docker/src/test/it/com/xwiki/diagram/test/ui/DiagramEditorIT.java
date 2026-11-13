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

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Point;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;

import com.xwiki.diagram.test.po.BackgroundImageDialog;
import com.xwiki.diagram.test.po.DiagramCell;
import com.xwiki.diagram.test.po.DiagramEditPage;
import com.xwiki.diagram.test.po.DiagramEditor;
import com.xwiki.diagram.test.po.DiagramFormatPanel;
import com.xwiki.diagram.test.po.DiagramHomePage;
import com.xwiki.diagram.test.po.DiagramModel;
import com.xwiki.diagram.test.po.DiagramViewPage;
import com.xwiki.diagram.test.po.DiagramViewer;
import com.xwiki.diagram.test.po.MoreShapesDialog;
import com.xwiki.diagram.test.po.PageSetupDialog;
import com.xwiki.diagram.test.util.DockerDiagramTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI tests for the diagram editor.
 *
 * @version $Id$
 * @since 2.1.4
 */
@UITest
class DiagramEditorIT
{
    private static final String GENERAL = "General";

    private static final String STYLE = "Style";

    private static final String TEXT = "Text";

    private static final String FILL = "Fill";

    private static final String LINE = "Line";

    private static final String GRADIENT = "Gradient";

    private static final String SHADOW = "Shadow";

    private static final String SHAPE_A = "shapeA";

    private static final String SHAPE_B = "shapeB";

    private static final String SHAPE_C = "shapeC";

    private static final String IMAGE = "landscape.png";

    private static final String OTHER_USER = "DiagramEditor";

    private static final LocalDocumentReference SOLR_QUEUE_SIZE_PAGE =
        new LocalDocumentReference(List.of("DiagramEditorIT"), "SolrQueueSize");

    /**
     * The shape libraries enabled from the "More Shapes" dialog and, for each of them, a shape palette that is added
     * to the side bar.
     */
    private static final Map<String, String> MORE_SHAPES = Map.ofEntries(
        Map.entry("AWS18", "AWS18 / Compute"),
        Map.entry("BPMN 2.0", "BPMN 2.0 General"),
        Map.entry("Cisco", "Cisco / Routers"),
        Map.entry("Clipart", "Clipart / Computer"),
        Map.entry("Electrical", "Electrical / Logic Gates"),
        Map.entry("Mockups", "Mockup Containers"),
        Map.entry("Proc. Eng.", "Proc. Eng. / Valves"),
        Map.entry("Signs", "Signs / Animals"),
        Map.entry("iOS", "iOS UI"),
        Map.entry("Value Stream Mapping", "Value Stream Mapping"));

    /**
     * The shape palettes displayed by default.
     */
    private static final List<String> DEFAULT_PALETTES =
        List.of(GENERAL, "Basic", "Arrows", "Flowchart", "Entity Relation", "UML");

    @BeforeAll
    void setup(TestUtils setup) throws Exception
    {
        setup.createAdminUser();
        setup.loginAsSuperAdmin();
        setup.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);
        setup.createUser(OTHER_USER, OTHER_USER, null);
        setup.rest().savePage(SOLR_QUEUE_SIZE_PAGE, "{{velocity}}$services.solr.queueSize{{/velocity}}",
            Syntax.XWIKI_2_1.toIdString(), "", null);
    }

    @BeforeEach
    void beforeEach(TestUtils setup)
    {
        setup.loginAsAdmin();
    }

    /**
     * Manual tests 2 (Add more shapes), 23 (Expand and collapse the shapes categories), 44 (Lean Mapping Shapes) and
     * the "Add a shape" tests for each shape library: 0 (AWS), 5 (Arrows), 6 (BPMN), 7 (Basic), 10 (Cisco), 11
     * (Clipart), 20 (Electrical), 22 (Entity Relation), 33 (Flowchart), 34 (General), 50 (Mockup), 52 (PID), 60
     * (Signs), 62 (UML) and 67 (iOS).
     */
    @Test
    @Order(1)
    void shapeLibraries()
    {
        DiagramEditPage editPage = DiagramHomePage.gotoPage().createDiagram("Shape Libraries");
        DiagramEditor editor = editPage.getDiagramEditor();

        // Add more shapes.
        MORE_SHAPES.values().forEach(palette -> assertFalse(editor.isPaletteDisplayed(palette), palette));
        MoreShapesDialog moreShapesDialog = editor.openMoreShapesDialog();
        MORE_SHAPES.keySet().forEach(library -> moreShapesDialog.setLibraryEnabled(library, true));
        moreShapesDialog.apply();
        MORE_SHAPES.values().forEach(palette -> assertTrue(editor.isPaletteDisplayed(palette), palette));

        // Expand and collapse the shape categories.
        assertTrue(editor.isPaletteExpanded(GENERAL));
        editor.togglePalette(GENERAL);
        assertFalse(editor.isPaletteExpanded(GENERAL));
        editor.togglePalette(GENERAL);
        assertTrue(editor.isPaletteExpanded(GENERAL));
        assertFalse(editor.isPaletteExpanded("Basic"));
        editor.togglePalette("Basic");
        assertTrue(editor.isPaletteExpanded("Basic"));
        editor.togglePalette("Basic");
        assertFalse(editor.isPaletteExpanded("Basic"));

        // Add a shape from each library. Some shapes (e.g. tables) are made of multiple cells.
        for (String palette : DEFAULT_PALETTES) {
            addShape(editor, palette);
        }
        for (String palette : MORE_SHAPES.values()) {
            addShape(editor, palette);
        }

        // Lean mapping shapes, using the search box.
        assertTrue(editor.searchShapes("lean mapping") > 0);
        int shapeCount = editor.getShapeCount();
        String leanMappingShape = editor.addSearchResult(0);
        assertTrue(editor.getModel().getCell(leanMappingShape).orElseThrow().getStyle().toString()
            .contains("lean_mapping"));
        int expectedShapeCount = editor.getShapeCount();
        assertTrue(expectedShapeCount > shapeCount);

        DiagramViewPage viewPage = editPage.clickSaveAndView();
        assertEquals(expectedShapeCount, viewPage.getDiagram().getModel().getVertices().size());
    }

    private void addShape(DiagramEditor editor, String palette)
    {
        int shapeCount = editor.getShapeCount();
        String shapeId = editor.addShape(palette, 0);
        assertTrue(editor.getShapeCount() > shapeCount, palette);
        assertTrue(editor.getModel().getCell(shapeId).isPresent(), palette);
    }

    /**
     * Manual tests 3 (Align button), 8 (Bold, Italic and Underline buttons) and 9 (Change the font and the size).
     */
    @Test
    @Order(2)
    void textFormatting(TestUtils setup, TestReference testReference) throws Exception
    {
        DiagramEditor editor = createEmptyDiagram(setup, testReference).getDiagramEditor();
        String shape = editor.addShape(GENERAL, 0);
        editor.setShapeText(shape, "Formatted text");
        editor.selectCell(shape);
        DiagramFormatPanel formatPanel = editor.getFormatPanel();
        formatPanel.openTab(TEXT);

        // Bold, italic and underline are stored as flags of the font style: 1, 2 and 4.
        formatPanel.clickButton("Bold");
        assertEquals("1", getStyle(editor, shape, "fontStyle"));
        formatPanel.clickButton("Italic");
        assertEquals("3", getStyle(editor, shape, "fontStyle"));
        formatPanel.clickButton("Underline");
        assertEquals("7", getStyle(editor, shape, "fontStyle"));
        formatPanel.clickButton("Bold");
        assertEquals("6", getStyle(editor, shape, "fontStyle"));

        // Align.
        for (String align : List.of("Left", "Right", "Center")) {
            formatPanel.clickButton(align);
            assertEquals(align.toLowerCase(), getStyle(editor, shape, "align"));
        }
        for (String verticalAlign : List.of("Top", "Bottom", "Middle")) {
            formatPanel.clickButton(verticalAlign);
            assertEquals(verticalAlign.toLowerCase(), getStyle(editor, shape, "verticalAlign"));
        }

        // Font and size.
        formatPanel.setFontFamily("Verdana");
        assertEquals("Verdana", getStyle(editor, shape, "fontFamily"));
        formatPanel.setFontSize(20);
        assertEquals("20", getStyle(editor, shape, "fontSize"));
    }

    /**
     * Manual tests 31 (Fill color button), 35 (Gradient button), 45 (Line button) and 59 (Shadow button).
     */
    @Test
    @Order(3)
    void styleOptions(TestUtils setup, TestReference testReference) throws Exception
    {
        DiagramEditPage editPage = createEmptyDiagram(setup, testReference);
        DiagramEditor editor = editPage.getDiagramEditor();
        String shape = editor.addShape(GENERAL, 0);
        DiagramFormatPanel formatPanel = editor.getFormatPanel();
        formatPanel.openTab(STYLE);

        // Fill color.
        formatPanel.setChecked(FILL, true);
        formatPanel.setColor(FILL, "FF0000");
        assertEquals("#FF0000", getStyle(editor, shape, "fillColor").toUpperCase());

        // Gradient.
        formatPanel.setChecked(GRADIENT, true);
        for (String direction : List.of("north", "west", "south", "radial", "east")) {
            formatPanel.setGradientDirection(direction);
            assertEquals(direction, getStyle(editor, shape, "gradientDirection"));
        }
        formatPanel.setColor(GRADIENT, "0000FF");
        assertEquals("#0000FF", getStyle(editor, shape, "gradientColor").toUpperCase());

        // Line type, color and thickness.
        formatPanel.setChecked(LINE, true);
        formatPanel.setLinePattern(1);
        assertEquals("1", getStyle(editor, shape, "dashed"));
        formatPanel.setColor(LINE, "00FF00");
        assertEquals("#00FF00", getStyle(editor, shape, "strokeColor").toUpperCase());
        formatPanel.setLineWidth(5);
        assertEquals("5", getStyle(editor, shape, "strokeWidth"));

        // Shadow.
        formatPanel.setChecked(SHADOW, true);
        assertEquals("1", getStyle(editor, shape, "shadow"));

        DiagramCell savedShape = editPage.clickSaveAndView().getDiagram().getModel().getVertices().get(0);
        assertEquals("#FF0000", savedShape.getStyle("fillColor").toUpperCase());
        assertEquals("#0000FF", savedShape.getStyle("gradientColor").toUpperCase());
        assertEquals("east", savedShape.getStyle("gradientDirection"));
        assertEquals("#00FF00", savedShape.getStyle("strokeColor").toUpperCase());
        assertEquals("5", savedShape.getStyle("strokeWidth"));
        assertEquals("1", savedShape.getStyle("dashed"));
        assertEquals("1", savedShape.getStyle("shadow"));
    }

    /**
     * Manual tests 4 (Arrange Shape To Front and To Back), 57 (Rotate a Shape 90 Degrees), 48 (Maximize the shape),
     * 49 (Minimize the shape), 58 (Rotate the shape), 63 (Undo button), 54 (Redo button) and 15 (Delete button).
     */
    @Test
    @Order(4)
    void arrangeAndEditShapes(TestUtils setup, TestReference testReference) throws Exception
    {
        DocumentReference diagramReference = createThreeShapesDiagram(setup, testReference);
        DiagramEditPage editPage = DiagramEditPage.gotoPage(diagramReference);
        DiagramEditor editor = editPage.getDiagramEditor();

        // To Front and To Back.
        assertEquals(0, editor.getModel().getZIndex(SHAPE_A));
        editor.selectCell(SHAPE_A);
        editor.toFront();
        assertEquals(2, editor.getModel().getZIndex(SHAPE_A));
        editor.toBack();
        assertEquals(0, editor.getModel().getZIndex(SHAPE_A));
        editor.bringToFrontUsingMenu();
        assertEquals(2, editor.getModel().getZIndex(SHAPE_A));

        // Rotate by 90 degrees.
        editor.selectCell(SHAPE_B);
        editor.rotateSelectionBy90();
        // Rectangles are turned by swapping their width and height.
        assertRotatedBy90(editor.getModel().getCell(SHAPE_B).orElseThrow());

        DiagramModel savedModel = editPage.clickSaveAndView().getDiagram().getModel();
        assertEquals(2, savedModel.getZIndex(SHAPE_A));
        assertRotatedBy90(savedModel.getCell(SHAPE_B).orElseThrow());

        editPage = DiagramEditPage.gotoPage(diagramReference);
        editor = editPage.getDiagramEditor();

        // Maximize and minimize the shape by dragging its bottom right corner.
        editor.selectCell(SHAPE_C);
        double initialWidth = editor.getModel().getCell(SHAPE_C).orElseThrow().getWidth();
        double initialHeight = editor.getModel().getCell(SHAPE_C).orElseThrow().getHeight();
        editor.resizeSelectedShape(SHAPE_C, 80, 60);
        DiagramCell maximized = editor.getModel().getCell(SHAPE_C).orElseThrow();
        assertTrue(maximized.getWidth() > initialWidth, maximized.toString());
        assertTrue(maximized.getHeight() > initialHeight, maximized.toString());
        editor.resizeSelectedShape(SHAPE_C, -120, -90);
        DiagramCell minimized = editor.getModel().getCell(SHAPE_C).orElseThrow();
        assertTrue(minimized.getWidth() < maximized.getWidth(), minimized.toString());
        assertTrue(minimized.getHeight() < maximized.getHeight(), minimized.toString());

        // Rotate the shape by dragging the rotation handle clockwise.
        assertEquals(0, minimized.getRotation());
        editor.rotateSelectedShape(SHAPE_C, 60, 60);
        double rotation = editor.getModel().getCell(SHAPE_C).orElseThrow().getRotation();
        assertTrue(rotation > 0 && rotation < 180, "Unexpected rotation: " + rotation);

        // Undo and redo.
        String newShape = editor.addShape(GENERAL, 4);
        assertEquals(4, editor.getShapeCount());
        editor.undo();
        assertEquals(3, editor.getShapeCount());
        assertFalse(editor.getModel().getCell(newShape).isPresent());
        editor.redo();
        assertEquals(4, editor.getShapeCount());
        assertTrue(editor.getModel().getCell(newShape).isPresent());

        // Delete using the context menu.
        editor.deleteCellUsingContextMenu(newShape);
        assertEquals(3, editor.getShapeCount());
        assertFalse(editor.getModel().getCell(newShape).isPresent());

        assertEquals(3, editPage.clickSaveAndView().getDiagram().getModel().getVertices().size());
    }

    /**
     * Manual tests 19 (Drop down menu), 65 (Zoom in button), 66 (Zoom out button), 55 (Reset View) and 51 (Navigation
     * through the page).
     */
    @Test
    @Order(5)
    void menusZoomAndNavigation(TestUtils setup, TestReference testReference) throws Exception
    {
        DiagramEditPage editPage = DiagramEditPage.gotoPage(createThreeShapesDiagram(setup, testReference));
        DiagramEditor editor = editPage.getDiagramEditor();

        // Drop down menus.
        editor.selectCell(SHAPE_A);
        assertMenuContains(editor, "File", "Import from", "Export as", "Page Setup...", "Print / Export as PDF");
        assertMenuContains(editor, "Edit", "Cut", "Copy", "Duplicate", "Delete", "Select All", "Lock/Unlock");
        assertMenuContains(editor, "View", "Format", "Outline", "Layers", "Grid", "Reset View", "Zoom In",
            "Zoom Out");
        assertMenuContains(editor, "Arrange", "To Front", "To Back", "Rotate shape only by 90° / Reverse", "Group");
        assertMenuContains(editor, "Extras", "Edit Diagram...", "Configuration...");
        assertMenuContains(editor, "Help", "Keyboard Shortcuts...");

        // Zoom in, zoom out and reset view.
        assertEquals(1, editor.getZoom());
        assertEquals("100%", editor.getDisplayedZoom());
        editor.zoomIn();
        assertTrue(editor.getZoom() > 1);
        assertNotEquals("100%", editor.getDisplayedZoom());
        editor.zoomOut();
        editor.zoomOut();
        assertTrue(editor.getZoom() < 1);
        editor.resetViewFromZoomMenu();
        assertEquals(1, editor.getZoom());
        assertEquals("100%", editor.getDisplayedZoom());

        // Navigation using the scroll bars (mouse wheel).
        Point initialPosition = editor.getScrollPosition();
        editor.scrollCanvas(0, 200);
        Point scrolledDown = editor.getScrollPosition();
        assertTrue(scrolledDown.getY() > initialPosition.getY(), scrolledDown.toString());
        editor.scrollCanvas(0, -200);
        assertTrue(editor.getScrollPosition().getY() < scrolledDown.getY());
        editor.scrollCanvas(200, 0);
        Point scrolledRight = editor.getScrollPosition();
        assertTrue(scrolledRight.getX() > initialPosition.getX(), scrolledRight.toString());
        editor.scrollCanvas(-200, 0);
        assertTrue(editor.getScrollPosition().getX() < scrolledRight.getX());

        editPage.clickCancel();
    }

    private void assertRotatedBy90(DiagramCell shape)
    {
        // The shape was 120 x 80 initially.
        assertEquals(80, shape.getWidth(), shape.toString());
        assertEquals(120, shape.getHeight(), shape.toString());
    }

    private void assertMenuContains(DiagramEditor editor, String menu, String... expectedItems)
    {
        List<String> items = editor.getMenuItems(menu);
        for (String expectedItem : expectedItems) {
            assertTrue(items.contains(expectedItem), String.format("[%s] not found in %s menu %s", expectedItem,
                menu, items));
        }
    }

    /**
     * Manual tests 17 (Diagram Page Setup) and 56 (Resize the working space).
     */
    @Test
    @Order(6)
    void pageSetup(TestUtils setup, TestReference testReference) throws Exception
    {
        DocumentReference diagramReference = createThreeShapesDiagram(setup, testReference);
        String imageURL = attachImage(setup, diagramReference);
        DiagramEditPage editPage = DiagramEditPage.gotoPage(diagramReference);
        DiagramEditor editor = editPage.getDiagramEditor();
        assertEquals("850", editor.getModel().getAttribute("pageWidth"));
        assertEquals("1100", editor.getModel().getAttribute("pageHeight"));

        PageSetupDialog pageSetup = editor.openPageSetup();
        pageSetup.setPaperSize("A4 (210 mm x 297 mm)");
        pageSetup.setLandscape(true);
        pageSetup.setGridSize(20);
        BackgroundImageDialog backgroundImageDialog = pageSetup.openBackgroundImageDialog();
        backgroundImageDialog.setImage(imageURL, 320, 200);
        backgroundImageDialog.apply();
        pageSetup.apply();

        // Background color, from the Diagram tab of the format panel.
        editor.selectNone();
        DiagramFormatPanel formatPanel = editor.getFormatPanel();
        formatPanel.setChecked("Background Color", true);
        formatPanel.setColor("Background Color", "FFFFCC");

        DiagramModel model = editor.getModel();
        // A4 landscape.
        assertEquals("1169", model.getAttribute("pageWidth"));
        assertEquals("827", model.getAttribute("pageHeight"));
        assertEquals("20", model.getAttribute("gridSize"));
        assertEquals("#FFFFCC", model.getAttribute("background").toUpperCase());
        assertBackgroundImage(model);

        model = editPage.clickSaveAndView().getDiagram().getModel();
        assertEquals("1169", model.getAttribute("pageWidth"));
        assertEquals("827", model.getAttribute("pageHeight"));
        assertEquals("20", model.getAttribute("gridSize"));
        assertEquals("#FFFFCC", model.getAttribute("background").toUpperCase());
        assertBackgroundImage(model);
    }

    /**
     * Manual tests 1 (Add image) and 36 (Image background).
     */
    @Test
    @Order(7)
    void images(TestUtils setup, TestReference testReference) throws Exception
    {
        createEmptyDiagramPage(setup, testReference);
        String imageURL = attachImage(setup, testReference);
        DiagramEditPage editPage = DiagramEditPage.gotoPage(testReference);
        DiagramEditor editor = editPage.getDiagramEditor();

        // Image from the computer (the equivalent of a drag & drop is to import it from the device).
        editor.importFromDevice(getResourceFile("images/" + IMAGE));
        // Image from an URL.
        String imageShape = editor.insertImage(imageURL);
        // The image URL might be converted (e.g. to a data URI) when the image is inserted.
        assertNotNull(editor.getModel().getCell(imageShape).orElseThrow().getStyle("image"));
        List<DiagramCell> images = getImages(editor.getModel());
        assertEquals(2, images.size(), images.toString());

        // Background image.
        editor.selectNone();
        BackgroundImageDialog backgroundImageDialog = editor.getFormatPanel().openBackgroundImageDialog();
        backgroundImageDialog.setImage(imageURL, 400, 250);
        backgroundImageDialog.apply();
        assertBackgroundImage(editor.getModel());

        DiagramViewer diagram = editPage.clickSaveAndView().getDiagram();
        assertEquals(2, getImages(diagram.getModel()).size());
        assertBackgroundImage(diagram.getModel());
        assertTrue(diagram.getImageCount() >= 2, "The images are not displayed.");
    }

    private void assertBackgroundImage(DiagramModel model)
    {
        // The background image is stored as JSON, e.g. {"src": "data:image/png;base64,...", "width": 320, ...}. The
        // image is embedded in the diagram when it's loaded from an URL.
        String backgroundImage = model.getAttribute("backgroundImage");
        assertTrue(backgroundImage.contains("\"src\":\"data:image/png") || backgroundImage.contains(IMAGE),
            backgroundImage.substring(0, Math.min(100, backgroundImage.length())));
    }

    private List<DiagramCell> getImages(DiagramModel model)
    {
        return model.getVertices().stream().filter(cell -> cell.getStyle("image") != null)
            .collect(Collectors.toList());
    }

    /**
     * Manual tests 29 (Export diagram as URL), 40 (Import diagram from URL) and 39 (Import diagram from Device).
     */
    @Test
    @Order(8)
    void exportAndImport(TestUtils setup, TestReference testReference) throws Exception
    {
        DiagramEditPage editPage = DiagramEditPage.gotoPage(createThreeShapesDiagram(setup, testReference));
        String diagramURL = editPage.getDiagramEditor().exportAsURL();
        assertTrue(diagramURL.startsWith("http") && diagramURL.contains("#"), diagramURL);
        editPage.clickCancel();

        DiagramEditor editor = createEmptyDiagram(setup,
            new DocumentReference("ImportTarget", testReference.getLastSpaceReference())).getDiagramEditor();
        assertEquals(0, editor.getShapeCount());

        editor.importFromURL(diagramURL);
        DiagramModel model = editor.getModel();
        assertEquals(3, model.getVertices().size());
        assertEquals(List.of("A", "B", "C"),
            model.getVertices().stream().map(DiagramCell::getValue).collect(Collectors.toList()));

        editor.importFromDevice(getResourceFile("diagramEditor/importedDiagram.drawio"));
        model = editor.getModel();
        assertEquals(5, model.getVertices().size());
        assertTrue(model.getVertices().stream().anyMatch(cell -> "Imported circle".equals(cell.getValue())));
    }

    /**
     * Manual test 47: Lock and Unlock Shapes.
     */
    @Test
    @Order(9)
    void lockAndUnlockShapes(TestUtils setup, TestReference testReference) throws Exception
    {
        DocumentReference diagramReference = createThreeShapesDiagram(setup, testReference);
        DiagramEditPage editPage = DiagramEditPage.gotoPage(diagramReference);
        DiagramEditor editor = editPage.getDiagramEditor();

        editor.selectAll();
        editor.lockUnlockSelection();
        assertTrue(editor.getModel().getVertices().stream().allMatch(DiagramCell::isLocked));
        // Locked shapes can't be deleted.
        editor.selectAll();
        editor.deleteSelectionUsingMenu();
        assertEquals(3, editor.getShapeCount());
        assertTrue(editPage.clickSaveAndView().getDiagram().getModel().getVertices().stream()
            .allMatch(DiagramCell::isLocked));

        // Another user can't change the locked shapes either, until they are unlocked.
        setup.login(OTHER_USER, OTHER_USER);
        editPage = DiagramEditPage.gotoPage(diagramReference);
        editor = editPage.getDiagramEditor();
        editor.selectAll();
        editor.deleteSelectionUsingMenu();
        assertEquals(3, editor.getShapeCount());

        editor.selectAll();
        editor.lockUnlockSelection();
        assertTrue(editor.getModel().getVertices().stream().noneMatch(DiagramCell::isLocked));
        editor.selectCell(SHAPE_B);
        editor.deleteSelection();
        assertEquals(2, editor.getShapeCount());
        DiagramModel model = editPage.clickSaveAndView().getDiagram().getModel();
        assertEquals(List.of(SHAPE_A, SHAPE_C),
            model.getVertices().stream().map(DiagramCell::getId).collect(Collectors.toList()));
    }

    /**
     * Manual test 46: Links to internal pages.
     */
    @Test
    @Order(10)
    void linksToInternalPages(TestUtils setup, TestReference testReference) throws Exception
    {
        DocumentReference linkedPage =
            new DocumentReference("WebHome", new SpaceReference("Page 1", testReference.getLastSpaceReference()));
        setup.createPage(linkedPage, "Content of the linked page.", "Page 1");
        DocumentReference diagramReference =
            new DocumentReference("WebHome", new SpaceReference("Linked Diagram", testReference.getLastSpaceReference()));
        String content = DockerDiagramTestUtils.getResourceContent(List.of("diagramEditor", "linkedDiagram.drawio"))
            .replace("${linkTarget}", setup.serializeReference(linkedPage));
        DockerDiagramTestUtils.createDiagram(setup, diagramReference, content, "Linked Diagram");
        assertTrue(new DiagramViewPage().getDiagram().getLinks().stream().anyMatch(link -> link.contains("Page 1")));

        // Rename the linked page. The links are updated using the Solr index so we need to wait for it to be ready.
        DockerDiagramTestUtils.waitForSolrIndexing(setup, SOLR_QUEUE_SIZE_PAGE);
        RenamePage renamePage = setup.gotoPage(linkedPage).rename();
        renamePage.getDocumentPicker().setTitle("QA Test");
        renamePage.clickRenameButton().waitUntilFinished();

        // The diagram is updated in the background.
        setup.gotoPage(diagramReference);
        DiagramViewPage diagramPage = DiagramViewPage.waitUntilLinkTargets(diagramReference, "QA Test");
        ViewPage targetPage = diagramPage.getDiagram().clickLink("Linked page");
        assertEquals("QA Test", targetPage.getDocumentTitle());
        assertEquals("Content of the linked page.", targetPage.getContent());
    }

    /**
     * Multi-page diagrams: each page is edited separately, saved, displayed in view mode and exported as image
     * attachments.
     */
    @Test
    @Order(11)
    void multiPageDiagram(TestUtils setup, TestReference testReference) throws Exception
    {
        DiagramEditPage editPage = createEmptyDiagram(setup, testReference);
        DiagramEditor editor = editPage.getDiagramEditor();
        assertEquals(List.of("Page-1"), editor.getPageNames());
        String firstShape = editor.addShape(GENERAL, 0);
        editor.setShapeText(firstShape, "First page shape");

        // Add a second page, with its own shapes.
        assertEquals("Page-2", editor.addPage());
        assertEquals(List.of("Page-1", "Page-2"), editor.getPageNames());
        assertEquals(0, editor.getShapeCount());
        String secondShape = editor.addShape(GENERAL, 4);
        editor.setShapeText(secondShape, "Second page shape");
        String thirdShape = editor.addShape(GENERAL, 0);
        assertEquals(2, editor.getShapeCount());

        // Each page keeps its own shapes.
        editor.selectPage("Page-1");
        assertEquals(List.of(firstShape),
            editor.getModel().getVertices().stream().map(DiagramCell::getId).collect(Collectors.toList()));
        editor.selectPage("Page-2");
        assertEquals(List.of(secondShape, thirdShape),
            editor.getModel().getVertices().stream().map(DiagramCell::getId).collect(Collectors.toList()));

        // Both pages are saved and can be browsed in view mode.
        DiagramViewer diagram = editPage.clickSaveAndView().getDiagram();
        assertEquals(List.of("Page-1", "Page-2"), diagram.getPageNames());
        diagram.hover();
        assertEquals("1 / 2", diagram.getPageIndicator());
        assertTrue(diagram.getDisplayedText().contains("First page shape"), diagram.getDisplayedText());
        diagram.goToNextPage();
        assertEquals("2 / 2", diagram.getPageIndicator());
        assertTrue(diagram.getDisplayedText().contains("Second page shape"), diagram.getDisplayedText());

        // Each page is exported as image attachments (used by the Diagram macro and the PDF export).
        List<String> attachments = DockerDiagramTestUtils.getAttachmentNames(setup, testReference);
        assertTrue(attachments.containsAll(List.of("diagram.png", "diagram.svg", "diagram2.png", "diagram2.svg")),
            attachments.toString());
    }

    private DiagramEditPage createEmptyDiagram(TestUtils setup, DocumentReference reference) throws Exception
    {
        createEmptyDiagramPage(setup, reference);
        return DiagramEditPage.gotoPage(reference);
    }

    private void createEmptyDiagramPage(TestUtils setup, DocumentReference reference) throws Exception
    {
        setup.rest().savePage(reference, "", Syntax.XWIKI_2_1.toIdString(), "Diagram", null);
        setup.addObject(reference, "Diagram.DiagramClass");
    }

    private DocumentReference createThreeShapesDiagram(TestUtils setup, DocumentReference reference) throws Exception
    {
        DockerDiagramTestUtils.createDiagram(setup, reference,
            DockerDiagramTestUtils.getResourceContent(List.of("diagramEditor", "threeShapes.drawio")), "Three Shapes");
        return reference;
    }

    private String getStyle(DiagramEditor editor, String cellId, String styleName)
    {
        return editor.getModel().getCell(cellId).orElseThrow().getStyle(styleName);
    }

    private String attachImage(TestUtils setup, DocumentReference reference) throws Exception
    {
        try (InputStream image = getClass().getResourceAsStream("/images/" + IMAGE)) {
            setup.attachFile(reference, IMAGE, image, false);
        }
        return setup.getURL(new AttachmentReference(IMAGE, reference), "download", "");
    }

    private File getResourceFile(String path) throws URISyntaxException
    {
        return new File(getClass().getResource("/" + path).toURI());
    }
}
