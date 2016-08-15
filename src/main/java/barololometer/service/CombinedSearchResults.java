package barololometer.service;

import java.util.List;

import barololometer.scrape.RankedPage;

public class CombinedSearchResults {

    private String query;
    private List<RankedPage> pages;
    private String sessionUuid;

    public CombinedSearchResults() {
    }

    public void setPages(List<RankedPage> pages) {
        this.pages = pages;
    }

    public List<RankedPage> getPages() {
        return pages;
    }

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
