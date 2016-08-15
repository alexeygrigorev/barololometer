package barololometer.scrape;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SearchResults {

    public static final SearchResults NULL = new SearchResults();

    private String uuid = UUID.randomUUID().toString();
    private LocalDateTime time = LocalDateTime.now();
    private String query;
    private SearchEngine searchEngine;
    private Map<String, String> params = new HashMap<>();
    private List<RankedPage> pages = new ArrayList<>(15);

    public SearchResults() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public SearchEngine getSearchEngine() {
        return searchEngine;
    }

    public void setSearchEngine(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public void setParam(String name, Object value) {
        params.put(name, String.valueOf(value));
    }

    public String getParam(String name) {
        return params.get(name);
    }

    public int getIntParam(String name) {
        return Integer.parseInt(params.get(name));
    }

    public void setPages(List<RankedPage> pages) {
        this.pages = pages;
    }

    public List<RankedPage> getPages() {
        return pages;
    }

    public void addPage(RankedPage page) {
        pages.add(page);
    }

    public boolean isNothing() {
        return this == NULL;
    }

    public boolean isNotNothing() {
        return !isNothing();
    }

}
