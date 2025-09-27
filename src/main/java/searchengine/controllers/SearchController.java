package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.search.SearchResultDto;
import searchengine.services.SearchService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            List<SearchResultDto> results = searchService.search(query, site, offset, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("result", true);
            response.put("count", results.size());
            response.put("data", results);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "result", false,
                    "error", "Ошибка сервера: " + e.getMessage()
            ));
        }
    }
}


