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

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.BaseElement;

/**
 * The dialog displayed when exporting a diagram (File > Export as > PNG..., SVG..., etc.) or printing it (File > Print
 * / Export as PDF).
 *
 * @version $Id$
 * @since 2.1.4
 */
public class DiagramExportDialog extends BaseElement
{
    private static final String DOWNLOADS = "window.__diagramTestDownloads";

    private static final String DOWNLOAD = "download";

    private final WebElement container;

    /**
     * @param container the dialog element
     */
    public DiagramExportDialog(WebElement container)
    {
        this.container = container;
    }

    /**
     * Intercepts the files downloaded by the browser. Browsers save the downloaded files on the disk of the machine
     * where they run, which is not accessible to the tests, so we read the content of the downloaded files instead.
     */
    static void interceptDownloads()
    {
        getUtil().getDriver().executeJavascript("if (!" + DOWNLOADS + ") {"
            + "  " + DOWNLOADS + " = [];"
            + "  const originalClick = HTMLAnchorElement.prototype.click;"
            + "  HTMLAnchorElement.prototype.click = function() {"
            + "    if (!this.hasAttribute('download')) {"
            + "      return originalClick.apply(this, arguments);"
            + "    }"
            + "    const name = this.download;"
            + "    fetch(this.href).then(response => response.blob()).then(blob => new Promise(resolve => {"
            + "      const reader = new FileReader();"
            + "      reader.onload = () => resolve(reader.result);"
            + "      reader.readAsDataURL(blob);"
            + "    })).then(dataURL => " + DOWNLOADS + ".push({name: name,"
            + "      content: dataURL.substring(dataURL.indexOf(',') + 1)}));"
            + "  };"
            + "}");
    }

    /**
     * @return the labels of the export options (check boxes)
     */
    @SuppressWarnings("unchecked")
    public List<String> getOptions()
    {
        return (List<String>) getDriver().executeJavascript("return Array.from(arguments[0]"
            + ".querySelectorAll('input[type=checkbox]')).map(checkbox => {"
            + "  let label = '';"
            + "  for (let node = checkbox.nextSibling; node && !label; node = node.nextSibling) {"
            + "    label = (node.textContent || '').trim();"
            + "  }"
            + "  return label;"
            + "});", this.container);
    }

    /**
     * Checks or un-checks an export option.
     *
     * @param label the option label (e.g. "Selection Only", "Transparent Background")
     * @param checked whether the option should be checked
     */
    public void setOption(String label, boolean checked)
    {
        int index = getOptions().indexOf(label);
        if (index < 0) {
            throw new IllegalArgumentException(String.format("Unknown export option [%s]: %s", label, getOptions()));
        }
        WebElement checkbox = this.container.findElements(By.cssSelector("input[type='checkbox']")).get(index);
        if (checkbox.isSelected() != checked) {
            DiagramEditor.click(checkbox);
        }
    }

    /**
     * Exports the diagram and downloads the result. When the export asks where to save the file, the file is
     * downloaded with the given name.
     *
     * @param fileName the name of the downloaded file
     * @return the downloaded file
     */
    public DownloadedFile export(String fileName)
    {
        int downloadCount = getDownloadCount();
        // The primary button is labeled "Export" for most formats.
        this.container.findElement(By.cssSelector("button.gePrimaryBtn")).click();
        // Most formats ask where to save the file, but some are downloaded directly.
        getDriver().waitUntilCondition(driver -> getDownloadCount() > downloadCount || getSaveDialog() != null);
        WebElement saveDialog = getSaveDialog();
        if (saveDialog != null) {
            WebElement nameInput = saveDialog.findElement(By.cssSelector("input[type='text']"));
            nameInput.clear();
            nameInput.sendKeys(fileName);
            new Select(saveDialog.findElement(By.tagName("select"))).selectByValue(DOWNLOAD);
            DiagramEditor.clickDialogButton(saveDialog, "OK");
        }
        getDriver().waitUntilCondition(driver -> getDownloadCount() > downloadCount);
        @SuppressWarnings("unchecked")
        Map<String, String> download =
            (Map<String, String>) getDriver().executeJavascript("return " + DOWNLOADS + "[arguments[0]];",
                downloadCount);
        return new DownloadedFile(download.get("name"), Base64.getDecoder().decode(download.get("content")));
    }

    /**
     * Opens the print preview (in a new window) using the Preview button of the print dialog, reads it and closes
     * it.
     *
     * @return the print preview
     */
    public PrintPreview preview()
    {
        XWikiWebDriver driver = getDriver();
        String originalWindow = driver.getWindowHandle();
        Set<String> windows = driver.getWindowHandles();
        // The preview window is opened only for real user clicks.
        DiagramEditor.click(this.container.findElement(By.xpath(".//button[normalize-space(.) = 'Preview']")));
        driver.waitUntilCondition(d -> driver.getWindowHandles().size() > windows.size());
        Set<String> newWindows = new HashSet<>(driver.getWindowHandles());
        newWindows.removeAll(windows);
        driver.switchTo().window(newWindows.iterator().next());
        try {
            driver.waitUntilCondition(d -> !driver.findElements(By.tagName("svg")).isEmpty());
            return new PrintPreview(driver.findElement(By.tagName("body")).getText(),
                driver.findElements(By.tagName("svg")).size());
        } finally {
            driver.close();
            driver.switchTo().window(originalWindow);
        }
    }

    private int getDownloadCount()
    {
        return ((Number) getDriver().executeJavascript("return " + DOWNLOADS + ".length;")).intValue();
    }

    private WebElement getSaveDialog()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector(".geDialog")).stream()
            .filter(dialog -> dialog.isDisplayed()
                && !dialog.findElements(By.cssSelector("select option[value='" + DOWNLOAD + "']")).isEmpty())
            .findFirst().orElse(null);
    }
}
