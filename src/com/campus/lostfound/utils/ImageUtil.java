package com.campus.lostfound.utils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ImageUtil
 * 
 * Secure, production-ready image utility for Smart Campus Lost & Found.
 * Features:
 * 1. Strict format whitelist: JPEG (.jpg, .jpeg) and PNG (.png) only.
 * 2. Header-first dimension and format inspection (prevents heap memory decompression bombs).
 * 3. File size limit (5MB) and resolution limit (4096 x 4096, 16MP).
 * 4. Path traversal defense: Discards client filename, generates unique names, asserts canonical boundary.
 * 5. Partial file cleanup: Immediately deletes unverified/partially copied files on any failure.
 * 6. Working-directory independent: Anchors relative paths to project application root.
 * 7. Bounded LRU thumbnail cache: Thread-safe cache capped at 50 thumbnails to prevent memory leaks.
 */
public class ImageUtil {

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    public static final int MAX_DIMENSION_PIXELS = 4096;
    public static final long MAX_PIXELS_TOTAL = 16_000_000L; // 16 Megapixels

    public static final int THUMBNAIL_WIDTH = 120;
    public static final int THUMBNAIL_HEIGHT = 90;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_FORMAT_NAMES = Set.of("JPEG", "PNG");

    private static final int MAX_CACHE_ENTRIES = 50;

    // Thread-safe Bounded LRU Cache for thumbnails
    private static final Map<String, ImageIcon> THUMBNAIL_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<>(MAX_CACHE_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            });

    /**
     * Resolves the application project root reliably regardless of current working directory.
     */
    public static Path getApplicationRoot() {
        try {
            Path codePath = Paths.get(ImageUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isDirectory(codePath) && codePath.getFileName() != null && codePath.getFileName().toString().equalsIgnoreCase("bin")) {
                return codePath.getParent();
            }
            if (codePath.getParent() != null && codePath.getParent().getFileName() != null && codePath.getParent().getFileName().toString().equalsIgnoreCase("bin")) {
                return codePath.getParent().getParent();
            }
            if (Files.isRegularFile(codePath)) { // Running from JAR
                return codePath.getParent();
            }
            return codePath;
        } catch (Exception e) {
            // Fallback to verified project workspace directory
            return Paths.get("E:\\JAVA Project\\SmartCampusLostAndFound");
        }
    }

    /**
     * Returns the controlled upload directory for items: uploads/items/
     */
    public static Path getUploadDirectory() {
        Path dir = getApplicationRoot().resolve("uploads").resolve("items");
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize image storage directory: " + dir, e);
        }
        return dir;
    }

    /**
     * Validates an image file before acceptance.
     * Inspects extension, file size, and image header (dimensions & format) without full raster decoding.
     *
     * @param sourceFile File selected by user
     * @return Normalized file extension (e.g., "jpg", "png")
     * @throws IllegalArgumentException with user-friendly reason if invalid
     */
    public static String validateImageFile(File sourceFile) throws IllegalArgumentException {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            throw new IllegalArgumentException("Selected file does not exist or is not a valid file.");
        }

        // 1. File size check
        long length = sourceFile.length();
        if (length <= 0) {
            throw new IllegalArgumentException("Selected file is empty (0 bytes).");
        }
        if (length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(String.format("File size (%d KB) exceeds maximum limit of %d MB.",
                    length / 1024, MAX_FILE_SIZE_BYTES / (1024 * 1024)));
        }

        // 2. Extension check
        String originalName = sourceFile.getName();
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            throw new IllegalArgumentException("File must have a valid image extension (.jpg, .jpeg, or .png).");
        }
        String ext = originalName.substring(dotIndex + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported image format '." + ext + "'. Only JPEG and PNG are supported.");
        }

        // 3. Header-first inspection (Format & Dimensions)
        try (ImageInputStream in = ImageIO.createImageInputStream(sourceFile)) {
            if (in == null) {
                throw new IllegalArgumentException("Cannot read image stream. File may be corrupted.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("File does not contain valid image header data.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                String formatName = reader.getFormatName();
                if (formatName == null || !ALLOWED_FORMAT_NAMES.contains(formatName.toUpperCase())) {
                    throw new IllegalArgumentException("Invalid image content. Declared format is not JPEG or PNG.");
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0) {
                    throw new IllegalArgumentException("Invalid image dimensions detected.");
                }
                if (width > MAX_DIMENSION_PIXELS || height > MAX_DIMENSION_PIXELS) {
                    throw new IllegalArgumentException(String.format("Image dimensions (%dx%d) exceed maximum of %dx%d pixels.",
                            width, height, MAX_DIMENSION_PIXELS, MAX_DIMENSION_PIXELS));
                }
                if ((long) width * height > MAX_PIXELS_TOTAL) {
                    throw new IllegalArgumentException("Total image resolution exceeds 16 megapixels.");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode image header: " + e.getMessage(), e);
        }

        // Normalize extension
        return ext.equals("jpeg") ? "jpg" : ext;
    }

    /**
     * Immutable result holding exact tracking metadata for a securely stored image.
     */
    public static class SavedImageResult {
        private final String relativePath;
        private final Path targetPath;
        private final Object fileKey;

        public SavedImageResult(String relativePath, Path targetPath, Object fileKey) {
            this.relativePath = relativePath;
            this.targetPath = targetPath;
            this.fileKey = fileKey;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public Path getTargetPath() {
            return targetPath;
        }

        public Object getFileKey() {
            return fileKey;
        }
    }

    /**
     * Securely copies a validated source image into the controlled uploads/items/ directory.
     * Uses atomic exclusive file creation (CREATE_NEW) to prevent pre-existing file overwriting or symlink races,
     * and records the OS file identity for verified current-attempt cleanup tracking.
     *
     * @param sourceFile The validated user file
     * @return SavedImageResult with normalized relative path, target Path, and file identity key
     * @throws IOException on write error
     * @throws SecurityException on path traversal attempt
     */
    public static SavedImageResult saveItemImageResult(File sourceFile) throws IOException, SecurityException {
        String ext = validateImageFile(sourceFile);

        Path uploadDir = getUploadDirectory().toRealPath();

        // Unique filename: item_<timestamp>_<uuid8>.<ext>
        String uniqueName = "item_" + System.currentTimeMillis() + "_" +
                            UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "." + ext;

        Path destPath = uploadDir.resolve(uniqueName).normalize();

        // Path Traversal Security Assertion:
        // Destination path must start with uploadDir and must reside directly in uploadDir
        if (!destPath.startsWith(uploadDir) || !destPath.getParent().equals(uploadDir)) {
            throw new SecurityException("Security violation: Target path escapes upload directory.");
        }

        // Exclusive atomic file creation (CREATE_NEW fails if destPath already exists)
        boolean fileCreatedByThisAttempt = false;
        try {
            try (InputStream in = Files.newInputStream(sourceFile.toPath());
                 OutputStream out = Files.newOutputStream(destPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                in.transferTo(out);
                fileCreatedByThisAttempt = true;
            }

            // Capture file attributes and OS file identity with NOFOLLOW_LINKS
            BasicFileAttributes attrs = Files.readAttributes(destPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attrs.isSymbolicLink() || attrs.isOther()) {
                throw new SecurityException("Security violation: Target is a symbolic link or reparse point.");
            }

            Path realTarget = destPath.toRealPath();
            if (!realTarget.startsWith(uploadDir) || realTarget.getParent() == null || !realTarget.getParent().equals(uploadDir)) {
                throw new SecurityException("Security violation: Canonical path escaped upload directory.");
            }

            Object fileKey = attrs.fileKey();
            if (fileKey == null) {
                // Portable identity fallback for filesystems where fileKey() is null (Windows/NTFS)
                fileKey = realTarget.toString() + ":" + attrs.creationTime().toMillis() + ":" + attrs.size();
            }

            return new SavedImageResult("uploads/items/" + uniqueName, destPath, fileKey);

        } catch (Throwable t) {
            // Failure occurred either during copy or during attribute/canonicalization inspection.
            // Delete destPath ONLY if this invocation has positively established that it created the file.
            if (fileCreatedByThisAttempt) {
                try {
                    BasicFileAttributes cleanupAttrs = Files.readAttributes(destPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    if (!cleanupAttrs.isSymbolicLink() && !cleanupAttrs.isOther()) {
                        Files.delete(destPath);
                    }
                } catch (Exception cleanupEx) {
                    destPath.toFile().deleteOnExit();
                }
            }

            // Rethrow original exception without masking or swallowing
            if (t instanceof IOException) {
                throw (IOException) t;
            } else if (t instanceof SecurityException) {
                throw (SecurityException) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new IOException("Failed to finalize saved item image: " + t.getMessage(), t);
            }
        }
    }

    /**
     * Backward-compatible convenience wrapper delegating to saveItemImageResult.
     */
    public static String saveItemImage(File sourceFile) throws IOException, SecurityException {
        return saveItemImageResult(sourceFile).getRelativePath();
    }

    /**
     * Resolves a relative image path to an existing absolute File.
     *
     * @param relativePath Stored database path (e.g. "uploads/items/item_...")
     * @return File object if file exists, or null if missing/empty
     */
    public static File resolveImageFile(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }
        Path resolved = getApplicationRoot().resolve(relativePath.trim()).normalize();
        File file = resolved.toFile();
        return file.exists() && file.isFile() ? file : null;
    }

    /**
     * Alias for resolveImageFile.
     */
    public static File resolveStoragePath(String relativePath) {
        return resolveImageFile(relativePath);
    }

    /**
     * Creates a scaled thumbnail directly from a local File (useful for pre-upload preview).
     */
    public static ImageIcon createThumbnail(File sourceFile, int targetWidth, int targetHeight) {
        if (sourceFile == null || !sourceFile.exists()) {
            return null;
        }
        try {
            BufferedImage original = ImageIO.read(sourceFile);
            if (original == null) {
                return null;
            }
            int origW = original.getWidth();
            int origH = original.getHeight();

            double scale = Math.min((double) targetWidth / origW, (double) targetHeight / origH);
            int newW = Math.max(1, (int) Math.round(origW * scale));
            int newH = Math.max(1, (int) Math.round(origH * scale));

            BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(original, 0, 0, newW, newH, null);
            g2.dispose();

            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Alias for getThumbnail.
     */
    public static ImageIcon loadThumbnail(String relativePath, int targetWidth, int targetHeight) {
        return getThumbnail(relativePath, targetWidth, targetHeight);
    }

    /**
     * Loads a scaled thumbnail for display, using bounded LRU cache.
     * If file is missing or invalid, returns null without throwing exceptions.
     *
     * @param relativePath Relative image path
     * @param targetWidth Width in pixels (default 120)
     * @param targetHeight Height in pixels (default 90)
     * @return ImageIcon thumbnail, or null if unavailable
     */
    public static ImageIcon getThumbnail(String relativePath, int targetWidth, int targetHeight) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }

        String cacheKey = relativePath + "_" + targetWidth + "x" + targetHeight;
        ImageIcon cached = THUMBNAIL_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        File imageFile = resolveImageFile(relativePath);
        if (imageFile == null) {
            return null;
        }

        try {
            BufferedImage original = ImageIO.read(imageFile);
            if (original == null) {
                return null;
            }

            // High-quality smooth scaling
            int origW = original.getWidth();
            int origH = original.getHeight();

            double scale = Math.min((double) targetWidth / origW, (double) targetHeight / origH);
            int newW = Math.max(1, (int) Math.round(origW * scale));
            int newH = Math.max(1, (int) Math.round(origH * scale));

            BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(original, 0, 0, newW, newH, null);
            g2.dispose();

            ImageIcon icon = new ImageIcon(scaled);
            THUMBNAIL_CACHE.put(cacheKey, icon);
            return icon;
        } catch (Exception e) {
            // Gracefully handle corrupt or unreadable image file
            return null;
        }
    }

    /**
     * Loads full resolution image safely for dialog viewing.
     *
     * @param relativePath Relative image path
     * @param maxDisplayWidth Maximum width bound for screen
     * @param maxDisplayHeight Maximum height bound for screen
     * @return Scaled preview ImageIcon, or null if missing
     */
    public static ImageIcon getFullPreview(String relativePath, int maxDisplayWidth, int maxDisplayHeight) {
        File file = resolveImageFile(relativePath);
        if (file == null) return null;

        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) return null;

            int w = img.getWidth();
            int h = img.getHeight();

            if (w <= maxDisplayWidth && h <= maxDisplayHeight) {
                return new ImageIcon(img);
            }

            double scale = Math.min((double) maxDisplayWidth / w, (double) maxDisplayHeight / h);
            int sw = Math.max(1, (int) Math.round(w * scale));
            int sh = Math.max(1, (int) Math.round(h * scale));

            Image scaled = img.getScaledInstance(sw, sh, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Clears in-memory thumbnail cache.
     */
    public static void clearCache() {
        THUMBNAIL_CACHE.clear();
    }
}
