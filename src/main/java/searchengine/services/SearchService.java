package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.search.SearchResultDto;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.LemmaFinder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    @Value("${search.too-frequent-percent:0.8}")
    private double TOO_FREQUENT_PERCENT;

    @Transactional(readOnly = true)
    public List<SearchResultDto> search(String query, String siteUrl, int offset, int limit) throws Exception {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Задан пустой поисковый запрос");
        }


        Map<String, Integer> queryLemmas = LemmaFinder.getInstance().collectLemmas(query);
        if (queryLemmas.isEmpty()) return Collections.emptyList();


        List<SiteEntity> sites = siteUrl == null
                ? siteRepository.findAll()
                : siteRepository.findByUrl(siteUrl).map(List::of)
                .orElseThrow(() -> new IllegalArgumentException("Указанный сайт не найден: " + siteUrl));

        List<SearchResultDto> allResults = new ArrayList<>();

        for (SiteEntity site : sites) {
            if (site.getPages().isEmpty() || site.getLemmas().isEmpty()) {
                throw new IllegalArgumentException("Для сайта " + site.getUrl() + " ещё не построен индекс");
            }

            int pagesCount = site.getPages().size();
            int threshold = (int) (pagesCount * TOO_FREQUENT_PERCENT);

            List<LemmaEntity> lemmas = lemmaRepository.findBySite(site).stream()
                    .filter(l -> queryLemmas.containsKey(l.getLemma()))
                    .filter(l -> l.getFrequency() < threshold)
                    .sorted(Comparator.comparingInt(LemmaEntity::getFrequency))
                    .collect(Collectors.toList());

            if (lemmas.isEmpty()) continue;


            Set<PageEntity> pages = new HashSet<>(
                    indexRepository.findByLemma(lemmas.get(0)).stream()
                            .map(IndexEntity::getPage).toList()
            );

            for (int i = 1; i < lemmas.size(); i++) {
                Set<PageEntity> nextPages = indexRepository.findByLemma(lemmas.get(i)).stream()
                        .map(IndexEntity::getPage).collect(Collectors.toSet());
                pages.retainAll(nextPages);
                if (pages.isEmpty()) break;
            }

            if (pages.isEmpty()) continue;


            Map<PageEntity, Double> absRel = new HashMap<>();
            for (PageEntity page : pages) {
                double rankSum = 0;
                for (LemmaEntity lemma : lemmas) {
                    rankSum += indexRepository.findByPageAndLemma(page, lemma)
                            .map(IndexEntity::getRank).orElse(0f);
                }
                absRel.put(page, rankSum);
            }

            double maxAbs = absRel.values().stream().max(Double::compare).orElse(1.0);


            for (PageEntity page : absRel.keySet()) {
                double relevance = absRel.get(page) / maxAbs;
                Document doc = Jsoup.parse(page.getContent());
                String title = doc.title();
                String snippet = makeSnippet(doc.text(), queryLemmas.keySet());

                allResults.add(new SearchResultDto(
                        site.getUrl(),
                        site.getName(),
                        page.getPath(),
                        title,
                        snippet,
                        relevance
                ));
            }
        }

        return allResults.stream()
                .sorted(Comparator.comparingDouble(SearchResultDto::getRelevance).reversed())
                .skip(offset)
                .limit(limit)
                .toList();
    }

    private String makeSnippet(String text, Set<String> lemmas) {
        String lowerText = text.toLowerCase();
        int firstIdx = -1;
        String foundLemma = "";

        for (String lemma : lemmas) {
            int idx = lowerText.indexOf(lemma.toLowerCase());
            if (idx >= 0 && (firstIdx == -1 || idx < firstIdx)) {
                firstIdx = idx;
                foundLemma = lemma;
            }
        }

        if (firstIdx == -1) {
            return text.substring(0, Math.min(200, text.length()));
        }

        int start = Math.max(0, firstIdx - 100);
        int end = Math.min(text.length(), firstIdx + 100);
        String fragment = text.substring(start, end);

        for (String lemma : lemmas) {
            fragment = fragment.replaceAll("(?i)" + lemma, "<b>" + lemma + "</b>");
        }
        return fragment;
    }
}

