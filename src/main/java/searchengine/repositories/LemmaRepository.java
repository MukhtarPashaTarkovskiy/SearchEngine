package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import java.util.Optional;
import java.util.List;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    Optional<LemmaEntity> findFirstBySiteAndLemma(SiteEntity site, String lemma);

    List<LemmaEntity> findBySite(SiteEntity site);
    void deleteAllBySite(SiteEntity site);
}
