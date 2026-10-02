package com.caselock.service;

import com.caselock.config.FileStorageProperties;
import com.caselock.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Wraps evidence file persistence on disk (or, in a future iteration, an
 * object store). Every stored file gets a random, non-guessable name so
 * that the original filename never doubles as a path on disk.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageProperties fileStorageProperties;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(fileStorageProperties.path()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException ex) {
            throw new BadRequestException("Could not initialize evidence file storage: " + ex.getMessage());
        }
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was provided for upload.", "FILE_EMPTY");
        }
        long maxBytes = fileStorageProperties.maxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BadRequestException(
                    "The uploaded file exceeds the maximum allowed size of " + fileStorageProperties.maxSizeMb() + " MB.",
                    "FILE_TOO_LARGE");
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (extension.isBlank() || !fileStorageProperties.allowedExtensions().contains(extension.toLowerCase())) {
            throw new BadRequestException(
                    "Files of type ." + extension + " are not permitted for evidence upload.", "FILE_TYPE_NOT_ALLOWED");
        }
    }

    public String store(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
        try {
            Path destination = rootLocation.resolve(storedFileName).normalize();
            if (!destination.getParent().equals(rootLocation)) {
                throw new BadRequestException("Invalid file destination.");
            }
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return storedFileName;
        } catch (IOException ex) {
            log.error("Failed to store evidence file", ex);
            throw new BadRequestException("Something went wrong while storing the evidence file. Please try again.");
        }
    }

    public InputStream load(String storedFileName) {
        try {
            Path file = rootLocation.resolve(storedFileName).normalize();
            if (!file.startsWith(rootLocation) || !Files.exists(file)) {
                throw new BadRequestException("The requested evidence file could not be found on the server.");
            }
            return Files.newInputStream(file);
        } catch (IOException ex) {
            throw new BadRequestException("Something went wrong while reading the evidence file. Please try again.");
        }
    }

    public Path resolve(String storedFileName) {
        return rootLocation.resolve(storedFileName).normalize();
    }

    private String extractExtension(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int dotIndex = cleaned.lastIndexOf('.');
        return dotIndex >= 0 ? cleaned.substring(dotIndex + 1) : "";
    }
}
