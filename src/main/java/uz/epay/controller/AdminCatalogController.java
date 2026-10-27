package uz.epay.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uz.epay.dto.BrandDto;
import uz.epay.dto.BrandRequest;
import uz.epay.dto.CategoryDto;
import uz.epay.dto.CategoryRequest;
import uz.epay.service.BrandService;
import uz.epay.service.CategoryService;

/** Admin: kategoriyalar va brendlar. Ro'yxatlar ochiq API dagi bilan bir xil (GET /api/categories, /api/brands). */
@RestController
@RequestMapping("/api/admin")
public class AdminCatalogController {
    private final CategoryService categories;
    private final BrandService brands;

    public AdminCatalogController(CategoryService categories, BrandService brands) {
        this.categories = categories;
        this.brands = brands;
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody CategoryRequest req) {
        return categories.create(req);
    }

    @PutMapping("/categories/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return categories.update(id, req);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categories.delete(id);
    }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    public BrandDto createBrand(@Valid @RequestBody BrandRequest req) {
        return brands.create(req);
    }

    @PutMapping("/brands/{id}")
    public BrandDto updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest req) {
        return brands.update(id, req);
    }

    @DeleteMapping("/brands/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBrand(@PathVariable Long id) {
        brands.delete(id);
    }
}
