package jd.controlling.linkcrawler;

public interface LinkCrawlerHandler {

    void handleFinalLink(CrawledLink link);

    void handleFilteredLink(CrawledLink link);

}
