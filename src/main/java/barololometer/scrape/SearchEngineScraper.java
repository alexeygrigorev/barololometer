package barololometer.scrape;

public interface SearchEngineScraper extends AutoCloseable {

    SearchResults scrape(String query);

    SearchResults nextPage(SearchResults previos);

}
