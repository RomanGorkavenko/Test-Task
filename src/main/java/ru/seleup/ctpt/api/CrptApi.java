package ru.seleup.ctpt.api;


import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

@Log
public class CrptApi {

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final ScheduledExecutorService scheduler;

    /**
     * Константа url запроса POST
     */
    private static final String URI_REQUEST = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    /**
     * Инициализация {@link ObjectMapper}
     */
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .registerModule(new JavaTimeModule());

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * метод – Создание документа для ввода в оборот товара, произведенного в РФ.
     * Документ и подпись должны передаваться в метод в виде Java объекта и строки соответственно.
     * @param document - Документ
     * @param signature - подпись
     */
    public void createNewDocument(Document document, String signature) throws InterruptedException {

        Semaphore semaphore = new Semaphore(requestLimit);
        semaphore.acquire();

        scheduler.scheduleAtFixedRate(semaphore::release, 1, 1, timeUnit);

        try {
            String json = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URI_REQUEST))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = HttpClient
                    .newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warning("response = " + response.statusCode());
                log.info(response.body());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Завершение работы.
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * Вызов метода {@link CrptApi#createNewDocument(Document, String)}
     */
    public static void main(String[] args) throws InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        Document document;

        try {
            File file = new File("src/main/resources/document.json");
            document = objectMapper.readValue(file, Document.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        crptApi.createNewDocument(document, "Roman");
        crptApi.shutdown();

    }


    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Document {

        private Description description;

        @JsonSetter("doc_id")
        private String docId;

        @JsonSetter("doc_status")
        private String docStatus;

        @JsonSetter("doc_type")
        private String docType;
        private boolean importRequest;

        @JsonSetter("owner_inn")
        private String ownerInn;

        @JsonSetter("participant_inn")
        private String participantInn;

        @JsonSetter("producer_inn")
        private String producerInn;

        @JsonSetter("production_date")
        private LocalDate productionDate;

        @JsonSetter("production_type")
        private String productionType;
        private List<Product> products;

        @JsonSetter("reg_date")
        private LocalDate regDate;

        @JsonSetter("reg_number")
        private String regNumber;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    static class Product {

        @JsonSetter("certificate_document")
        private String certificateDocument;

        @JsonSetter("certificate_document_date")
        private LocalDate certificateDocumentDate;

        @JsonSetter("certificate_document_number")
        private String certificateDocumentNumber;

        @JsonSetter("owner_inn")
        private String ownerInn;

        @JsonSetter("producer_inn")
        private String producerInn;

        @JsonSetter("production_date")
        private LocalDate productionDate;

        @JsonSetter("tnved_code")
        private String tnvedCode;

        @JsonSetter("uit_code")
        private String uitCode;

        @JsonSetter("uitu_code")
        private String uituCode;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    static class Description {
        private String participantInn;
    }
}