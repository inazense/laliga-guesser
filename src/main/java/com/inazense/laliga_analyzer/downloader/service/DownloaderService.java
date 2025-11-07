package com.inazense.laliga_analyzer.downloader.service;

import com.inazense.laliga_analyzer.commons.dto.ApiResponse;
import com.inazense.laliga_analyzer.commons.enums.AppMessages;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DownloaderService {

	@Value("${downloader.baseUrl}")
	private String baseUrl;

	@Value("${downloader.firstSeason}")
	private String firstSeason;

	@Value("${downloader.lastSeason}")
	private String lastSeason;

	@Value("${downloader.filename}")
	private String filename;

	@Value("${downloader.tmpFilename}")
	private String tmpFilename;

	@Value("${downloader.splitter}")
	private String splitter;

	@Value("${downloader.seasonColumn}")
	private String seasonColumn;

	@Value("${downloader.csvFilename}")
	private String csvFilename;

	public ApiResponse downloadData() {
		AppMessages response = AppMessages.DOWNLOADER_DOWNLOAD_OK;
		
		int firstYear = Integer.parseInt(firstSeason);
		int lastYear = Integer.parseInt(lastSeason);
		
		return ApiResponse.builder().code(response.getCode()).message(response.getMessage()).build();
	}
}
