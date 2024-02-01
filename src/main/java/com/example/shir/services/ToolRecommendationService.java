package com.example.shir.services;

import com.example.shir.models.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ToolRecommendationService {
    // максимум сколько увидит инструментов пользователей
    private static final int LIMIT_PRODUCT = 10;
    private final String baseUrl;
    private final RestTemplate restTemplate;

    @Autowired
    public ToolRecommendationService(@Value("${api.recommend.url}") String baseUrl, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
    }

    /**
     * Метод получения рекомендаций выборки инструментов от ИИ. <br>
     * Если ответ от ИИ пустой, возвращаем пустой массив.  <br>
     * Если countProduct, меньше чем 200 - (такой диапазон ID у ИИ), преобразуем Id максимум до countProduct.  <br>
     * Если больше, то возвращаем ответ как есть.  <br>
     * @param countProduct количество инструментов в БД
     */
    public List<ProductDTO> recommendedToolsForUser(long userId, long countProduct) {
        var responseEntity = restTemplate.getForEntity(baseUrl, ProductDTO[].class, userId);
        ProductDTO[] body = responseEntity.getBody();
        log.debug("recommendedToolsForUser body: {}", (Object) body);

        if (body == null || body.length == 0) {
            return List.of();
        }
        // нужно нормализовать id, тк ИИ отдает диапазон id от 1 до 200
        if (countProduct < 200) {
            return normalizedId(countProduct, body);
        }
        return Arrays.asList(body);
    }

    /**
     * Нормализует идентификаторы полученные от ИИ, в зависимости от countProduct или LIMIT_PRODUCT.
     * @param countProduct Количество объявлений или максимальное значение идентификатора для нормализации.
     * @param body         Массив объектов Tool, содержащих идентификаторы.
     * @return Список Tool с нормализованными и отсортированными идентификаторами.
     */
    private static List<ProductDTO> normalizedId(long countProduct, ProductDTO[] body) {
        final long minProductCount = Math.min(countProduct, LIMIT_PRODUCT);
        List<Long> normalizedIds;
        // Если countProduct <= 10, то просто нормализуем как раньше
        if (countProduct <= 10) {
            normalizedIds = Arrays.stream(body)
                    .map(productDTO -> (productDTO.getId() % countProduct) + 1).distinct()
                    .collect(Collectors.toList());
        } else {
            normalizedIds = Arrays.stream(body)
                    .sorted(Comparator.comparingLong(ProductDTO::getId))
                    .map(productDTO -> (Objects.hash(productDTO.getId()) % countProduct) + 1).distinct()
                    .limit(minProductCount)
                    .collect(Collectors.toList());
        }
        log.debug("recommendedToolsForUser body: <{}>", normalizedIds);
        return normalizedIds.stream()
                .map(ProductDTO::new)
                .sorted(Comparator.comparingLong(ProductDTO::getId))
                .collect(Collectors.toList());
    }
}