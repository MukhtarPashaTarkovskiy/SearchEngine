package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.LemmaFinder;
import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final SitesList sitesList;
    private final LemmaService lemmaService;
    private static final int MAX_DEPTH = 3;
    private static final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);

    private volatile boolean indexing = false;
    private ExecutorService pool;

    public boolean isIndexing() {
        return indexing;
    }


    public synchronized boolean startIndexing() {
        if (indexing) return false;
        indexing = true;

        pool = Executors.newFixedThreadPool(Math.max(1, sitesList.getSites().size()));
        for (Site s : sitesList.getSites()) {
            pool.submit(() -> crawlSite(s));
        }
        return true;
    }

    public synchronized boolean stopIndexing() {
        if (!indexing) return false;
        indexing = false;
        if (pool != null) {
            pool.shutdownNow();
        }
        siteRepository.findAll().forEach(se -> {
            if (se.getStatus() == SiteStatus.INDEXING) {
                se.setStatus(SiteStatus.FAILED);
                se.setLastError("Индексация остановлена пользователем");
                se.setStatusTime(LocalDateTime.now());
                siteRepository.save(se);
            }
        });
        return true;
    }


    private void crawlSite(Site confSite) {
        SiteEntity site = prepareSite(confSite);
        String root = extractRootUrl(confSite.getUrl());
        Set<String> visited = ConcurrentHashMap.newKeySet();

        ForkJoinPool fjp = new ForkJoinPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        try {
            fjp.invoke(new CrawlTask(root + "/", site, root, visited,0)); // ✅ стартуем с "/"
            site.setStatus(indexing ? SiteStatus.INDEXED : site.getStatus());
        } catch (Exception e) {
            site.setStatus(SiteStatus.FAILED);
            site.setLastError(e.getMessage());
        } finally {
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            fjp.shutdown();
        }
    }

    private String normalizeUrl(String url) {
        try {
            URI uri = URI.create(url);
            String norm = uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() == -1 ? "" : ":" + uri.getPort())
                    + uri.getPath();
            return norm.replaceAll("/+$", ""); // убираем хвостовые /
        } catch (Exception e) {
            return url;
        }
    }


    private SiteEntity prepareSite(Site confSite) {
        siteRepository.findByUrl(confSite.getUrl()).ifPresent(old -> {
            logger.warn(" Удаляем сайт и все связанные сущности: {}", old.getUrl());
            siteRepository.delete(old);
        });

        SiteEntity site = new SiteEntity();
        site.setUrl(confSite.getUrl());
        site.setName(confSite.getName());
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError(null);
        return siteRepository.save(site);
    }

    private String extractRootUrl(String url) {
        try {
            URI u = URI.create(url);
            String scheme = u.getScheme();
            String host = u.getHost();
            int port = u.getPort();
            if (scheme == null || host == null) {
                return url.replaceFirst("^(https?://[^/]+).*", "$1");
            }
            return scheme + "://" + host + (port == -1 ? "" : ":" + port);
        } catch (Exception e) {
            return url.replaceFirst("^(https?://[^/]+).*", "$1");
        }
    }


    private Connection.Response fetchResponse(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(Optional.ofNullable(sitesList.getUserAgent()).orElse("Mozilla/5.0 (compatible; MySearchBot/1.0)"))
                .referrer(Optional.ofNullable(sitesList.getReferer()).orElse("https://www.google.com"))
                .timeout(20_000)
                .ignoreHttpErrors(true)
                .execute();
    }

    private String toPath(String root, String fullUrl) {
        if (!fullUrl.startsWith(root)) {
            return null; // внешние ссылки не сохраняем
        }
        String path = fullUrl.substring(root.length());
        if (path.isEmpty()) return "/";
        return path.replaceAll("/+$", ""); // убираем хвостовые слэши
    }


    private PageEntity savePage(SiteEntity site, String root, String link, int code, String htmlContent) {
        String path = toPath(root, link);
        if (path.isEmpty()) path = "/";

        PageEntity page = pageRepository.findBySiteAndPath(site, path).orElse(new PageEntity());
        page.setSite(site);
        page.setPath(path);
        page.setCode(code);
        page.setContent(htmlContent == null ? "" : htmlContent);
        return pageRepository.save(page);
    }

    @Transactional
    public boolean indexPage(String url) {
        Optional<Site> confOpt = sitesList.getSites().stream()
                .filter(s -> url.startsWith(s.getUrl()))
                .findFirst();

        if (confOpt.isEmpty()) return false;
        Site confSite = confOpt.get();
        String root = extractRootUrl(confSite.getUrl());

        SiteEntity site = siteRepository.findByUrl(confSite.getUrl()).orElseGet(() -> {
            SiteEntity se = new SiteEntity();
            se.setUrl(confSite.getUrl());
            se.setName(confSite.getName());
            se.setStatus(SiteStatus.INDEXED);
            se.setStatusTime(LocalDateTime.now());
            return siteRepository.save(se);
        });

        try {
            Connection.Response res = fetchResponse(url);
            int status = res.statusCode();
            String body = res.body() == null ? "" : res.body();

            String path = toPath(root, url);
            if (path.isEmpty()) path = "/";

            pageRepository.findBySiteAndPath(site, path).ifPresent(existing -> {
                indexRepository.deleteAllByPage(existing);
                pageRepository.delete(existing);
            });

            if (status >= 400) {

                savePage(site, root, url, status, body);
                return true;
            }

            PageEntity page = savePage(site, root, url, status, body);


            LemmaFinder lf = LemmaFinder.getInstance();
            String text = extractText(body);
            Map<String, Integer> lemmas = lf.collectLemmas(text);
            lemmaService.applyLemmas(site, page, lemmas);

            return true;

        } catch (Exception e) {
            site.setStatus(SiteStatus.FAILED);
            site.setLastError(e.getMessage());
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            return false;
        }
    }

    private String extractText(String html) {
        try {
            return Jsoup.parse(html).text();
        } catch (Exception e) {
            return html;
        }
    }

    private class CrawlTask extends RecursiveAction {
        private final String url;
        private final SiteEntity site;
        private final String root;
        private final Set<String> visited;
        private final int depth;
        private static final int MAX_DEPTH = 3;


        public CrawlTask(String url, SiteEntity site, String root, Set<String> visited,int depth) {
            this.url = url;
            this.site = site;
            this.root = root;
            this.visited = visited;
            this.depth = depth;

        }

        @Override
        protected void compute() {
            String normUrl = normalizeUrl(url);
            if (!indexing) return;
            if (depth >= MAX_DEPTH) return;
            if (!visited.add(url)) return;


            try {
                Connection.Response res = fetchResponse(url);
                int status = res.statusCode();
                String body = res.body() == null ? "" : res.body();

                if (status >= 400) {
                    savePage(site, root, url, status, body);
                    return;
                }

                PageEntity page = savePage(site, root, url, status, body);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);

                try {
                    String text = extractText(body);
                    Map<String, Integer> lemmas = LemmaFinder.getInstance().collectLemmas(text);
                    lemmaService.applyLemmas(site, page, lemmas);
                    logger.info(" Индексация страницы завершена: {} ({} лемм)", page.getPath(), lemmas.size());
                } catch (Exception le) {
                    logger.error("Лемматизация не выполнена для {}: {}", url, le.toString());
                }

                if (status == 200 && body != null && !body.isBlank()) {
                    Document doc = res.parse();
                    List<CrawlTask> subtasks = new ArrayList<>();

                    for (Element a : doc.select("a[href]")) {
                        if (!indexing || Thread.currentThread().isInterrupted()) return;

                        String rawHref = a.attr("abs:href");
                        if (rawHref == null || rawHref.isEmpty()) continue;

                        // сначала убираем якорь (#), потом нормализуем
                        rawHref = rawHref.replaceAll("#.*$", "");
                        String href = normalizeUrl(rawHref);
                        if (href == null || href.isEmpty()) continue;

                        try {
                            URI uri = URI.create(href);
                            URI rootUri = URI.create(root);
                            if (!Objects.equals(uri.getHost(), rootUri.getHost())) continue;
                        } catch (Exception ignored) {
                            continue;
                        }

                        // depth + 1
                        subtasks.add(new CrawlTask(href, site, root, visited, depth + 1));
                    }

                    Integer delay = sitesList.getDelayMs();
                    if (delay != null && delay > 0) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    if (!subtasks.isEmpty()) invokeAll(subtasks);
                }


            } catch (IOException e) {
                logger.debug("Ошибка доступа {}: {}", url, e.toString());
            } catch (Exception e) {
                logger.error("Неожиданная ошибка {}: {}", url, e.toString());
            }
        }
    }

}












