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

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.WheelInput;
import org.openqa.selenium.remote.LocalFileDetector;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The draw.io based diagram editor, used both for standalone diagrams (diagram pages) and for inline diagrams.
 *
 * @version $Id$
 * @since 2.1.4
 */
public class DiagramEditor extends BaseElement
{
    /**
     * JavaScript prefix giving access to the draw.io editor UI ({@code ui}) and to the graph ({@code graph}).
     */
    private static final String JS_UI = "var ui = window._xfiles[window._xfiles.length - 1].getUi();"
        + " var graph = ui.editor.graph;";

    private static final String JS_CELL = "var cell = graph.getModel().getCell(arguments[0]);";

    private static final String SIDEBAR = "//div[contains(@class, 'geSidebarContainer')"
        + " and not(contains(@class, 'geFormatContainer'))]";

    private static final String VISIBLE_POPUP_MENUS = "table.mxPopupMenu";

    private static final String POPUP_MENU_ITEM =
        ".//tr[contains(@class, 'mxPopupMenuItem')][td[2][normalize-space(.) = '%s']]";

    private static final String CLASS = "class";

    private static final String DISABLED = "mxDisabled";

    private static final String VALUE = "value";

    private static final String GE_ITEM = "a.geItem";

    private static final String APPLY = "Apply";

    private static final String FILE_MENU = "File";

    private static final String EDIT_MENU = "Edit";

    private static final String ARRANGE_MENU = "Arrange";

    private static final String IMPORT_FROM = "Import from";

    private static final String URL = "URL...";

    /**
     * Creates a new instance and waits for the editor to be ready.
     */
    public DiagramEditor()
    {
        waitUntilReady();
    }

    private void waitUntilReady()
    {
        getDriver().waitUntilJavascriptCondition("return !!window._xfiles && window._xfiles.length > 0"
            + " && !!window._xfiles[window._xfiles.length - 1].getUi()"
            + " && window._xfiles[window._xfiles.length - 1].getUi().getCurrentFile() != null"
            + " && document.querySelector('.geSidebarContainer a.geTitle') != null");
    }

    /**
     * @return a new mouse and keyboard action sequence. The mouse moves are instantaneous: by default the mouse moves
     *     through all the points between its current position and the target, which opens the menus it passes over.
     */
    static Actions actions()
    {
        return new DiagramActions(getUtil().getDriver().getWrappedDriver());
    }

    /**
     * Clicks an element of the editor. Firefox refuses to click directly some of the draw.io elements ("could not be
     * scrolled into view") so we scroll them into view ourselves and then click using the mouse.
     */
    static void click(WebElement element)
    {
        // Center the element because the XWiki form actions (Save & View, etc.) stick to the bottom of the window and
        // can cover the bottom of the editor.
        getUtil().getDriver().executeJavascript(
            "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
        actions().moveToElement(element).click().perform();
    }

    private Object js(String script, Object... arguments)
    {
        return getDriver().executeJavascript(JS_UI + script, arguments);
    }

    // Diagram content

    /**
     * @return the diagram currently displayed by the editor (the current diagram page)
     */
    public DiagramModel getModel()
    {
        return DiagramModel.parse((String) js("return mxUtils.getXml(ui.editor.getGraphXml());"));
    }

    /**
     * @return the number of shapes from the current diagram page
     */
    public int getShapeCount()
    {
        return getModel().getVertices().size();
    }

    /**
     * @return the identifiers of the selected cells
     */
    @SuppressWarnings("unchecked")
    public List<String> getSelectedCellIds()
    {
        return (List<String>) js("return graph.getSelectionCells().map(cell => cell.id);");
    }

    /**
     * @return the current zoom level (1 means 100%)
     */
    public double getZoom()
    {
        return ((Number) js("return graph.view.scale;")).doubleValue();
    }

    /**
     * @return the zoom level displayed in the tool bar (e.g. "100%")
     */
    public String getDisplayedZoom()
    {
        return getToolbarLabel("Zoom").getText().trim();
    }

    /**
     * @return the scroll position of the diagram canvas
     */
    public Point getScrollPosition()
    {
        @SuppressWarnings("unchecked")
        List<Number> position =
            (List<Number>) js("return [graph.container.scrollLeft, graph.container.scrollTop];");
        return new Point(position.get(0).intValue(), position.get(1).intValue());
    }

    /**
     * @param cellId the cell identifier
     * @return the size of the given cell, as displayed on the screen (i.e. taking into account the zoom level)
     */
    public double getDisplayedWidth(String cellId)
    {
        return ((Number) js(JS_CELL + "return graph.view.getState(cell).width;", cellId)).doubleValue();
    }

    // Diagram pages

    /**
     * @return the names of the diagram pages, in the order of the page tabs
     */
    @SuppressWarnings("unchecked")
    public List<String> getPageNames()
    {
        return (List<String>) js("return ui.pages.map(page => page.getName());");
    }

    /**
     * @return the name of the diagram page currently displayed
     */
    public String getCurrentPageName()
    {
        return (String) js("return ui.currentPage.getName();");
    }

    /**
     * Adds a new diagram page using the "Insert Page" (+) tab. The new page becomes the current page.
     *
     * @return the name of the new page
     */
    public String addPage()
    {
        int pageCount = getPageNames().size();
        click(getDriver().findElement(By.cssSelector(".geTabContainer .geControlTab[title='Insert Page']")));
        getDriver().waitUntilCondition(driver -> getPageNames().size() > pageCount);
        return getCurrentPageName();
    }

    /**
     * Displays the given diagram page by clicking on its tab.
     *
     * @param name the page name
     */
    public void selectPage(String name)
    {
        WebElement tab = getDriver().findElementsWithoutWaiting(By.cssSelector(".geTabContainer .geTab")).stream()
            .filter(element -> !element.getAttribute(CLASS).contains("geControlTab")
                && name.equals(element.getText().trim()))
            .findFirst().orElseThrow();
        click(tab);
        getDriver().waitUntilCondition(driver -> name.equals(getCurrentPageName()));
    }

    // Shape library (side bar)

    /**
     * @param title the title of a shape palette (e.g. "General", "Cisco / Routers")
     * @return whether the palette is available in the side bar
     */
    public boolean isPaletteDisplayed(String title)
    {
        return getDriver().findElementsWithoutWaiting(getPaletteTitleLocator(title)).stream()
            .anyMatch(WebElement::isDisplayed);
    }

    /**
     * @param title the title of a shape palette
     * @return whether the palette is expanded (its shapes are visible)
     */
    public boolean isPaletteExpanded(String title)
    {
        WebElement content = getPaletteContent(title);
        return content.isDisplayed() && !getDriver().findElementsWithoutWaiting(content, By.cssSelector(GE_ITEM))
            .isEmpty();
    }

    /**
     * Expands or collapses the given shape palette by clicking on its title.
     *
     * @param title the title of a shape palette
     */
    public void togglePalette(String title)
    {
        boolean expanded = isPaletteExpanded(title);
        click(getPaletteTitle(title));
        getDriver().waitUntilCondition(driver -> isPaletteExpanded(title) != expanded);
    }

    /**
     * Adds a shape from the given palette to the diagram by clicking on it in the side bar. The palette is expanded if
     * needed.
     *
     * @param paletteTitle the title of the shape palette
     * @param index the index of the shape inside the palette
     * @return the identifier of the added shape
     */
    public String addShape(String paletteTitle, int index)
    {
        if (!isPaletteExpanded(paletteTitle)) {
            togglePalette(paletteTitle);
        }
        WebElement shape =
            getDriver().findElementsWithoutWaiting(getPaletteContent(paletteTitle), By.cssSelector(GE_ITEM))
                .get(index);
        return clickShapeAndWaitForInsert(shape);
    }

    /**
     * Searches shapes using the search box from the side bar.
     *
     * @param query the search query
     * @return the number of shapes found
     */
    public int searchShapes(String query)
    {
        WebElement searchInput = getDriver().findElement(By.xpath(SIDEBAR + "//input[@type = 'text']"));
        click(searchInput);
        actions().sendKeys(query, Keys.ENTER).perform();
        getDriver().waitUntilCondition(driver -> !getSearchResults().isEmpty());
        return getSearchResults().size();
    }

    /**
     * Adds to the diagram one of the shapes found using {@link #searchShapes(String)}.
     *
     * @param index the index of the search result
     * @return the identifier of the added shape
     */
    public String addSearchResult(int index)
    {
        return clickShapeAndWaitForInsert(getSearchResults().get(index));
    }

    private List<WebElement> getSearchResults()
    {
        // The search results are displayed between the search input and the first shape palette.
        return getDriver()
            .findElementsWithoutWaiting(By.xpath(String.format("(%s//a[contains(@class, 'geTitle')])[1]"
                + "/preceding::a[contains(@class, 'geItem')][ancestor::div[contains(@class, 'geSidebarContainer')]]",
                SIDEBAR)))
            .stream().filter(WebElement::isDisplayed).collect(Collectors.toList());
    }

    private String clickShapeAndWaitForInsert(WebElement shape)
    {
        int count = getModel().getCells().size();
        click(shape);
        getDriver().waitUntilCondition(driver -> getModel().getCells().size() > count);
        return getSelectedCellIds().get(0);
    }

    /**
     * Opens the dialog used to choose the shape libraries displayed in the side bar.
     *
     * @return the dialog
     */
    public MoreShapesDialog openMoreShapesDialog()
    {
        WebElement button = getDriver().findElement(
            By.xpath(SIDEBAR + "//button[contains(normalize-space(.), 'More Shapes')]"));
        click(button);
        return new MoreShapesDialog();
    }

    private By getPaletteTitleLocator(String title)
    {
        return By.xpath(String.format("%s//a[contains(@class, 'geTitle') and normalize-space(.) = '%s']", SIDEBAR,
            title));
    }

    private WebElement getPaletteTitle(String title)
    {
        // Some palettes have the same title (e.g. "Arrows") but only one of them is displayed.
        List<WebElement> titles = getDriver().findElementsWithoutWaiting(getPaletteTitleLocator(title));
        if (titles.isEmpty()) {
            throw new NoSuchElementException(String.format("Shape palette [%s] not found. Available palettes: %s",
                title, getDriver().findElementsWithoutWaiting(By.xpath(SIDEBAR + "//a[contains(@class, 'geTitle')]"))
                    .stream().filter(WebElement::isDisplayed).map(WebElement::getText).collect(Collectors.toList())));
        }
        return titles.stream().filter(WebElement::isDisplayed).findFirst().orElse(titles.get(0));
    }

    private WebElement getPaletteContent(String title)
    {
        return getPaletteTitle(title).findElement(By.xpath("following-sibling::div[1]"));
    }

    // Canvas interactions

    /**
     * Selects the given cell by clicking on it.
     *
     * @param cellId the cell identifier
     */
    public void selectCell(String cellId)
    {
        clickOnCell(cellId);
        getDriver().waitUntilCondition(driver -> getSelectedCellIds().contains(cellId));
    }

    /**
     * Clicks on the given cell, without waiting for it to be selected (e.g. locked cells can't be selected).
     *
     * @param cellId the cell identifier
     */
    public void clickOnCell(String cellId)
    {
        Point center = getCellCenter(cellId);
        moveToCanvas(center.getX(), center.getY()).click().perform();
    }

    /**
     * Clears the selection using Edit > Select None.
     */
    public void selectNone()
    {
        clickMenuItem(EDIT_MENU, "Select None");
        getDriver().waitUntilCondition(driver -> getSelectedCellIds().isEmpty());
    }

    private void stopTextEditing()
    {
        actions().keyDown(Keys.CONTROL).sendKeys(Keys.ENTER).keyUp(Keys.CONTROL).perform();
        getDriver().waitUntilCondition(driver -> ((Boolean) js("return !graph.isEditing();")));
    }

    /**
     * Double clicks on the given shape and types the given text, which replaces the shape label. The text editing is
     * stopped by clicking on an empty area of the canvas.
     *
     * @param cellId the shape identifier
     * @param text the text to type
     */
    public void setShapeText(String cellId, String text)
    {
        Point center = getCellCenter(cellId);
        moveToCanvas(center.getX(), center.getY()).doubleClick().perform();
        WebElement textEditor = getDriver().findElement(By.cssSelector(".mxCellEditor"));
        getDriver().waitUntilCondition(driver -> textEditor.isDisplayed());
        // Select the existing text in order to replace it.
        actions().keyDown(Keys.CONTROL).sendKeys("a").keyUp(Keys.CONTROL)
            .sendKeys(text).perform();
        stopTextEditing();
        getDriver().waitUntilCondition(
            driver -> getModel().getCell(cellId).map(cell -> cell.getValue().contains(text)).orElse(false));
    }

    /**
     * Resizes the selected shape by dragging its bottom right handle.
     *
     * @param cellId the identifier of the selected shape
     * @param deltaX the horizontal distance to drag the handle
     * @param deltaY the vertical distance to drag the handle
     */
    public void resizeSelectedShape(String cellId, int deltaX, int deltaY)
    {
        // The bottom right sizer is the last one (draw.io orders the sizers from top left to bottom right).
        dragHandle(cellId, "handler.sizers[7].node", deltaX, deltaY);
    }

    /**
     * Rotates the selected shape by dragging its rotation handle.
     *
     * @param cellId the identifier of the selected shape
     * @param deltaX the horizontal distance to drag the rotation handle
     * @param deltaY the vertical distance to drag the rotation handle
     */
    public void rotateSelectedShape(String cellId, int deltaX, int deltaY)
    {
        dragHandle(cellId, "handler.rotationShape.node", deltaX, deltaY);
    }

    private void dragHandle(String cellId, String handleExpression, int deltaX, int deltaY)
    {
        @SuppressWarnings("unchecked")
        List<Number> handle = (List<Number>) js(JS_CELL
            + "var handler = graph.selectionCellsHandler.getHandler(cell);"
            + "var rect = " + handleExpression + ".getBoundingClientRect();"
            + "var container = graph.container.getBoundingClientRect();"
            + "return [rect.left + rect.width / 2 - container.left, rect.top + rect.height / 2 - container.top];",
            cellId);
        moveToCanvas(handle.get(0).intValue(), handle.get(1).intValue()).clickAndHold().moveByOffset(deltaX / 2,
            deltaY / 2).moveByOffset(deltaX - deltaX / 2, deltaY - deltaY / 2).release().perform();
    }

    /**
     * Scrolls the diagram canvas using the mouse wheel.
     *
     * @param deltaX the horizontal scroll amount
     * @param deltaY the vertical scroll amount
     */
    public void scrollCanvas(int deltaX, int deltaY)
    {
        WebElement container = getCanvas();
        actions()
            .scrollFromOrigin(WheelInput.ScrollOrigin.fromElement(container), deltaX, deltaY).perform();
    }

    /**
     * Deletes the given cell using the context menu.
     *
     * @param cellId the cell identifier
     */
    public void deleteCellUsingContextMenu(String cellId)
    {
        selectCell(cellId);
        Point center = getCellCenter(cellId);
        moveToCanvas(center.getX(), center.getY()).contextClick().perform();
        clickPopupMenuItem(getLastPopupMenu(), "Delete");
        getDriver().waitUntilCondition(driver -> getModel().getCell(cellId).isEmpty());
    }

    private Point getCellCenter(String cellId)
    {
        @SuppressWarnings("unchecked")
        List<Number> center = (List<Number>) js(JS_CELL + "graph.scrollCellToVisible(cell);"
            + "var rect = graph.view.getState(cell).shape.node.getBoundingClientRect();"
            + "var container = graph.container.getBoundingClientRect();"
            + "return [rect.left + rect.width / 2 - container.left, rect.top + rect.height / 2 - container.top];",
            cellId);
        return new Point(center.get(0).intValue(), center.get(1).intValue());
    }

    private WebElement getCanvas()
    {
        WebElement container = (WebElement) js("graph.container.scrollIntoView({block: 'center'});"
            + " return graph.container;");
        return container;
    }

    /**
     * Moves the mouse inside the canvas, at the given position relative to the top left corner of the canvas.
     */
    private Actions moveToCanvas(int x, int y)
    {
        WebElement container = getCanvas();
        // The offset is computed from the center of the element.
        int offsetX = x - container.getSize().getWidth() / 2;
        int offsetY = y - container.getSize().getHeight() / 2;
        return actions().moveToElement(container, offsetX, offsetY);
    }

    // Tool bar

    /**
     * Clicks the Undo tool bar button.
     */
    public void undo()
    {
        clickToolbarButton("Undo");
    }

    /**
     * Clicks the Redo tool bar button.
     */
    public void redo()
    {
        clickToolbarButton("Redo");
    }

    /**
     * Clicks the Zoom In tool bar button.
     */
    public void zoomIn()
    {
        double zoom = getZoom();
        clickToolbarButton("Zoom In");
        getDriver().waitUntilCondition(driver -> getZoom() > zoom);
    }

    /**
     * Clicks the Zoom Out tool bar button.
     */
    public void zoomOut()
    {
        double zoom = getZoom();
        clickToolbarButton("Zoom Out");
        getDriver().waitUntilCondition(driver -> getZoom() < zoom);
    }

    /**
     * Clicks the To Front tool bar button.
     */
    public void toFront()
    {
        clickToolbarButton("To Front");
    }

    /**
     * Clicks the To Back tool bar button.
     */
    public void toBack()
    {
        clickToolbarButton("To Back");
    }

    /**
     * Clicks the Delete tool bar button.
     */
    public void deleteSelection()
    {
        clickToolbarButton("Delete");
    }

    /**
     * Resets the view using the zoom drop down from the tool bar.
     */
    public void resetViewFromZoomMenu()
    {
        closeMenus();
        showEditorAtTop();
        click(getToolbarLabel("Zoom"));
        clickPopupMenuItem(getLastPopupMenu(), "Fit Window / Reset View");
        getDriver().waitUntilCondition(driver -> getZoom() == 1);
    }

    /**
     * Inserts an image using the Insert drop down from the tool bar.
     *
     * @param imageURL the URL of the image to insert
     * @return the identifier of the inserted image shape
     */
    public String insertImage(String imageURL)
    {
        closeMenus();
        int count = getModel().getCells().size();
        showEditorAtTop();
        click(getToolbarLabel("Insert"));
        clickPopupMenuItem(getLastPopupMenu(), "Image...");
        WebElement dialog = getLastDialog();
        WebElement urlInput = dialog.findElement(By.tagName("input"));
        urlInput.clear();
        urlInput.sendKeys(imageURL);
        clickDialogButton(dialog, APPLY);
        getDriver().waitUntilCondition(driver -> getModel().getCells().size() > count);
        return getSelectedCellIds().get(0);
    }

    private void clickToolbarButton(String title)
    {
        WebElement button = getDriver().findElement(By.xpath(
            String.format("//div[contains(@class, 'geToolbar')]//a[starts-with(@title, '%s')]", title)));
        getDriver().waitUntilCondition(driver -> !button.getAttribute(CLASS).contains(DISABLED));
        click(button);
    }

    private WebElement getToolbarLabel(String title)
    {
        return getDriver().findElement(By.xpath(String
            .format("//div[contains(@class, 'geToolbar')]//a[contains(@class, 'geLabel') and starts-with(@title, '%s')]",
                title)));
    }

    // Menu bar

    /**
     * @param menu the name of a menu from the menu bar (e.g. "File", "Edit")
     * @return the labels of the enabled entries of the given menu
     */
    public List<String> getMenuItems(String menu)
    {
        WebElement popup = openMenu(menu);
        List<String> items = getDriver().findElementsWithoutWaiting(popup, By.cssSelector("tr.mxPopupMenuItem"))
            .stream().map(row -> row.findElements(By.tagName("td")))
            .filter(cells -> cells.size() > 1 && !cells.get(1).getAttribute(CLASS).contains(DISABLED))
            .map(cells -> cells.get(1).getText().trim()).collect(Collectors.toList());
        closeMenus();
        return items;
    }

    /**
     * Clicks on a menu entry. The first element of the path is the name of the menu from the menu bar, the following
     * ones are the labels of the (sub-)menu entries.
     *
     * @param path the path to the menu entry (e.g. "File", "Export as", "URL...")
     */
    public void clickMenuItem(String... path)
    {
        WebElement popup = openMenu(path[0]);
        for (int i = 1; i < path.length - 1; i++) {
            int popupCount = getVisiblePopupMenus().size();
            WebElement item = popup.findElement(By.xpath(String.format(POPUP_MENU_ITEM, path[i])));
            // Sub-menus are opened when hovering their parent entry. Note that we must not scroll the page while a
            // menu is opened because this closes the menu.
            actions().moveToElement(item).perform();
            getDriver().waitUntilCondition(driver -> getVisiblePopupMenus().size() > popupCount);
            popup = getLastPopupMenu();
        }
        clickPopupMenuItem(popup, path[path.length - 1]);
    }

    private WebElement openMenu(String menu)
    {
        closeMenus();
        WebElement menuItem = getDriver().findElement(By.xpath(
            String.format("//div[contains(@class, 'geMenubar')]/a[contains(@class, 'geItem') and normalize-space(.) = '%s']",
                menu)));
        // Make sure the menu (which can be long) fits in the window, because we can't scroll while it's opened.
        showEditorAtTop();
        actions().moveToElement(menuItem).click().perform();
        getDriver().waitUntilCondition(driver -> !getVisiblePopupMenus().isEmpty());
        return getLastPopupMenu();
    }

    private void clickPopupMenuItem(WebElement popup, String label)
    {
        WebElement item = popup.findElement(By.xpath(String.format(POPUP_MENU_ITEM, label)));
        actions().moveToElement(item).click().perform();
    }

    private List<WebElement> getVisiblePopupMenus()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(VISIBLE_POPUP_MENUS)).stream()
            .filter(WebElement::isDisplayed).collect(Collectors.toList());
    }

    private WebElement getLastPopupMenu()
    {
        getDriver().waitUntilCondition(driver -> !getVisiblePopupMenus().isEmpty());
        List<WebElement> popups = getVisiblePopupMenus();
        return popups.get(popups.size() - 1);
    }

    private void showEditorAtTop()
    {
        js("ui.menubarContainer.scrollIntoView({block: 'start'});");
    }

    private void closeMenus()
    {
        // Clicking again on the menu bar toggles the menu, so we close the opened menus explicitly.
        js("ui.hideCurrentMenu();");
    }

    // Menu actions

    /**
     * Selects all the cells using Edit > Select All.
     */
    public void selectAll()
    {
        clickMenuItem(EDIT_MENU, "Select All");
    }

    /**
     * Deletes the selected cells using Edit > Delete. Nothing happens if the selected cells can't be deleted (e.g.
     * because they are locked).
     */
    public void deleteSelectionUsingMenu()
    {
        clickMenuItem(EDIT_MENU, "Delete");
        closeMenus();
    }

    /**
     * Locks or unlocks the selected cells using Edit > Lock/Unlock.
     */
    public void lockUnlockSelection()
    {
        clickMenuItem(EDIT_MENU, "Lock/Unlock");
    }

    /**
     * Rotates the selected shape by 90 degrees using Arrange > Rotate shape only by 90° / Reverse.
     */
    public void rotateSelectionBy90()
    {
        clickMenuItem(ARRANGE_MENU, "Rotate shape only by 90° / Reverse");
    }

    /**
     * Brings the selected shapes to the front using Arrange > To Front.
     */
    public void bringToFrontUsingMenu()
    {
        clickMenuItem(ARRANGE_MENU, "To Front");
    }

    /**
     * Opens File > Page Setup...
     *
     * @return the page setup dialog
     */
    public PageSetupDialog openPageSetup()
    {
        clickMenuItem(FILE_MENU, "Page Setup...");
        return new PageSetupDialog(getLastDialog());
    }

    /**
     * Exports the diagram using File > Export as > URL...
     *
     * @return the URL generated for the diagram
     */
    public String exportAsURL()
    {
        clickMenuItem(FILE_MENU, "Export as", URL);
        clickDialogButton(getLastDialog(), "Create");
        WebElement linkDialog = getDriver().findElement(By.xpath("//div[contains(@class, 'geDialog')][.//textarea]"));
        String url = linkDialog.findElement(By.tagName("textarea")).getAttribute(VALUE);
        clickDialogButton(linkDialog, "Close");
        waitUntilNoDialog();
        return url;
    }

    /**
     * Opens the dialog used to export the diagram in the given format, from File > Export as.
     *
     * @param format the format, as displayed in the menu (e.g. "PNG...", "JPEG...", "WebP...", "SVG...", "XML...",
     *     "VSDX (beta)...")
     * @return the export dialog
     */
    public DiagramExportDialog openExportDialog(String format)
    {
        DiagramExportDialog.interceptDownloads();
        clickMenuItem(FILE_MENU, "Export as", format);
        return new DiagramExportDialog(getLastDialog());
    }

    /**
     * Opens File > Print / Export as PDF.
     *
     * @return the print dialog
     */
    public DiagramExportDialog openPrintDialog()
    {
        clickMenuItem(FILE_MENU, "Print / Export as PDF");
        return new DiagramExportDialog(getLastDialog());
    }

    /**
     * Imports a diagram using File > Import from > URL...
     *
     * @param url the URL of the diagram to import
     */
    public void importFromURL(String url)
    {
        int count = getModel().getCells().size();
        clickMenuItem(FILE_MENU, IMPORT_FROM, URL);
        WebElement dialog = getLastDialog();
        WebElement urlInput = dialog.findElement(By.tagName("input"));
        getDriver().waitUntilCondition(driver -> urlInput.isDisplayed());
        urlInput.sendKeys(url);
        clickDialogButton(dialog, "Import");
        getDriver().waitUntilCondition(driver -> getModel().getCells().size() > count);
    }

    /**
     * Imports a file (a diagram or an image) using File > Import from > Device...
     *
     * @param file the file to import
     */
    public void importFromDevice(File file)
    {
        int count = getModel().getCells().size();
        // The browser file picker can't be controlled with Selenium so we intercept the click on the (hidden) file
        // input and then we set the file directly on the file input, which is what the browser does when the user
        // selects a file.
        getDriver().executeJavascript("var original = HTMLInputElement.prototype.click;"
            + "HTMLInputElement.prototype.click = function() {"
            + "  if (this.type === 'file') {"
            + "    HTMLInputElement.prototype.click = original;"
            + "    this.classList.add('diagram-test-file-picker');"
            + "  } else {"
            + "    original.apply(this, arguments);"
            + "  }"
            + "};");
        clickMenuItem(FILE_MENU, IMPORT_FROM, "Device...");
        // The file detector uploads the file to the browser machine. It must be set before finding the file input.
        getDriver().setFileDetector(new LocalFileDetector());
        WebElement fileInput = getDriver().findElement(By.cssSelector("input.diagram-test-file-picker"));
        fileInput.sendKeys(file.getAbsolutePath());
        getDriver().waitUntilCondition(driver -> getModel().getCells().size() > count);
    }

    // Format panel

    /**
     * @return the format panel displayed on the right side of the editor
     */
    public DiagramFormatPanel getFormatPanel()
    {
        return new DiagramFormatPanel();
    }

    // Dialogs

    private WebElement getLastDialog()
    {
        getDriver().waitUntilCondition(driver -> !getVisibleDialogs().isEmpty());
        List<WebElement> dialogs = getVisibleDialogs();
        return dialogs.get(dialogs.size() - 1);
    }

    private List<WebElement> getVisibleDialogs()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(".geDialog")).stream()
            .filter(WebElement::isDisplayed).collect(Collectors.toList());
    }

    private void waitUntilNoDialog()
    {
        getDriver().waitUntilCondition(driver -> getVisibleDialogs().isEmpty());
    }

    static void clickDialogButton(WebElement dialog, String label)
    {
        dialog.findElement(By.xpath(String.format(".//button[normalize-space(.) = '%s']", label))).click();
    }
}
