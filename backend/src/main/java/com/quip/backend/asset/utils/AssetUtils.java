package com.quip.backend.asset.utils;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

@Getter
@Configuration
public class AssetUtils {

    @Value("${app.assets.base-url}")
    private String baseUrl;

    @Value("${app.assets.base-path}")
    private String basePath;


    public String buildUrlFor(String mediaType, String mediaFolder, String filename) {
        return String.format("%s%s/%s/%s", baseUrl, mediaType, mediaFolder, filename);
    }

    public String buildPathForAsset(String mediaType) {
        return String.format("%s%s", basePath, mediaType);
    }

    public String buildPathForFolder(String mediaType, String mediaFolder) {
        return String.format("%s%s/%s", basePath, mediaType, mediaFolder);
    }

    public String buildPathForAsset(String mediaType, String mediaFolder, String fileName) {
        return String.format("%s%s/%s/%s", basePath, mediaType, mediaFolder, fileName);
    }

    public Optional<MediaType> determineMediaType(String filename) {
        try {
            String mimeType = Files.probeContentType(Paths.get(filename));
            return Optional.of(MediaType.parseMediaType(mimeType));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}