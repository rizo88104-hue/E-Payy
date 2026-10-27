package uz.epay.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.epay.dto.BrandDto;
import uz.epay.dto.CategoryDto;
import uz.epay.dto.PageResponse;
import uz.epay.dto.ProductCard;
import uz.epay.dto.ProductDetails;
import uz.epay.service.BrandService;
import uz.epay.service.CategoryService;
import uz.epay.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CategoryService categories;
    private final BrandService brands;
    private final ProductService products;

    public CatalogController(CategoryService categories, BrandService brands, ProductService products) {
        this.categories = categories;
        this.brands = brands;
        this.products = products;
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return categories.list();
    }

    @GetMapping("/brands")
    public List<BrandDto> brands() {
        return brands.list();
    }

    @GetMapping("/products")
    public PageResponse<ProductCard> products(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) Long brandId,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return products.catalog(q, categoryId, brandId, page, size);
    }

    @GetMapping("/products/{id}")
    public ProductDetails product(@PathVariable Long id) {
        return products.published(id);
    }
}
