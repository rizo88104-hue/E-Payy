package uz.epay.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.epay.dto.BrandDto;
import uz.epay.dto.BrandRequest;
import uz.epay.entity.Brand;
import uz.epay.exception.ConflictException;
import uz.epay.exception.NotFoundException;
import uz.epay.repository.BrandRepository;
import uz.epay.repository.ProductRepository;

import java.util.List;

@Service
public class BrandService {
    private final BrandRepository brands;
    private final ProductRepository products;

    public BrandService(BrandRepository brands, ProductRepository products) {
        this.brands = brands;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<BrandDto> list() {
        return brands.findAllByOrderByNameAsc().stream().map(BrandDto::from).toList();
    }

    @Transactional
    public BrandDto create(BrandRequest req) {
        if (brands.existsByNameIgnoreCase(req.name().trim())) {
            throw new ConflictException("Bu nomli brend allaqachon bor");
        }
        if (brands.existsBySlug(req.slug())) {
            throw new ConflictException("Bu slug band: " + req.slug());
        }
        return BrandDto.from(brands.save(apply(new Brand(), req)));
    }

    @Transactional
    public BrandDto update(Long id, BrandRequest req) {
        Brand b = get(id);
        if (brands.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) {
            throw new ConflictException("Bu nomli brend allaqachon bor");
        }
        if (brands.existsBySlugAndIdNot(req.slug(), id)) {
            throw new ConflictException("Bu slug band: " + req.slug());
        }
        return BrandDto.from(apply(b, req));
    }

    @Transactional
    public void delete(Long id) {
        Brand b = get(id);
        if (products.existsByBrandId(id)) {
            throw new ConflictException("Brendda mahsulotlar bor, uni o'chirib bo'lmaydi");
        }
        brands.delete(b);
    }

    private Brand get(Long id) {
        return brands.findById(id).orElseThrow(() -> new NotFoundException("Brend topilmadi: " + id));
    }

    private static Brand apply(Brand b, BrandRequest req) {
        b.setName(req.name().trim());
        b.setSlug(req.slug());
        b.setLogo(req.logo());
        b.setDescription(req.description());
        b.setCountry(req.country());
        b.setFounded(req.founded());
        b.setWebsite(req.website());
        return b;
    }
}
