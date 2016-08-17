package barololometer.scrape;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.collect.Lists;

public class DuckDuckGoScraper implements SearchEngineScraper {

    @Override
    public SearchResults scrape(String query) {
        String url = "https://duckduckgo.com/lite?q=" + HttpUtils.encode(query.toLowerCase());

        List<RankedPage> foundPages = Lists.newArrayListWithCapacity(30);
        String html = HttpUtils.userAgentRequest(url);
        Document document = Jsoup.parse(html);
        Elements resultTable = document.select("table tbody:has(a.result-link) tr:not(.result-sponsored)");

        List<List<Element>> blocks = Lists.partition(resultTable, 4);

        int position = 1;
        for (List<Element> block : blocks) {
            if (block.size() != 4) {
                continue;
            }

            Elements elements = new Elements(block);
            Elements link = elements.select("a.result-link");
            String href = link.attr("href");
            String title = link.text();
            String snippet = elements.select("td.result-snippet").text();

            RankedPage page = new RankedPage();
            page.setPage(1);
            page.setPosition(position);
            page.setUrl(href);
            page.setQuery(query);
            page.setTitle(title);
            page.setSnippet(snippet);
            page.setSearchEngine(SearchEngine.DUCK_DUCK_GO);

            foundPages.add(page);

            position++;
        }

        SearchResults searchResults = new SearchResults();
        searchResults.setQuery(query);
        searchResults.setPages(foundPages);
        searchResults.setSearchEngine(SearchEngine.DUCK_DUCK_GO);
        return searchResults;
    }

    @Override
    public SearchResults nextPage(SearchResults previos) {
        return SearchResults.NULL;
    }

    public static void main(String[] args) {
        DuckDuckGoScraper scraper = new DuckDuckGoScraper();
        SearchResults scrape = scraper.scrape("apple");
        List<RankedPage> pages = scrape.getPages();
        pages.forEach(System.out::println);

        scraper.close();
    }

    @Override
    public void close() {
    }
}
