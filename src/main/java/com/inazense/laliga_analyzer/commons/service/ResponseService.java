package com.inazense.laliga_analyzer.commons.service;

import com.inazense.laliga_analyzer.commons.dto.ApiResponse;
import com.inazense.laliga_analyzer.commons.enums.AppMessages;
import org.springframework.stereotype.Service;

@Service
public class ResponseService {

	public ApiResponse createResponse(AppMessages msg, Object data) {
		return ApiResponse.builder()
				.code(msg.getCode())
				.message(msg.getMessage())
				.data(data)
				.build();
	}
}
