package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;


public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    @Transactional
    @Modifying
    @Query("delete from IndexEntity i where i.page = :page")
    void deleteAllByPage(PageEntity page);


    List<IndexEntity> findByPage(PageEntity page);


    Optional<IndexEntity> findByPageAndLemma(PageEntity page, LemmaEntity lemma);

    @Modifying
    @Transactional
    @Query("delete from IndexEntity i where i.page.site = :site")
    void deleteAllBySite(@Param("site") SiteEntity site);

    List<IndexEntity> findByLemma(LemmaEntity lemma);

}
