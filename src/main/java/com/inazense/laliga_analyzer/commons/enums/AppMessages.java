package com.inazense.laliga_analyzer.commons.enums;

import lombok.Getter;

@Getter
public enum AppMessages {
	
	DOWNLOADER_DOWNLOAD_OK("D_00001", "LaLiga data downloaded successfully."),
	DOWNLOADER_DOWNLOAD_FAIL("D_00002", "Failed to download LaLiga data.");
	
	private String code;
	private String message;
	
	AppMessages(String code, String message) {
		this.code = code;
		this.message = message;
	}
}
