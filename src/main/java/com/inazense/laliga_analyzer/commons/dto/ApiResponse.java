package com.inazense.laliga_analyzer.commons.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse {
	
	private String code;
	private String message;
	private Object data;
}
