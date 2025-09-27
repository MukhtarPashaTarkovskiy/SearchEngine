package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    Optional<PageEntity> findBySiteAndPath(SiteEntity site, String path);

    boolean existsBySiteAndPath(SiteEntity site, String path);

    List<PageEntity> findBySite(SiteEntity site);

    void deleteAllBySite(SiteEntity site);
}
