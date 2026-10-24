package com.ntaganira.heritier.ipos.backoffice.web;

import com.ntaganira.heritier.ipos.backoffice.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.concurrent.TimeUnit;

@Controller
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/files/{*path}")
    public ResponseEntity<Resource> file(@PathVariable String path) {
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        Resource resource = storageService.load(relativePath);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storageService.contentType(relativePath)))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }
}
