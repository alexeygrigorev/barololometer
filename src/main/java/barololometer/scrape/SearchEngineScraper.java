package barololometer.scrape;

public interface SearchEngineScraper {

    SearchResults scrape(String query);

    SearchResults nextPage(SearchResults previos);

}
