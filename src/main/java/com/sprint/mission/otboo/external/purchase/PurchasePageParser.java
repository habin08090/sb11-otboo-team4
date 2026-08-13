package com.sprint.mission.otboo.external.purchase;

import com.sprint.mission.otboo.external.purchase.dto.PurchasePageResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class PurchasePageParser {

  public PurchasePageResponse parse(String html) {
    if (html == null || html.isBlank()) {
      return new PurchasePageResponse(null, null, null, null);
    }

    Document doc = Jsoup.parse(html);

    String title = getOgContent(doc, "og:title");
    String imageUrl = getOgContent(doc, "og:image");
    String description = getOgContent(doc, "og:description");
    String siteName = getOgContent(doc, "og:site_name");

    return new PurchasePageResponse(title, imageUrl, description, siteName);
  }

  private String getOgContent(Document doc, String property) {
    return doc.select("meta[property=" + property + "]")
        .stream()
        .findFirst()
        .map(element -> element.attr("content"))
        .filter(content -> !content.isBlank())
        .orElse(null);
  }
}
