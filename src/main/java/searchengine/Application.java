package searchengine;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import searchengine.model.PageEntity;
import searchengine.repositories.PageRepository;

import java.util.List;


@SpringBootApplication
@EnableAsync
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);


    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);

        String root = "https://www.lenta.ru/news".replaceAll("^(https?://[^/]+).*", "$1");

        logger.info(root);

    }


}
