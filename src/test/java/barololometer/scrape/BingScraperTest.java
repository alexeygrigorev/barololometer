package barololometer.scrape;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class BingScraperTest {

    @Test
    public void notEmpty() {
        BingScraper scraper = new BingScraper();
        SearchResults results = scraper.scrape("kaggle");

        assertEquals("kaggle", results.getQuery());
        assertEquals(SearchEngine.BING, results.getSearchEngine());

        List<RankedPage> pages = results.getPages();
        assertContentNotEmpty(pages);

        RankedPage firstPageLastItem = pages.get(pages.size() - 1);

        SearchResults nextPage = scraper.nextPage(results);
        pages = nextPage.getPages();
        assertContentNotEmpty(pages);

        RankedPage secondPageFirstItem = pages.get(0);
        assertEquals(firstPageLastItem.getPosition() + 1, secondPageFirstItem.getPosition());
    }

    private static void assertContentNotEmpty(List<RankedPage> pages) {
        assertTrue(pages.size() > 5);
        for (RankedPage page : pages) {
            assertTrue(StringUtils.isNotBlank(page.getUrl()));
            assertTrue(StringUtils.isNotBlank(page.getTitle()));
            assertTrue(StringUtils.isNotBlank(page.getSnippet()));
        }
    }

}
