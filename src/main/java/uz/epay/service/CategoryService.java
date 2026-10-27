package uz.epay.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.epay.dto.CategoryDto;
import uz.epay.dto.CategoryRequest;
import uz.epay.entity.Category;
import uz.epay.exception.ConflictException;
import uz.epay.exception.NotFoundException;
import uz.epay.repository.CategoryRepository;
import uz.epay.repository.ProductRepository;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    private final ProductRepository products;

    public CategoryService(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        return categories.findAllByOrderBySortOrderAscNameAsc().stream().map(CategoryDto::from).toList();
    }

    @Transactional
    public CategoryDto create(CategoryRequest req) {
        if (categories.existsByNameIgnoreCase(req.name().trim())) {
            throw new ConflictException("Bu nomli kategoriya allaqachon bor");
        }
        return CategoryDto.from(categories.save(apply(new Category(), req)));
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest req) {
        Category c = get(id);
        if (categories.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) {
            throw new ConflictException("Bu nomli kategoriya allaqachon bor");
        }
        return CategoryDto.from(apply(c, req));
    }

    @Transactional
    public void delete(Long id) {
        Category c = get(id);
        if (products.existsByCategoryId(id)) {
            throw new ConflictException("Kategoriyada mahsulotlar bor, uni o'chirib bo'lmaydi");
        }
        categories.delete(c);
    }

    private Category get(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Kategoriya topilmadi: " + id));
    }

    private static Category apply(Category c, CategoryRequest req) {
        c.setName(req.name().trim());
        c.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        return c;
    }
}
