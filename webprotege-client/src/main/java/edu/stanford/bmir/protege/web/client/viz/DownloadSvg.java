package edu.stanford.bmir.protege.web.client.viz;

import com.google.common.io.BaseEncoding;
import com.google.gwt.user.client.Window;
import elemental.client.Browser;
import elemental.css.*;
import elemental.dom.Element;
import elemental.stylesheets.StyleSheet;
import elemental.stylesheets.StyleSheetList;
import elemental.svg.SVGStyleElement;

import javax.annotation.Nonnull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 12 Oct 2018
 */
public class DownloadSvg {

    public void save(@Nonnull Element element, @Nonnull String fileName) {
        download(element, fileName);
    }


    private static void download(Element element, String fileName) {
        // First convert the image to a data URI
        Element svgCopy = (Element) element.cloneNode(true);
        SVGStyleElement styleElement = Browser.getDocument().createSVGStyleElement();
        StyleSheetList styleSheets = Browser.getDocument().getStyleSheets();
        StringBuilder rules = new StringBuilder();
        for(int styleSheetIndex = 0; styleSheetIndex < styleSheets.length(); styleSheetIndex++) {
            CSSStyleSheet styleSheet = (CSSStyleSheet) styleSheets.item(styleSheetIndex);
            CSSRuleList ruleList = styleSheet.getCssRules();
            if (ruleList != null) {
                for(int ruleIndex = 0; ruleIndex < ruleList.getLength(); ruleIndex++) {
                    CSSRule rule = ruleList.item(ruleIndex);
                    String ruleText = rule.getCssText();
                    if(ruleText.startsWith(".wp-graph")) {
                        rules.append(ruleText);
                    }
                }
            }
        }
        CSSStyleDeclaration computedStyle = Browser.getWindow().getComputedStyle(element, null);
        rules.append("\n");
        rules.append(":root {--primary--background-color: white;}\n");
        rules.append("text {\n");
        for(int i = 0; i < computedStyle.getLength(); i++) {
            String property = computedStyle.item(i);
            if (property.startsWith("font")) {
                CSSValue value = computedStyle.getPropertyCSSValue(property);
                rules.append(property);
                rules.append(":");
                rules.append(value.getCssText());
                rules.append(";\n");
            }
        }
        rules.append("}\n");
        styleElement.setTextContent(rules.toString());

        svgCopy.insertBefore(styleElement, svgCopy.getFirstChild());
        svgCopy.setAttribute("xmlns", "http://www.w3.org/2000/svg");
        String svgHtml = "<?xml version=\"1.0\" standalone=\"no\"?>" + svgCopy.getOuterHTML();
        String base64Encoding = BaseEncoding.base64().encode(svgHtml.getBytes());
        String dataIri = "data:image/svg+xml;base64," + base64Encoding;

        // Now create an anchor element and set it to download the image
        Element a = Browser.getDocument().createElement("a");
        a.setAttribute("href", dataIri);
        a.setAttribute("href-lang", "image/svg+xml");
        a.setAttribute("target", "_blank");
        a.setAttribute("download", fileName);
        a.click();
    }
}
