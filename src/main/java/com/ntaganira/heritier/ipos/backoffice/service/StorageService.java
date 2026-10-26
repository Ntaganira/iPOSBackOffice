package com.ntaganira.heritier.ipos.backoffice.service;

/**
 * <pre>
 * - Project   : iPOS - Point of Sale, Stock and Back-Office
 * - Package   : com.ntaganira.heritier.ipos.backoffice.service
 * - File      : StorageService.java
 * - Date      : 2026. 10. 26.
 * - User      : Heritier NTAGANIRA
 * - Desc      : Stores and retrieves uploaded files.
 * </pre>
 */

import com.ntaganira.heritier.ipos.backoffice.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class StorageService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final long MAX_ATTACHMENT_BYTES = 5L * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Path root;

    public StorageService(AppProperties properties) {
        this.root = Path.of(properties.storage().uploads()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root.resolve("avatars"));
        Files.createDirectories(root.resolve("logo"));
        Files.createDirectories(root.resolve("products"));
        Files.createDirectories(root.resolve("expenses"));
    }

    public String saveAvatar(MultipartFile file) {
        return saveImage(file, "avatars", 256, MAX_AVATAR_BYTES);
    }

    public String saveLogo(MultipartFile file) {
        return saveImage(file, "logo", 512, MAX_ATTACHMENT_BYTES);
    }

    public String saveProductImage(MultipartFile file) {
        return saveImage(file, "products", 800, MAX_ATTACHMENT_BYTES);
    }

    public String saveExpenseAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new IllegalArgumentException("The attachment must be 5 MB or smaller.");
        }
        String type = contentType(file);
        if (!Set.of("image/jpeg", "image/png", "image/webp", "application/pdf").contains(type)) {
            throw new IllegalArgumentException("Attachments must be a JPG, PNG, WEBP image or a PDF.");
        }
        byte[] bytes = read(file);
        verifyMagic(bytes, type);
        String extension = switch (type) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String name = UUID.randomUUID() + extension;
        write("expenses", name, bytes);
        return "expenses/" + name;
    }

    public String saveImage(MultipartFile file, String folder, int maxDimension, long maxBytes) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("The image must be " + (maxBytes / (1024 * 1024)) + " MB or smaller.");
        }
        String type = contentType(file);
        if (!IMAGE_TYPES.contains(type)) {
            throw new IllegalArgumentException("Only JPG, PNG or WEBP images are accepted.");
        }
        byte[] bytes = read(file);
        verifyMagic(bytes, type);
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) {
                throw new IllegalArgumentException("The image could not be read. Please choose a valid image file.");
            }
            BufferedImage thumbnail = squareThumbnail(source, maxDimension);
            String name = UUID.randomUUID() + ".png";
            Path target = root.resolve(folder).resolve(name);
            Files.createDirectories(target.getParent());
            ImageIO.write(thumbnail, "png", target.toFile());
            return folder + "/" + name;
        } catch (IOException ex) {
            throw new IllegalArgumentException("The image could not be processed.", ex);
        }
    }

    public Resource load(String relativePath) {
        try {
            Path path = resolve(relativePath);
            if (!Files.exists(path)) {
                return null;
            }
            return new UrlResource(path.toUri());
        } catch (Exception ex) {
            return null;
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException ignored) {
            // A stale file must not block a profile or product update.
        }
    }

    public String contentType(String relativePath) {
        if (relativePath == null) {
            return "application/octet-stream";
        }
        String lower = relativePath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    private Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid file path.");
        }
        return path;
    }

    private void write(String folder, String name, byte[] bytes) {
        try {
            Path target = root.resolve(folder).resolve(name);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException ex) {
            throw new IllegalStateException("The file could not be stored.", ex);
        }
    }

    private byte[] read(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("The uploaded file could not be read.", ex);
        }
    }

    private String contentType(MultipartFile file) {
        String type = file.getContentType();
        if (type != null) {
            return type.toLowerCase(Locale.ROOT);
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "image/jpeg";
    }

    private void verifyMagic(byte[] bytes, String type) {
        if (bytes.length < 12) {
            throw new IllegalArgumentException("The uploaded file is not valid.");
        }
        boolean valid = switch (type) {
            case "image/png" -> (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
            case "image/jpeg" -> (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
            case "image/webp" -> bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            case "application/pdf" -> bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("The file content does not match its type.");
        }
    }

    private BufferedImage squareThumbnail(BufferedImage source, int maxDimension) {
        int size = Math.min(Math.min(source.getWidth(), source.getHeight()), maxDimension);
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        BufferedImage cropped = source.getSubimage(x, y, size, size);
        BufferedImage target = new BufferedImage(maxDimension, maxDimension, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(cropped, 0, 0, maxDimension, maxDimension, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    static void copy(InputStream in, Path target) throws IOException {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
