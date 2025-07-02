package com.quip.backend.asset.controller;

import com.quip.backend.asset.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetController {

    private static final Logger logger = LoggerFactory.getLogger(AssetController.class);

    private final AssetService assetService;

    /**
     * Endpoint to retrieve an asset by file type, folder, and filename.
     */
    @GetMapping("{fileType}/{folder}/{filename:.+}")
    public ResponseEntity<?> getAsset(
            @PathVariable String fileType,
            @PathVariable String folder,
            @PathVariable String filename) {
        return assetService.getAsset(fileType, folder, filename);
    }

    /**
     * Endpoint to upload an asset.
     *
     * @param file     The file to upload.
     * @param fileType The type of the file (e.g., "image", "video").
     * @param folder   The folder where the file should be stored.
     * @return A ResponseEntity containing the URL of the uploaded asset or an error message.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") String fileType,
            @RequestParam("folder") String folder) {
        try {
            // Call the assetService to save the file and get its URL
            String fileUrl = assetService.saveAsset(file, fileType, folder);
            logger.info("Asset uploaded successfully. File URL: {}", fileUrl);
            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            logger.error("Failed to upload asset", e);
            return ResponseEntity.status(500).body("Failed to upload asset");
        }
    }
}