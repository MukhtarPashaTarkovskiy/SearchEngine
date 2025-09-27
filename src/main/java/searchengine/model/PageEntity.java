package searchengine.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "page", uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "path"}))
@Getter
@Setter
@NoArgsConstructor
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndexEntity> indices = new ArrayList<>();

    private String path;
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;
}
