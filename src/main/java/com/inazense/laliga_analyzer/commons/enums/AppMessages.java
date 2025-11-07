package com.inazense.laliga_analyzer.commons.enums;

import lombok.Getter;

@Getter
public enum AppMessages {
	
	DOWNLOADER_DOWNLOAD_OK("D_00001", "LaLiga data downloaded successfully."),
	DOWNLOADER_DOWNLOAD_FAIL("D_00002", "Failed to download LaLiga data."),
	PREDICTOR_PREDICT_OK("P_00001", "Prediction done."),
	PREDICTOR_PREDICT_FAIL_MODEL("P_00002", "Model not loaded. Please train the model first."),
	PREDICTOR_PREDICT_FAIL_DATE("P_00003", "Invalid date format. Use yyyy-MM-dd");
	private String code;
	private String message;
	
	AppMessages(String code, String message) {
		this.code = code;
		this.message = message;
	}
}
