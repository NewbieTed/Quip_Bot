package com.quip.backend.asset.service;

import com.quip.backend.asset.utils.AssetUtils;
import com.quip.backend.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class AssetService {

    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);

    private final AssetUtils assetUtils;

    /**
     * Retrieves an asset based on file type, folder, and filename.
     */
    public ResponseEntity<?> getAsset(String fileType, String folder, String filename) {
        try {
            String assetPath = assetUtils.buildPathForAsset(fileType, folder, filename);
            Resource resource = new ClassPathResource(assetPath);

            if (resource.exists() && resource.isReadable()) {
                MediaType mediaType = assetUtils
                        .determineMediaType(filename)
                        .orElse(MediaType.APPLICATION_OCTET_STREAM);

                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } else {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "Asset " + filename + " is not found.");
            }
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load asset: " + filename + ".");
        }
    }

    /**
     * Saves a media file to the server and returns its URL.
     *
     * @param file     The file to save.
     * @param fileType The type of file (e.g., "image", "video").
     * @param folder   The folder to store the file in.
     * @return The URL to access the saved file.
     * @throws IOException if there's an issue saving the file.
     */
    public String saveAsset(MultipartFile file, String fileType, String folder) throws IOException {


        // Use AssetUtils to build the storage path for the file
        String folderPath = assetUtils.buildPathForFolder(fileType, folder);
        Path uploadPath = Paths.get(folderPath);

        // Ensure the directory exists
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate a unique file name
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

        String fileName = String.format("%s_%s_%s", fileType, timestamp, file.getOriginalFilename());

        Path filePath = uploadPath.resolve(fileName);

        // Save the file to the specified directory
        Files.copy(file.getInputStream(), filePath);

        // Log the file save operation
        logger.info("Saved asset at path: {}", filePath);

        // Generate and return the URL to access the file
        return assetUtils.buildUrlFor(fileType, folder, fileName);
    }

    private ResponseEntity<BaseResponse<String>> buildErrorResponse(HttpStatus status, String message) {
        BaseResponse<String> errorResponse = BaseResponse.failure(status.value(), message);
        return ResponseEntity.status(status).body(errorResponse);
    }
}