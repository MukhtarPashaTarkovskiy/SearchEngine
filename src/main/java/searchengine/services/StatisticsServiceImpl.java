package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteEntity> siteEntities = siteRepository.findAll();
        if (!siteEntities.isEmpty()) {
            for (SiteEntity se : siteEntities) {
                DetailedStatisticsItem item = new DetailedStatisticsItem();
                item.setName(se.getName());
                item.setUrl(se.getUrl());
                item.setStatus(se.getStatus() == null ? SiteStatus.FAILED.name() : se.getStatus().name());
                item.setStatusTime(se.getStatusTime() == null ? System.currentTimeMillis() : se.getStatusTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                item.setError(se.getLastError());
                int pages = pageRepository.findBySite(se).size();
                int lemmas = lemmaRepository.findBySite(se).size();
                item.setPages(pages);
                item.setLemmas(lemmas);
                total.setPages(total.getPages() + pages);
                total.setLemmas(total.getLemmas() + lemmas);
                detailed.add(item);
            }
        } else {

            for (Site site : sites.getSites()) {
                DetailedStatisticsItem item = new DetailedStatisticsItem();
                item.setName(site.getName());
                item.setUrl(site.getUrl());
                item.setStatus(SiteStatus.FAILED.name());
                item.setStatusTime(System.currentTimeMillis());
                item.setPages(0);
                item.setLemmas(0);
                detailed.add(item);
            }
        }

        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(data);
        return response;
    }
}








