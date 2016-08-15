package barololometer.scrape;

import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.apache.tomcat.util.net.URL;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class GoogleScraper implements SearchEngineScraper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleScraper.class);
    private static final String BASE_URL = "https://www.google.com/search?hl=en&gl=en";

    private final WebDriver driver;

    private GoogleScraper(WebDriver driver) {
        this.driver = driver;
    }

    public static GoogleScraper phantomJs(String phantomJsBinary) {
        System.setProperty("phantomjs.binary.path", phantomJsBinary);
        DesiredCapabilities phantomjs = DesiredCapabilities.phantomjs();
        WebDriver driver = new PhantomJSDriver(phantomjs);
        return new GoogleScraper(driver);
    }

    @Override
    public SearchResults scrape(String query) {
        String url = BASE_URL + "&q=" + query.toLowerCase().replace(' ', '+');
        LOGGER.info("scraping page {} for {}, url: {}", query, url);
        return parseContent(query, url, 1, 1);
    }

    private SearchResults parseContent(String query, String url, int positionStart, int pageNo) {
        driver.get(url);
        int position = positionStart;
        List<RankedPage> foundPages = Lists.newArrayListWithExpectedSize(10);

        sleep();

        List<WebElement> blocks = driver.findElements(By.cssSelector("div.g"));

        for (WebElement element : blocks) {
            try {
                Optional<RankedPage> page = tryParse(query, element, pageNo, position);
                if (!page.isPresent()) {
                    continue;
                }

                foundPages.add(page.get());
                position++;
            } catch (Exception e) {
                LOGGER.warn("got exception while parsing: {}. {}", element, e.getMessage());
            }
        }

        SearchResults searchResults = new SearchResults();
        searchResults.setQuery(query);
        searchResults.setPages(foundPages);
        searchResults.setSearchEngine(SearchEngine.GOOGLE);
        searchResults.setParam("page", pageNo);
        searchResults.setParam("position", foundPages.size());

        return searchResults;
    }

    private Optional<RankedPage> tryParse(String query, WebElement resultBlock, int pageNo, int position) {
        WebElement link = resultBlock.findElement(By.cssSelector("h3.r a"));
        String href = extractAddress(link.getAttribute("href"));
        String title = link.getText();

        List<WebElement> snippetElement = resultBlock.findElements(By.cssSelector(".st"));
        if (snippetElement.isEmpty()) {
            return Optional.empty();
        }

        String snippet = snippetElement.get(0).getText();

        RankedPage page = new RankedPage();
        page.setPage(pageNo);
        page.setPosition(position);
        page.setUrl(href);
        page.setQuery(query);
        page.setTitle(title);
        page.setSnippet(snippet.replace('\n', ' '));
        page.setSearchEngine(SearchEngine.GOOGLE);

        return Optional.of(page);
    }

    private String extractAddress(String href) {
        try {
            URL url = new URL(href);

            Map<String, String> params = new LinkedHashMap<String, String>();
            String query = url.getQuery();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] split = pair.split("=");
                String key = URLDecoder.decode(split[0], "UTF-8");
                String value = URLDecoder.decode(split[1], "UTF-8");
                params.put(key, value);
            }

            return params.get("q");
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public SearchResults nextPage(SearchResults previos) {
        Validate.isTrue(SearchEngine.GOOGLE.equals(previos.getSearchEngine()),
                "the previous results must be from GOOGLE");

        String query = previos.getQuery();
        int start = previos.getIntParam("position");
        int newPage = previos.getIntParam("page") + 1;

        String url = BASE_URL + "&q=" + query.toLowerCase().replace(' ', '+') + "&start=" + start;
        LOGGER.info("scraping page {} for {}, url: {}", query, url);
        return parseContent(query, url, start + 1, newPage);
    }

    @Override
    public void close() {
        try {
            driver.close();
        } finally {
            driver.quit();
        }
    }

    public static void main(String[] args) throws Exception {
        GoogleScraper scraper = GoogleScraper.phantomJs("/home/agrigorev/tmp/soft/phantomjs/bin/phantomjs");
        SearchResults first = scraper.scrape("kaggle avito");

        Thread.sleep(2031);

        SearchResults next = scraper.nextPage(first);

        Iterable<RankedPage> allPages = Iterables.concat(first.getPages(), next.getPages());
        allPages.forEach(System.out::println);

        scraper.close();
    }

}
