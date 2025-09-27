package searchengine.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "site")
@Getter
@Setter
@NoArgsConstructor
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PageEntity> pages = new ArrayList<>();

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LemmaEntity> lemmas = new ArrayList<>();

    private String url;
    private String name;
    private LocalDateTime statusTime;
    @Enumerated(EnumType.STRING)
    private SiteStatus status;
    private String lastError;
}

