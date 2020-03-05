package com.ere.resttester;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@SpringBootApplication
public class RestTesterApplication implements CommandLineRunner {

    private RestTemplate restTemplate() {
        return new RestTemplate();
    }

    private volatile HttpHeaders httpHeaders = new HttpHeaders();
    private volatile String url;
    private volatile int threadPool;

    private ExecutorService executor;

    private volatile int okAtomic;
    private volatile int badAtomic;

    public static void main(String[] args) {
        SpringApplication.run(RestTesterApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args != null) {
            url = args[0];
            threadPool = Integer.parseInt(args[1]);
            httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            executor = Executors.newFixedThreadPool(threadPool);
            restTest();
        }
    }

    private void restTest() {
        while (!executor.isShutdown()) {
            try {
                for (int i = 0; i < threadPool; i++) {
                    executor.execute(() -> {
                        try {
                            var response = restTemplate().exchange(url, HttpMethod.GET,
                                    new HttpEntity<>(null, httpHeaders), String.class);
                            if (response.getStatusCode().equals(HttpStatus.OK)) {
                                okIncrement();
                            }
                        } catch (HttpClientErrorException ex) {
                            badIncrement();
                        } catch (ResourceAccessException exception) {
                            executor.shutdownNow();
                        }
                        log.info("OK: " + okAtomic + " . Bad: " + badAtomic);
                    });
                }
            } catch (Exception e) {
                break;
            }
        }
    }

    private synchronized void okIncrement() {
        okAtomic++;
    }

    private synchronized void badIncrement() {
        badAtomic++;
    }

}
