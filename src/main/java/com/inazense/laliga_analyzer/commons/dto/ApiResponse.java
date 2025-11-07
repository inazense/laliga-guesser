package com.inazense.laliga_analyzer.commons.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApiResponse {
	
	private String code;
	private String message;
	private Object data;
}
