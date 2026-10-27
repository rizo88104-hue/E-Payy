package uz.epay.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.epay.config.EpayProperties;
import uz.epay.dto.DeleteResult;
import uz.epay.dto.PageResponse;
import uz.epay.dto.ProductCard;
import uz.epay.dto.ProductDetails;
import uz.epay.dto.ProductRequest;
import uz.epay.entity.Brand;
import uz.epay.entity.Category;
import uz.epay.entity.Product;
import uz.epay.entity.ProductImage;
import uz.epay.entity.ProductStatus;
import uz.epay.entity.StaffRole;
import uz.epay.exception.BadRequestException;
import uz.epay.exception.ConflictException;
import uz.epay.exception.ForbiddenException;
import uz.epay.exception.NotFoundException;
import uz.epay.repository.BrandRepository;
import uz.epay.repository.CategoryRepository;
import uz.epay.repository.ProductRepository;
import uz.epay.repository.ProductSpecs;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {
    public static final int MAX_IMAGES = 10;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductUsageChecker usage;
    private final ImageStorage storage;
    private final boolean warehouseCanEditPrice;

    public ProductService(ProductRepository products, CategoryRepository categories, BrandRepository brands,
                          ProductUsageChecker usage, ImageStorage storage, EpayProperties props) {
        this.products = products;
        this.categories = categories;
        this.brands = brands;
        this.usage = usage;
        this.storage = storage;
        this.warehouseCanEditPrice = props.security().warehouseCanEditPrice();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductCard> catalog(String q, Long categoryId, Long brandId, int page, int size) {
        return search(q, categoryId, brandId, ProductStatus.PUBLISHED, page, size);
    }

    @Transactional(readOnly = true)
    public ProductDetails published(Long id) {
        Product p = products.findWithDetailsById(id)
                .filter(x -> x.getStatus() == ProductStatus.PUBLISHED)
                .orElseThrow(() -> notFound(id));
        return ProductDetails.from(p);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductCard> search(String q, Long categoryId, Long brandId, ProductStatus status,
                                            int page, int size) {
        return PageResponse.of(products.findAll(ProductSpecs.filter(q, categoryId, brandId, status),
                PageRequest.of(page, size, NEWEST_FIRST)), ProductCard::from);
    }

    @Transactional(readOnly = true)
    public ProductDetails details(Long id) {
        return ProductDetails.from(load(id));
    }

    @Transactional
    public ProductDetails create(ProductRequest req) {
        if (products.existsBySku(req.sku())) {
            throw new ConflictException("Bu SKU bilan mahsulot allaqachon bor: " + req.sku());
        }
        return ProductDetails.from(products.saveAndFlush(apply(new Product(), req)));
    }

    @Transactional
    public ProductDetails update(Long id, ProductRequest req) {
        if (req.version() == null) {
            throw new BadRequestException("version majburiy: mahsulotni qayta oching");
        }
        Product p = load(id);
        if (!Objects.equals(p.getVersion(), req.version())) {
            throw new ObjectOptimisticLockingFailureException(Product.class, id);
        }
        if (products.existsBySkuAndIdNot(req.sku(), id)) {
            throw new ConflictException("Bu SKU bilan mahsulot allaqachon bor: " + req.sku());
        }
        checkWarehouseLimits(p, req);
        return ProductDetails.from(products.saveAndFlush(apply(p, req)));
    }

    @Transactional
    public DeleteResult delete(Long id) {
        Product p = load(id);
        if (usage.isUsedInOrders(id)) {
            p.setStatus(ProductStatus.ARCHIVED);
            return new DeleteResult(DeleteResult.Outcome.ARCHIVED);
        }
        List<String> urls = p.getImages().stream().map(ProductImage::getUrl).toList();
        products.delete(p);
        AfterTransaction.onCommit(() -> urls.forEach(storage::delete));
        return new DeleteResult(DeleteResult.Outcome.DELETED);
    }

    @Transactional
    public ProductDetails addImage(Long id, MultipartFile file) {
        Product p = load(id);
        if (p.getImages().size() >= MAX_IMAGES) {
            throw new BadRequestException("Bitta mahsulotga ko'pi bilan " + MAX_IMAGES + " ta rasm qo'shish mumkin");
        }
        String url = storage.save(file);
        AfterTransaction.onRollback(() -> storage.delete(url));
        p.addImage(url);
        return ProductDetails.from(products.saveAndFlush(p));
    }

    @Transactional
    public ProductDetails deleteImage(Long id, Long imageId) {
        Product p = load(id);
        ProductImage img = p.getImages().stream().filter(i -> i.getId().equals(imageId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Rasm topilmadi: " + imageId));
        p.getImages().remove(img);
        p.reindexImages();
        AfterTransaction.onCommit(() -> storage.delete(img.getUrl()));
        return ProductDetails.from(products.saveAndFlush(p));
    }

    @Transactional
    public ProductDetails reorderImages(Long id, List<Long> ids) {
        Product p = load(id);
        Set<Long> current = p.getImages().stream().map(ProductImage::getId).collect(Collectors.toSet());
        if (ids.size() != current.size() || !current.equals(new HashSet<>(ids))) {
            throw new BadRequestException("Rasmlar ro'yxati mos emas: mahsulotning har bir rasmi bir martadan bo'lsin");
        }
        p.getImages().sort(Comparator.comparingInt(i -> ids.indexOf(i.getId())));
        p.reindexImages();
        return ProductDetails.from(products.saveAndFlush(p));
    }

    private void checkWarehouseLimits(Product p, ProductRequest req) {
        if (!CurrentUser.hasRole(StaffRole.WAREHOUSE) || CurrentUser.hasRole(StaffRole.ADMIN)) {
            return;
        }
        if (!warehouseCanEditPrice && p.getPrice().compareTo(req.price()) != 0) {
            throw new ForbiddenException("Narxni o'zgartirish huquqingiz yo'q");
        }
        if (req.status() == ProductStatus.ARCHIVED && p.getStatus() != ProductStatus.ARCHIVED) {
            throw new ForbiddenException("Mahsulotni arxivlash huquqingiz yo'q");
        }
    }

    private Product load(Long id) {
        return products.findWithDetailsById(id).orElseThrow(() -> notFound(id));
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("Mahsulot topilmadi: " + id);
    }

    private Product apply(Product p, ProductRequest req) {
        Category category = categories.findById(req.categoryId())
                .orElseThrow(() -> new BadRequestException("Kategoriya topilmadi: " + req.categoryId()));
        Brand brand = req.brandId() == null ? null : brands.findById(req.brandId())
                .orElseThrow(() -> new BadRequestException("Brend topilmadi: " + req.brandId()));
        p.setSku(req.sku());
        p.setName(req.name().trim());
        p.setDescription(req.description());
        p.setCategory(category);
        p.setBrand(brand);
        p.setPrice(req.price());
        p.setSpecs(req.specs());
        if (req.status() != null) {
            p.setStatus(req.status());
        }
        p.setMxikCode(req.mxikCode());
        p.setPackageCode(req.packageCode());
        p.setVatPercent(req.vatPercent());
        return p;
    }
}
