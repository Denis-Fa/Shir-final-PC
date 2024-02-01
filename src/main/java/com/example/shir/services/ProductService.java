package com.example.shir.services;

import com.example.shir.models.Image;
import com.example.shir.models.Product;
import com.example.shir.models.ProductDTO;
import com.example.shir.models.User;
import com.example.shir.repositories.ProductRepository;
import com.example.shir.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ToolRecommendationService toolRecommendationService;

    public List<Product> listProducts(String title) {
        if (title != null) return productRepository.findByTitle(title);
        return productRepository.findAll();
    }

    public void saveProduct(Principal principal, Product product, MultipartFile file1, MultipartFile file2, MultipartFile file3) throws IOException {
        product.setUser(getUserByPrincipal(principal));
        Image image1;
        Image image2;
        Image image3;
        if (file1.getSize() != 0) {
            image1 = toImageEntity(file1);
            image1.setPreviewImage(true);
            product.addImageToProduct(image1);
        }
        if (file2.getSize() != 0) {
            image2 = toImageEntity(file2);
            product.addImageToProduct(image2);
        }
        if (file3.getSize() != 0) {
            image3 = toImageEntity(file3);
            product.addImageToProduct(image3);
        }
        log.info("Saving new Product. Title: {}; Author email: {}", product.getTitle(), product.getUser().getEmail());
        Product productFromDb = productRepository.save(product);
        productFromDb.setPreviewImageId(productFromDb.getImages().get(0).getId());
        productRepository.save(product);
    }

    public User getUserByPrincipal(Principal principal) {
        if (principal == null) return new User();
        return userRepository.findByEmail(principal.getName());
    }

    private Image toImageEntity(MultipartFile file) throws IOException {
        Image image = new Image();
        image.setName(file.getName());
        image.setOriginalFileName(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setSize(file.getSize());
        image.setBytes(file.getBytes());
        return image;
    }

    public void deleteProduct(User user, Long id) {
        Product product = productRepository.findById(id)
                .orElse(null);
        if (product != null) {
            if (product.getUser().getId().equals(user.getId())) {
                productRepository.delete(product);
                log.info("Product with id = {} was deleted", id);
            } else {
                log.error("User: {} haven't this product with id = {}", user.getEmail(), id);
            }
        } else {
            log.error("Product with id = {} is not found", id);
        }    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }


    /**
     * Метод для получения инструментов из БД или ИИ. <br>
     * Получаем из базы данных количество хранящихся инструментов.  <br>
     * Если поиск по инструментам пустой (null, empty), возвращаем пустой массив.  <br>
     * Если title null или пустой, возвращаем выборку от ИИ рекомендаций по объявлениям.  <br>
     * Если title НЕ пустой - выбираем ищем объявления в БД.  <br>
     * @param title поиск по названию объявлений.
     * @param user пользователь ищущий объявления.
     */
    public List<Product> getProducts(String title, User user) {
        List<Product> products = List.of();
        long countProduct = this.countProduct();
        log.debug("getProducts count product: {}", countProduct);
        if (countProduct <= 0 || user == null || user.getId() == null) {
            return products;
        }
        if (title == null || title.isEmpty()) {
            var productDTOList = toolRecommendationService.recommendedToolsForUser(user.getId(), countProduct);
            products = this.getProductsByProductDTO(productDTOList);
        } else {
            products = this.listProducts(title);
        }
        return products;
    }

    public long countProduct() {
        return productRepository.count();
    }

    private List<Product> getProductsByProductDTO(List<ProductDTO> productDTO) {
        List<Long> listId = productDTO.stream()
                .map(ProductDTO::getId)
                .collect(Collectors.toList());
        log.debug("getProductsByProductDTO product Id: {}", listId);
        return productRepository.findAllById(listId);
    }
}
