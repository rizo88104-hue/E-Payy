package uz.epay.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uz.epay.dto.DeleteResult;
import uz.epay.dto.ImageOrderRequest;
import uz.epay.dto.PageResponse;
import uz.epay.dto.ProductCard;
import uz.epay.dto.ProductDetails;
import uz.epay.dto.ProductRequest;
import uz.epay.entity.ProductStatus;
import uz.epay.service.ProductService;

/** Admin: mahsulotlar (barcha statuslar) va ularning rasmlari. */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductService products;

    public AdminProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping
    public PageResponse<ProductCard> list(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) Long brandId,
                                          @RequestParam(required = false) ProductStatus status,
                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return products.search(q, categoryId, brandId, status, page, size);
    }

    @GetMapping("/{id}")
    public ProductDetails get(@PathVariable Long id) {
        return products.details(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetails create(@Valid @RequestBody ProductRequest req) {
        return products.create(req);
    }

    @PutMapping("/{id}")
    public ProductDetails update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return products.update(id, req);
    }

    /** Buyurtmalarda ishlatilgan bo'lsa o'chirilmaydi, ARCHIVED qilinadi — javobda qaysi biri bo'lgani. */
    @DeleteMapping("/{id}")
    public DeleteResult delete(@PathVariable Long id) {
        return products.delete(id);
    }

    @PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetails addImage(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return products.addImage(id, file);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ProductDetails deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        return products.deleteImage(id, imageId);
    }

    @PutMapping("/{id}/images/order")
    public ProductDetails reorderImages(@PathVariable Long id, @Valid @RequestBody ImageOrderRequest req) {
        return products.reorderImages(id, req.ids());
    }
}
