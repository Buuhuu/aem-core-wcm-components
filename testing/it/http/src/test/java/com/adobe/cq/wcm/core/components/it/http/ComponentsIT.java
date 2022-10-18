package com.adobe.cq.wcm.core.components.it.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.clients.ClientException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.assertEquals;

public class ComponentsIT {

    @ClassRule
    public static final CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    static CQClient adminAuthor;

    @BeforeClass
    public static void beforeClass() {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
    }

    @Test
    public void testTeaser() throws ClientException, IOException {
        String content = adminAuthor.doGet("/content/core-components/teaser.html", 200).getContent();


        testComponent(content, ImmutableMap.of(
            ".teaser.teaser-v1", Arrays.asList("/components/teaser-v1-with-link-to-asset.html"),
            ".teaser.teaser-v2", Arrays.asList("/components/teaser-v2-with-link-to-asset.html")
        ));
    }

    private void testComponent(String actualContent, Map<String, List<String>> selectorAndExpectations) throws IOException {
        Document document = parse(actualContent);

        for(Map.Entry<String, List<String>> selectorAndExpectation : selectorAndExpectations.entrySet()) {
            String selector = selectorAndExpectation.getKey();
            List<String> expectations = selectorAndExpectation.getValue();
            for (int i = 0; i < expectations.size(); i++) {
                String expectation = expectations.get(i);
                String expected = IOUtils.resourceToString(expectation, StandardCharsets.UTF_8);
                Document expectedDocument = parse(expected);

                assertEquals(
                    selector + "[" + i + "] does not match " + expectation,
                    expectedDocument.select(selector).first().toString(),
                    document.select(selector).get(i).toString()
                );
            }
        }
    }

    private Document parse(String content) {
        // normalize date times
        content =  content.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(.\\d{2,4})?", "0000-00-00T00:00:00");

        Document document = Jsoup.parse(content);
        removeNoise(document.select("html").first());
        return document;
    }

    private void removeNoise(Node element) {
        for (int i = 0; i < element.childNodeSize();) {
            Node child = element.childNode(i);
            // remove nodes that are: comments, cq tags
            if (StringUtils.equalsAny(child.nodeName(), "#comment", "cq")) {
                child.remove();
                continue;
            }
            // remove empty text nodes
            if (StringUtils.equals(child.nodeName(), "#text") && StringUtils.isBlank(((TextNode) child).text())) {
                child.remove();
                continue;
            }
            // normalize img src attributes
            if (StringUtils.equals(child.nodeName(), "img") || StringUtils.equals(child.attr("data-cmp-is"), "image")) {
                for (String attr : new String[] { "src", "data-cmp-src" }) {
                    String src = child.attr(attr);
                    if (StringUtils.isNotEmpty(src)) {
                        src = src.replaceAll("/\\d+/", "/0/");
                        child.attr(attr, src);
                    }
                }
            }

            // recurse
            removeNoise(child);
            i++;
        }
    }
}
