package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : ProductLookupService.java
 * - Date      : 2026. 11. 06.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Resolves product aliases and decodes barcode or QR images.
 * </pre>
 */

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.ntaganira.heritier.ipos.backoffice.config.ProductExtractionProperties;
import com.ntaganira.heritier.ipos.backoffice.dto.BarcodeLookupResult;
import com.ntaganira.heritier.ipos.backoffice.dto.ProductDetail;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProductLookupService {

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png");

    private final ProductRepository productRepository;
    private final ProductExtractionProperties properties;

    public ProductLookupService(ProductRepository productRepository,
                                ProductExtractionProperties properties) {
        this.productRepository = productRepository;
        this.properties = properties;
    }

    public BarcodeLookupResult lookup(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (code.isEmpty()) {
            throw new ProductLookupException("Enter or scan a barcode, SKU or part number.", HttpStatus.BAD_REQUEST);
        }
        return findDetail(code)
                .map(product -> new BarcodeLookupResult(code, true, product, null))
                .orElseGet(() -> new BarcodeLookupResult(code, false, null,
                        "No product matches the scanned code."));
    }

    public List<ProductDetail> search(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        int pageSize = Math.max(1, Math.min(limit, 25));
        return productRepository.searchByKeyword(like, PageRequest.of(0, pageSize))
                .stream()
                .map(this::toDetail)
                .toList();
    }

    public BarcodeLookupResult scan(MultipartFile image) {
        String code = decode(image);
        return lookup(code);
    }

    private Optional<ProductDetail> findDetail(String code) {
        return productRepository.findByBarcode(code)
                .or(() -> productRepository.findFirstByPartNumberIgnoreCaseOrderByIdAsc(code))
                .or(() -> productRepository.findByCodeIgnoreCase(code))
                .map(this::toDetail);
    }

    private ProductDetail toDetail(Product product) {
        return new ProductDetail(
                product.getId(),
                product.getName(),
                product.getCode(),
                product.getPartNumber(),
                product.getBarcode(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getSellingPrice(),
                product.getBuyingPrice(),
                product.getUnit() == null ? null : product.getUnit().getName(),
                product.getLocation() == null ? null : product.getLocation().label(),
                product.getTaxCategory() == null ? null : product.getTaxCategory().getCode(),
                product.getStockQuantity(),
                product.getReorderLevel(),
                product.isActive(),
                product.getImagePath(),
                product.getAliases().stream().map(alias -> alias.getAlias()).toList());
    }

    private String decode(MultipartFile image) {
        validateImage(image);
        BufferedImage source;
        try (InputStream input = image.getInputStream()) {
            source = ImageIO.read(input);
        } catch (IOException ex) {
            throw new ProductLookupException("The barcode image could not be read.", ex, HttpStatus.BAD_REQUEST);
        }
        if (source == null) {
            throw new ProductLookupException("The barcode image is not a valid image file.", HttpStatus.BAD_REQUEST);
        }

        for (int angle = 0; angle < 360; angle += 90) {
            String value = decode(source, angle);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        throw new ProductLookupException(
                "No barcode or QR code was found. Try a clearer, closer photo.", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private String decode(BufferedImage source, int angle) {
        BufferedImage image = angle == 0 ? source : rotate(source, angle);
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(DecodeHintType.ALSO_INVERTED, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, new ArrayList<>(EnumSet.of(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODABAR,
                BarcodeFormat.EAN_8,
                BarcodeFormat.EAN_13,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.ITF,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.PDF_417,
                BarcodeFormat.AZTEC)));

        MultiFormatReader reader = new MultiFormatReader();
        LuminanceSource sourceImage = new BufferedImageLuminanceSource(image);
        try {
            Result result = reader.decode(new BinaryBitmap(new HybridBinarizer(sourceImage)), hints);
            return result.getText();
        } catch (ReaderException ex) {
            try {
                Result result = reader.decode(
                        new BinaryBitmap(new GlobalHistogramBinarizer(sourceImage)), hints);
                return result.getText();
            } catch (ReaderException ignored) {
                return null;
            }
        }
    }

    private BufferedImage rotate(BufferedImage source, int angle) {
        double radians = Math.toRadians(angle);
        int width = source.getWidth();
        int height = source.getHeight();
        int rotatedWidth = angle == 180 ? width : height;
        int rotatedHeight = angle == 180 ? height : width;
        BufferedImage target = new BufferedImage(rotatedWidth, rotatedHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.translate(rotatedWidth / 2.0, rotatedHeight / 2.0);
            graphics.rotate(radians);
            graphics.translate(-width / 2.0, -height / 2.0);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ProductLookupException("Choose a barcode or QR image to scan.", HttpStatus.BAD_REQUEST);
        }
        if (image.getSize() > properties.maxImageBytes()) {
            throw new ProductLookupException("The barcode image must be "
                    + (properties.maxImageBytes() / (1024 * 1024)) + " MB or smaller.", HttpStatus.BAD_REQUEST);
        }
        String contentType = image.getContentType();
        if (!StringUtils.hasText(contentType)
                || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ProductLookupException("Upload a JPG or PNG barcode image.", HttpStatus.BAD_REQUEST);
        }
    }
}
