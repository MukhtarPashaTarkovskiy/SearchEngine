package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.StatisticsService;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingServiceImpl indexingService;
    private final SitesList sitesList;

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        boolean ok = indexingService.startIndexing();
        if (ok) {
            return ResponseEntity.ok(Map.of("result", true));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", false, "error", "Индексация уже запущена"));
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        boolean ok = indexingService.stopIndexing();
        if (ok) {
            return ResponseEntity.ok(Map.of("result", true));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("result", false, "error", "Индексация не запущена"));
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam("url") String url) {
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("result", false, "error", "Параметр url обязателен"));
        }

        boolean inConfig = sitesList.getSites().stream()
                .anyMatch(s -> Objects.nonNull(s.getUrl()) && url.startsWith(s.getUrl()));

        if (!inConfig) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"
            ));
        }

        boolean ok = indexingService.indexPage(url);
        if (ok) {
            return ResponseEntity.ok(Map.of("result", true));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("result", false, "error", "Ошибка при индексации страницы"));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

}





