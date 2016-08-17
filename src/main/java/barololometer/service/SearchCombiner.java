package barololometer.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import barololometer.scrape.BingScraper;
import barololometer.scrape.DuckDuckGoScraper;
import barololometer.scrape.GoogleScraper;
import barololometer.scrape.RankedPage;
import barololometer.scrape.SearchEngine;
import barololometer.scrape.SearchEngineScraper;
import barololometer.scrape.SearchResults;

public class SearchCombiner implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchCombiner.class);

    private static final int FIRST_PAGE_LIMIT = 12;
    private static final Ordering<RankedPage> BY_POSITION = Ordering.natural().onResultOf(RankedPage::getPosition);

    private final ListeningScheduledExecutorService delayedExecutor = MoreExecutors
            .listeningDecorator(Executors.newScheduledThreadPool(4));
    private final ExecutorService databaseSaverExecutor = Executors.newSingleThreadExecutor();
    private final Map<SearchEngine, SearchEngineScraper> scrapers;
    private final Cache<String, SearchSession> sessions = CacheBuilder.newBuilder()
            .concurrencyLevel(4).expireAfterWrite(5, TimeUnit.MINUTES).build();

    public SearchCombiner(Map<SearchEngine, SearchEngineScraper> scrapers) {
        this.scrapers = Maps.immutableEnumMap(scrapers);
    }

    public CombinedSearchResults firstPageSearch(String query) {
        String sessionUuid = UUID.randomUUID().toString();
        List<SearchResults> firstPages = scrapers.values().parallelStream()
                .map(s -> s.scrape(query))
                .peek(r -> saveAsync(r))
                .collect(Collectors.toList());

        List<RankedPage> merged = combineSearchResults(firstPages);
        List<RankedPage> firstPage = merged.subList(0, FIRST_PAGE_LIMIT);
        List<RankedPage> remaining = ImmutableList.copyOf(merged.subList(FIRST_PAGE_LIMIT, merged.size()));

        List<Future<SearchResults>> futureResults = delaySecondPageCrawl(sessionUuid, firstPages);
        Set<String> shownUrls = extractUrls(firstPage);

        SearchSession session = new SearchSession(query, futureResults, remaining, shownUrls);
        sessions.put(sessionUuid, session);

        CombinedSearchResults results = new CombinedSearchResults();
        results.setQuery(query);
        results.setSessionUuid(sessionUuid);
        results.setPages(firstPage);

        return results;
    }

    public CombinedSearchResults moreResults(String sessionUuid) throws SessionExpiredException {
        SearchSession previousSession = sessions.getIfPresent(sessionUuid);
        if (previousSession == null) {
            throw new SessionExpiredException();
        }

        String newSessionUuid = UUID.randomUUID().toString();
        List<SearchResults> newResults = newPageResults(previousSession.futureResults);
        List<RankedPage> newPages = extractIndividualPages(newResults);

        List<Future<SearchResults>> nextPageResults = delaySecondPageCrawl(sessionUuid, newResults);
        Iterable<RankedPage> allPages = Iterables.concat(previousSession.unshownPages, newPages);
        List<RankedPage> combined = combine(previousSession.shownUrls, allPages);

        Set<String> allShownUrls = new HashSet<>(extractUrls(combined));
        allShownUrls.addAll(previousSession.shownUrls);

        List<RankedPage> remaining = Collections.emptyList();
        SearchSession nextSession = new SearchSession(previousSession.query, nextPageResults, remaining, allShownUrls);
        sessions.put(newSessionUuid, nextSession);

        CombinedSearchResults results = new CombinedSearchResults();
        results.setQuery(previousSession.query);
        results.setSessionUuid(newSessionUuid);
        results.setPages(combined);

        return results;
    }

    private static Set<String> extractUrls(List<RankedPage> firstPage) {
        return firstPage.stream().map(RankedPage::getUrl).collect(Collectors.toSet());
    }

    private static List<SearchResults> newPageResults(List<Future<SearchResults>> futureResults) {
        return futureResults.stream()
                .map(f -> getUnchecked(f))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(SearchResults::isNotNothing)
                .collect(Collectors.toList());
    }

    private static Optional<SearchResults> getUnchecked(Future<SearchResults> f) {
        try {
            return Optional.of(f.get());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static List<RankedPage> combineSearchResults(List<SearchResults> firstPages) {
        List<RankedPage> pages = extractIndividualPages(firstPages);
        return combine(Collections.emptySet(), pages);
    }

    private static List<RankedPage> extractIndividualPages(List<SearchResults> searchResults) {
        return searchResults.stream()
                .flatMap(r -> r.getPages().stream())
                .collect(Collectors.toList());
    }

    private static List<RankedPage> combine(Set<String> shownUrls, Iterable<RankedPage> pages) {
        ArrayListMultimap<SearchEngine, RankedPage> byEngine = groupBySearchEngine(pages);
        List<Iterator<RankedPage>> queue = filteringIterators(shownUrls, byEngine);
        return mergeIterators(queue);
    }

    private static ArrayListMultimap<SearchEngine, RankedPage> groupBySearchEngine(Iterable<RankedPage> pages) {
        ArrayListMultimap<SearchEngine, RankedPage> groupByEngine = ArrayListMultimap.create();
        pages.forEach(page -> groupByEngine.put(page.getSearchEngine(), page));
        return groupByEngine;
    }

    private static List<Iterator<RankedPage>> filteringIterators(Set<String> previousUrls,
            ArrayListMultimap<SearchEngine, RankedPage> groups) {
        UrlNotSeenPredicate uniqueUrls = new UrlNotSeenPredicate(previousUrls);

        List<Iterator<RankedPage>> queue = new ArrayList<>();
        for (SearchEngine se : groups.keySet()) {
            List<RankedPage> group = groups.get(se);
            List<RankedPage> sorted = BY_POSITION.immutableSortedCopy(group);
            Iterator<RankedPage> it = Iterators.filter(sorted.iterator(), uniqueUrls);
            queue.add(it);
        }

        return queue;
    }

    private static List<RankedPage> mergeIterators(List<Iterator<RankedPage>> iterators) {
        List<RankedPage> results = new ArrayList<>(25);

        List<Iterator<RankedPage>> thisRound = new ArrayList<>(iterators);
        List<Iterator<RankedPage>> newRound = new ArrayList<>(iterators.size());

        while (!thisRound.isEmpty()) {
            for (Iterator<RankedPage> it : thisRound) {
                if (it.hasNext()) {
                    results.add(it.next());
                    newRound.add(it);
                }
            }

            thisRound.clear();
            Collections.shuffle(newRound);
            thisRound.addAll(newRound);
            newRound.clear();
        }

        return results;
    }

    private List<Future<SearchResults>> delaySecondPageCrawl(String requestUuid, List<SearchResults> collect) {
        ImmutableList.Builder<Future<SearchResults>> futures = ImmutableList.builder();

        for (SearchResults res : collect) {
            if (res.isNothing()) {
                continue;
            }

            SearchEngine engine = res.getSearchEngine();

            ListenableScheduledFuture<SearchResults> future = delayedExecutor.schedule(() -> {
                SearchEngineScraper scraper = scrapers.get(engine);
                return scraper.nextPage(res);
            } , engine.randomWaitTime(), TimeUnit.MILLISECONDS);

            Futures.addCallback(future, saveToDbCallback(), databaseSaverExecutor);
            futures.add(future);
        }

        return futures.build();
    }

    private FutureCallback<SearchResults> saveToDbCallback() {
        return new FutureCallback<SearchResults>() {
            @Override
            public void onSuccess(SearchResults result) {
                save(result);
            }

            @Override
            public void onFailure(Throwable t) {
            }
        };
    }

    private void saveAsync(SearchResults result) {
        databaseSaverExecutor.execute(() -> save(result));
    }

    private void save(SearchResults result) {
        if (result.isNothing()) {
            return;
        }
        if (result.getPages().isEmpty()) {
            return;
        }

        System.out.println("SAVE TO DB IS CALLED!");
        LOGGER.info("saving results for {} from {}", result.getQuery(), result.getSearchEngine());
    }

    @Override
    public void close() {
        for (SearchEngineScraper scraper : scrapers.values()) {
            try {
                scraper.close();
            } catch (Exception e) {
            }
        }

        databaseSaverExecutor.shutdown();
        delayedExecutor.shutdown();
    }

    private static class UrlNotSeenPredicate implements Predicate<RankedPage> {
        private final Set<String> seen;

        public UrlNotSeenPredicate(Set<String> shownUrls) {
            this.seen = new HashSet<>(shownUrls);
        }

        @Override
        public boolean apply(RankedPage page) {
            String url = page.getUrl();
            if (seen.contains(url)) {
                return false;
            }
            seen.add(url);
            return true;
        }
    }

    private static class SearchSession {
        private final String query;
        private final List<Future<SearchResults>> futureResults;
        private final List<RankedPage> unshownPages;
        private final Set<String> shownUrls;

        public SearchSession(String query, List<Future<SearchResults>> futureResults, List<RankedPage> unshownPages, Set<String> shownUrls) {
            this.query = query;
            this.futureResults = futureResults;
            this.unshownPages = unshownPages;
            this.shownUrls = shownUrls;
        }
    }

    public static void main(String[] args) throws SessionExpiredException {
        Map<SearchEngine, SearchEngineScraper> scrapers = new EnumMap<>(SearchEngine.class);
        scrapers.put(SearchEngine.DUCK_DUCK_GO, new DuckDuckGoScraper());
        scrapers.put(SearchEngine.BING, new BingScraper());
        scrapers.put(SearchEngine.GOOGLE, GoogleScraper.phantomJs("/home/agrigorev/tmp/soft/phantomjs/bin/phantomjs"));

        SearchCombiner combiner = new SearchCombiner(scrapers);

        CombinedSearchResults searchResults = combiner.firstPageSearch("if you know what I mean");
        List<RankedPage> pages = searchResults.getPages();
        pages.forEach(System.out::println);

        CombinedSearchResults page2 = combiner.moreResults(searchResults.getSessionUuid());
        page2.getPages().forEach(System.out::println);

        CombinedSearchResults page3 = combiner.moreResults(page2.getSessionUuid());
        page3.getPages().forEach(System.out::println);

        combiner.close();
    }
}
