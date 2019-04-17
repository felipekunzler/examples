package com.sap.demo.applicationconnector.rest;

import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import com.sap.demo.applicationconnector.client.PairingService;
import com.sap.demo.applicationconnector.client.RegistrationService;
import com.sap.demo.applicationconnector.entity.Connection;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;

@Profile("ApplicationConnector")
@RestController
public class ApplicationConnectorApi {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private PairingService pairingService;

	@PostMapping("/api/v1/applicationconnector/registration/automatic")
	@ApiOperation(value = "Register to Kyma Application automatically", notes = "This Operation registers "
			+ "the Service to the configured Kyma Application automatically")
	public ResponseEntity<Map<String, String>> connectivityTest(@RequestBody ConnectUrl connectUrl) {

		URI connectionURI = URI.create(connectUrl.getUrl());

		Connection connection = pairingService.executeInitialPairing(connectionURI);

		if (connection != null) {
			String registrationId = registrationService.registerWithKymaInstance();
			if (StringUtils.isNotBlank(registrationId)) {
				return new ResponseEntity<Map<String, String>>(Collections.singletonMap("id", registrationId),
						HttpStatus.OK);
			} else {
				return new ResponseEntity<Map<String, String>>(Collections.singletonMap("id", registrationId),
						HttpStatus.valueOf(500));
			}
		} else {
			return new ResponseEntity<Map<String, String>>(
					Collections.singletonMap("response", "Pairing with Kyma application failed. Please see logs."),
					HttpStatus.valueOf(500));
		}
	}

	@PostMapping("/api/v1/applicationconnector/registration/manual")
	@ApiOperation(value = "Register to Kyma Application manually", notes = "This Operation registers "
			+ "the Service to the configured Kyma Application using the JKS that was created manually", consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Map<String, String>> connectivityTest(@RequestParam String infoUrl,
			@ApiParam(name = "jksFile", value = "Upload the generated JKS file", required = true) @RequestParam("jksFile") MultipartFile jksFile) {

		KeyStore keyStore = pairingService.createKeyStoreFromFile(jksFile);
		URI info = URI.create(infoUrl);

		Connection connection = pairingService.executeManualPairing(info, keyStore);
		
		String registrationId = registrationService.registerWithKymaInstance();

		if (StringUtils.isNotBlank(registrationId)) {
			return new ResponseEntity<Map<String, String>>(Collections.singletonMap("id", registrationId),
					HttpStatus.OK);
		} else {
			return new ResponseEntity<Map<String, String>>(Collections.singletonMap("id", registrationId),
					HttpStatus.valueOf(500));
		}
	}

	@DeleteMapping("/api/v1/applicationconnector/registration")
	@ApiOperation(value = "Delete Kyma Application registrations", notes = "This Operation deletes all Application registration information.")
	public ResponseEntity<String> deleteServiceRegistration() {

		registrationService.deleteRegistrations();
		pairingService.deleteConnections();

		return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
	}

	@GetMapping("/api/v1/applicationconnector/registration")
	@ApiOperation(value = "Read Kyma Application registrations", notes = "This Operation reads all Application registration information.")
	public ResponseEntity<List<RegistrationService.RegistrationQueryResponse>> readServiceRegistrations() {

		return new ResponseEntity<List<RegistrationService.RegistrationQueryResponse>>(
				registrationService.getExistingRegistrations(), HttpStatus.OK);
	}

	// Model for Get Response
	@Data
	public static class ConnectUrl {
		@ApiModelProperty(name = "url", example = "<URL_from_Kyma>")
		private String url;
	}

	@Data
	public static class MetaInformation {
		private String infoUrl;
		private String certificateSubject;
		private String certificateAlgorithm;
	}
}
