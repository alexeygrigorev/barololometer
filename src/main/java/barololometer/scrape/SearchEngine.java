package barololometer.scrape;

import org.apache.commons.lang3.RandomUtils;

public enum SearchEngine {

    GOOGLE(300, 1500),
    BING(300, 500),
    DUCK_DUCK_GO(0, 0);

    private final int minGracePeriod;
    private final int maxGracePeriod;

    private SearchEngine(int minGracePeriod, int maxGracePeriod) {
        this.minGracePeriod = minGracePeriod;
        this.maxGracePeriod = maxGracePeriod;
    }

    public int randomWaitTime() {
        return RandomUtils.nextInt(minGracePeriod, maxGracePeriod);
    }
}
