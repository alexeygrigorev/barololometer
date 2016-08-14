package barololometer.scrape;

public class RankedPage {

    private SearchEngine searchEngine;
    private String query;
    private int page;
    private int position;
    private String url;
    private String title;
    private String snippet;

    public RankedPage() {
    }

    public void setSearchEngine(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    public SearchEngine getSearchEngine() {
        return searchEngine;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    @Override
    public String toString() {
        return "RankedPage [query=" + query + ", page=" + page + ", position=" + position + ", url=" + url + ", title="
                + title + ", snippet=" + snippet + "]";
    }
}
