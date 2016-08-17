package barololometer;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import barololometer.scrape.BingScraper;
import barololometer.scrape.DuckDuckGoScraper;
import barololometer.scrape.GoogleScraper;
import barololometer.scrape.SearchEngine;
import barololometer.scrape.SearchEngineScraper;
import barololometer.service.SearchCombiner;

@Configuration
public class Container {

    @Bean
    public SearchCombiner combiner() {
        Map<SearchEngine, SearchEngineScraper> scrapers = new EnumMap<>(SearchEngine.class);
        scrapers.put(SearchEngine.DUCK_DUCK_GO, new DuckDuckGoScraper());
        scrapers.put(SearchEngine.BING, new BingScraper());
        scrapers.put(SearchEngine.GOOGLE, GoogleScraper.phantomJs("/home/agrigorev/tmp/soft/phantomjs/bin/phantomjs"));

        return new SearchCombiner(scrapers);
    }

}
