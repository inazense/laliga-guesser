package com.inazense.laliga_analyzer.commons.constants;

public class Endpoints {
	
	public static final String DOWNLOADER_TAG_NAME = "Downloader";
	public static final String DOWNLOADER_TAG_DESC = "API to download LaLiga data from external sources";
	public static final String DOWNLOADER_REQUEST_MAPPING = "/downloader";
	public static final String DOWNLOADER_ENDPOINT_DOWNLOAD_PATH = "/download";
	public static final String DOWNLOADER_ENDPOINT_DOWNLOAD_SUMMARY = "Download data and create csv";
	
	public static final String PREDICTOR_TAG_NAME = "Predictor";
	public static final String PREDICTOR_TAG_DESC = "Match prediction endpoints";
	public static final String PREDICTOR_REQUEST_MAPPING = "/predictor";
	public static final String PREDICTOR_ENDPOINT_PREDICT_PATH = "/predict";
	public static final String PREDICTOR_ENDPOINT_PREDICT_SUMMARY = "Predict match result";
	
}
