package com.inazense.laliga_analyzer.downloader.controller;

import com.inazense.laliga_analyzer.commons.constants.Endpoints;
import com.inazense.laliga_analyzer.commons.dto.ApiResponse;
import com.inazense.laliga_analyzer.downloader.service.DownloaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = Endpoints.DOWNLOADER_TAG_NAME, description = Endpoints.DOWNLOADER_TAG_DESC)
@RequiredArgsConstructor
@RequestMapping(Endpoints.DOWNLOADER_REQUEST_MAPPING)
public class DownloaderController {
	
	private final DownloaderService downloaderService;
	
	@Operation(summary = Endpoints.DOWNLOADER_ENDPOINT_DOWNLOAD_SUMMARY)
	@PostMapping(Endpoints.DOWNLOADER_ENDPOINT_DOWNLOAD_PATH)
	public ResponseEntity<ApiResponse> download() {
		return ResponseEntity.ok(downloaderService.downloadData());
	}
}
