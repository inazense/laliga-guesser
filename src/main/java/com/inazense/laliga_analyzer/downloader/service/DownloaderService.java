package com.inazense.laliga_analyzer.downloader.service;

import com.inazense.laliga_analyzer.commons.dto.ApiResponse;
import com.inazense.laliga_analyzer.commons.service.ResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;

import static com.inazense.laliga_analyzer.commons.enums.AppMessages.DOWNLOADER_DOWNLOAD_FAIL;
import static com.inazense.laliga_analyzer.commons.enums.AppMessages.DOWNLOADER_DOWNLOAD_OK;

@Service
@RequiredArgsConstructor
public class DownloaderService {
	
	private final ResponseService responseService;

	@Value("${downloader.baseUrl}")
	private String baseUrl;

	@Value("${downloader.firstSeason}")
	private String firstSeason;

	@Value("${downloader.lastSeason}")
	private String lastSeason;

	@Value("${downloader.filename}")
	private String filename;

	@Value("${downloader.csvFilename}")
	private String csvFilename;

	public ApiResponse downloadData() {
		ApiResponse response = responseService.createResponse(DOWNLOADER_DOWNLOAD_OK, null);
		
		int firstYear = Integer.parseInt(firstSeason);
		int lastYear = Integer.parseInt(lastSeason);
		
		try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilename))) {
			boolean firstHeader = true;
			
			for (int year = firstYear; year <= lastYear; year++) {
				String season = String.format("%02d%02d", year, (year + 1) % 100);
				String url = baseUrl + season + filename;
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(new URL(url).openStream()))) {
					String line;
					boolean isFirstLine = true;
					while ((line = reader.readLine()) != null) {
						if (isFirstLine) {
							if (firstHeader) {
								writer.println(line);
								firstHeader = false;
							}
							isFirstLine = false;
						} else {
							writer.println(line);
						}
					}
				} catch (IOException e) {
					response = responseService.createResponse(DOWNLOADER_DOWNLOAD_FAIL, e.getMessage());
					
				}
			}
		}
		catch (Exception e) {
			response = responseService.createResponse(DOWNLOADER_DOWNLOAD_FAIL, e.getMessage());
		}
		
		return response;
	}
}
