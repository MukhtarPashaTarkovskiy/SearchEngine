package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    Optional<SiteEntity> findByUrl(String url);
}
