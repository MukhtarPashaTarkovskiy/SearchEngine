package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "lemma", uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "lemma"}))
@Getter @Setter @NoArgsConstructor
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndexEntity> indices = new ArrayList<>();

    private String lemma;
    private int frequency;
}

