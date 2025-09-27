package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;

import java.util.Map;


@Service
@RequiredArgsConstructor
public class LemmaService {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private static final Logger logger = LoggerFactory.getLogger(LemmaService.class);


    @Transactional
    public void applyLemmas(SiteEntity site, PageEntity page, Map<String, Integer> lemmas) {
        logger.info("Применяем леммы для страницы {}", page.getPath());

        lemmas.forEach((lemmaStr, count) -> {
            LemmaEntity lemma = lemmaRepository.findFirstBySiteAndLemma(site, lemmaStr)
                    .orElseGet(() -> {
                        LemmaEntity le = new LemmaEntity();
                        le.setSite(site);
                        le.setLemma(lemmaStr);
                        le.setFrequency(0);
                        return lemmaRepository.save(le);
                    });


            if (indexRepository.findByPageAndLemma(page, lemma).isEmpty()) {
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemma = lemmaRepository.save(lemma);
            }

            IndexEntity idx = new IndexEntity();
            idx.setLemma(lemma);
            idx.setPage(page);
            idx.setRank(count);
            logger.info(" Сохраняем индекс: lemma={}, page={}, rank={}",
                    lemma.getLemma(), page.getPath(), count);
            indexRepository.save(idx);
        });
    }

}

