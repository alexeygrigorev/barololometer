package barololometer.scrape;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class BingScraper implements SearchEngineScraper {

    private static final Logger LOGGER = LoggerFactory.getLogger(BingScraper.class);
    private static final String BING_URL = "https://www.bing.com/search";
    private static final String BING_COUNTRY = "us";

    @Override
    public SearchResults scrape(String query) {
        String url = baseUrl(query);
        LOGGER.info("scraping page {} for {}, url: {}", query, url);

        String html = HttpUtils.userAgentRequest(url);
        List<RankedPage> foundPages = parseContent(query, html, 1, 1);

        SearchResults searchResults = new SearchResults();
        searchResults.setQuery(query);
        searchResults.setPages(foundPages);
        searchResults.setSearchEngine(SearchEngine.BING);
        searchResults.setParam("page", 1);
        searchResults.setParam("position", foundPages.size());

        return searchResults;
    }

    private static String baseUrl(String query) {
        return BING_URL + "?cc=" + BING_COUNTRY + "&q=" + query.toLowerCase().replace(' ', '+');
    }

    private List<RankedPage> parseContent(String query, String html, int positionStart, int pageNo) {
        int position = positionStart;
        List<RankedPage> foundPages = Lists.newArrayListWithExpectedSize(10);

        Document document = Jsoup.parse(html);
        Elements blocks = document.select("ol#b_results li.b_algo");
        for (Element element : blocks) {
            Elements link = element.select("h2 a");
            String href = link.attr("href");
            String title = link.text();

            String snippet = element.select("div.b_caption p").text();

            RankedPage page = new RankedPage();
            page.setPage(1);
            page.setPosition(position);
            page.setUrl(href);
            page.setQuery(query);
            page.setTitle(title);
            page.setSnippet(snippet);
            page.setSearchEngine(SearchEngine.BING);

            foundPages.add(page);
            position++;
        }

        return foundPages;
    }

    @Override
    public SearchResults nextPage(SearchResults previos) {
        Validate.isTrue(SearchEngine.BING.equals(previos.getSearchEngine()), "the previous results must be from BING");

        String query = previos.getQuery();
        int nextResultOffset = previos.getIntParam("position") + 1;
        String url = baseUrl(query) + "&first=" + nextResultOffset;

        int newPage = previos.getIntParam("page") + 1;

        LOGGER.info("scraping page {} for {}, page url: {}", query, url);
        String html = HttpUtils.userAgentRequest(url);
        List<RankedPage> foundPages = parseContent(query, html, nextResultOffset, newPage);

        SearchResults searchResults = new SearchResults();
        searchResults.setQuery(query);
        searchResults.setPages(foundPages);
        searchResults.setSearchEngine(SearchEngine.BING);
        searchResults.setParam("page", newPage);

        int newPosition = nextResultOffset + foundPages.size();
        searchResults.setParam("position", newPosition);

        return searchResults;
    }

    public static void main(String[] args) {
        BingScraper bingScraper = new BingScraper();
        SearchResults first = bingScraper.scrape("kaggle");
        SearchResults next = bingScraper.nextPage(first);

        Iterable<RankedPage> allPages = Iterables.concat(first.getPages(), next.getPages());
        allPages.forEach(System.out::println);

        bingScraper.close();
    }

    @Override
    public void close() {
    }

}
