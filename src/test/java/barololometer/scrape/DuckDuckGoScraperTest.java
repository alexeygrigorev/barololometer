package barololometer.scrape;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class DuckDuckGoScraperTest {

    @Test
    public void notEmpty() {
        DuckDuckGoScraper scraper = new DuckDuckGoScraper();
        SearchResults results = scraper.scrape("kaggle");

        assertEquals("kaggle", results.getQuery());
        assertEquals(SearchEngine.DUCK_DUCK_GO, results.getSearchEngine());

        List<RankedPage> pages = results.getPages();
        assertTrue(pages.size() > 20);

        for (RankedPage page : pages) {
            assertTrue(StringUtils.isNotBlank(page.getUrl()));
            assertTrue(StringUtils.isNotBlank(page.getTitle()));
            assertTrue(StringUtils.isNotBlank(page.getSnippet()));
        }

        SearchResults nextPage = scraper.nextPage(results);
        assertTrue(nextPage.isNothing());
    }
}
