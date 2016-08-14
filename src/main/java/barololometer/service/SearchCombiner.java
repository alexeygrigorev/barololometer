package barololometer.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import barololometer.scrape.RankedPage;
import barololometer.scrape.SearchEngine;
import barololometer.scrape.SearchEngineScraper;
import barololometer.scrape.SearchResults;

public class SearchCombiner {

    private static Ordering<RankedPage> BY_POSITION = Ordering.natural().onResultOf(RankedPage::getPosition)
            .compound(Ordering.arbitrary());

    private final ScheduledExecutorService delayedExecutor = Executors.newScheduledThreadPool(4);
    private final Map<SearchEngine, SearchEngineScraper> scrapers;
    private final Cache<String, List<Future<SearchResults>>> delayedCrawl = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(5, TimeUnit.MINUTES).build();

    public SearchCombiner(Map<SearchEngine, SearchEngineScraper> scrapers) {
        this.scrapers = Maps.immutableEnumMap(scrapers);
    }

    public void search(String query) {
        String requestUuid = UUID.randomUUID().toString();
        List<SearchResults> collect = scrapers.values().parallelStream().map(s -> s.scrape(query))
                .collect(Collectors.toList());

        delaySecondPageCrawl(requestUuid, collect);

        // list<rankedpage> allresults = scrapers.parallelstream()
        // .map(s -> {
        // SearchResults first = s.scrape(query);
        // int waitTime = RandomUtils.nextInt(100, 700);
        // Thread.sleep(waitTime);
        // SearchResults nextPage = s.nextPage(first);
        // })
        // .flatMap(r -> r.getPages().stream())
        // .sorted(BY_POSITION)
        // .filter(new UrlNotSeenPredicate())
        // .collect(Collectors.toList());
        // allResults.forEach(System.out::println);
    }

    private void delaySecondPageCrawl(String requestUuid, List<SearchResults> collect) {
        ImmutableList.Builder<Future<SearchResults>> futures = ImmutableList.builder();
        for (SearchResults res : collect) {
            Future<SearchResults> future = delayedExecutor.schedule(() -> {
                SearchEngine engine = res.getSearchEngine();
                SearchEngineScraper scraper = scrapers.get(engine);
                return scraper.nextPage(res);
            }, 3, TimeUnit.SECONDS);
            futures.add(future);
        }

        delayedCrawl.put(requestUuid, futures.build());
    }

    private static class UrlNotSeenPredicate implements Predicate<RankedPage> {
        private final Set<String> urls = new HashSet<>();

        @Override
        public boolean test(RankedPage t) {
            if (urls.contains(t.getUrl())) {
                return false;
            }
            urls.add(t.getUrl());
            return true;
        }
    }

}
