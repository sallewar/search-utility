package com.searchUtility.core.servlets;

import java.io.IOException;


import javax.jcr.RepositoryException;
import javax.servlet.Servlet;

import com.adobe.agl.impl.InvalidFormatException;
import com.searchUtility.core.constants.CommonConstants;
import com.searchUtility.core.services.ExternalLinkBuilderService;
import com.searchUtility.core.services.ReadWriteService;
import com.searchUtility.core.services.SearchToolService;
import com.searchUtility.core.utils.reportsgeneration.*;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Component(service = Servlet.class, property = { Constants.SERVICE_DESCRIPTION + "=Simple Demo Servlet",
		"sling.servlet.methods=" + HttpConstants.METHOD_GET, "sling.servlet.resourceTypes=" + "searchUtility/searchTool" })

public class SearchUtility extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchUtility.class);
	private static final String RUN_REPORT_ASSET_SEARCH = "activeAsset";
	private static final String RUN_REPORT_COMPONENT_SEARCH = "activeComp";
	private static final String RUN_REPORT_STRING_SEARCH = "activeSearchString";
	private static final String RUN_REPORT_URL_SEARCH = "activeSearchUrl";
	private static final String RUN_REPORT_PROPERTY_SEARCH = "activeProperty";
	private static final String RUN_REPORT_VANITY_SEARCH = "vanityPropertyActive";

	private static final String ASSET_SEARCH_STATUS = "assetSearchStatus";
	private static final String COMPONENT_SEARCH_STATUS = "componentSearchStatus";
	private static final String STRING_SEARCH_STATUS = "stringSearchStatus";
	private static final String LINK_SEARCH_STATUS = "linkSearchStatus";
	private static final String PROPERTY_SEARCH_STATUS = "propertySearchStatus";

	public static final transient AssetSearchUtils assetSearchUtils = new AssetSearchUtils();

	@Reference
	private transient ReadWriteService readWriteService;

	@Reference
	private transient ExternalLinkBuilderService extBuildService;

	@Reference
	private transient ResourceResolverFactory resolverFactory;

	@Reference
	private transient SearchToolService searchToolService;

	@Override
	public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {

		String activeAsset = request.getParameter(RUN_REPORT_ASSET_SEARCH);
		String activeComp = request.getParameter(RUN_REPORT_COMPONENT_SEARCH);
		String activeSearchString = request.getParameter(RUN_REPORT_STRING_SEARCH);
		String activeUrlPatternMatch = request.getParameter(RUN_REPORT_URL_SEARCH);
		String activePropertyMatch = request.getParameter(RUN_REPORT_PROPERTY_SEARCH);
		String activeVanityMatch = request.getParameter(RUN_REPORT_VANITY_SEARCH);

		LOGGER.debug("COMP search: {}\n ASSET search: {}\n String search: {}\n URL search: {}\n Property Search: {} \n Vanity Search: {}", activeComp, activeAsset,
				activeSearchString, activeUrlPatternMatch, activePropertyMatch);


		JSONObject jsonResponse = new JSONObject();

		try {
			ResourceResolver resolver = readWriteService.getReadService();
			ResourceResolver writeResolver = readWriteService.getWriteService();
			jsonResponse.put(CommonConstants.MESSAGE,
					"Please enable report generation for at least one module, from the dialog.");
			jsonResponse.put("statusCode", 500);

			if (CommonConstants.TRUE.equals(activeAsset)) {
				assetSearchUtils.setAssetFields(resolver, request, extBuildService, jsonResponse,searchToolService);

				if (!jsonResponse.has(ASSET_SEARCH_STATUS)
						|| jsonResponse.getInt(ASSET_SEARCH_STATUS) != 500) {
					AssetSearchUtils.writeToAssetExcel(jsonResponse, writeResolver,searchToolService);
				}
				LOGGER.debug("STATUS CODE FOR ASSET SEARCH: {}", jsonResponse.get("assetSearchUtility"));

			}
			if (CommonConstants.TRUE.equals(activeComp)) {
				ComponentSeacrhUtils.setLinkFields(resolver, request, extBuildService, jsonResponse,searchToolService);
				if (!jsonResponse.has(COMPONENT_SEARCH_STATUS) || jsonResponse.getInt(COMPONENT_SEARCH_STATUS) != 500) {
					ComponentSeacrhUtils.writeToExcel(jsonResponse, writeResolver,searchToolService);
				}
				LOGGER.debug("STATUS CODE FOR COMPONENT SEARCH: {}", jsonResponse.get("componentSearchUtility"));

			}
			if (CommonConstants.TRUE.equals(activeSearchString)) {
				StringSearchUtils.findStringInContent(request, extBuildService, resolver, jsonResponse,searchToolService);
				if (!jsonResponse.has(STRING_SEARCH_STATUS) || jsonResponse.getInt(STRING_SEARCH_STATUS) != 500) {
					StringSearchUtils.writeToExcel(jsonResponse, writeResolver,searchToolService);
				}
				LOGGER.debug("STATUS CODE FOR STRING SEARCH: {}", jsonResponse.get("stringSearchUtility"));

			}
			if (CommonConstants.TRUE.equals(activeUrlPatternMatch)) {
				UrlFieldSearchUtils.findLinkPatternInLinkFieldsRTE(request, extBuildService, jsonResponse,searchToolService);
				if (!jsonResponse.has(LINK_SEARCH_STATUS) || jsonResponse.getInt(LINK_SEARCH_STATUS) != 500) {
					UrlFieldSearchUtils.writeToExcel(jsonResponse, writeResolver,searchToolService);
				}
				LOGGER.debug("STATUS CODE FOR URL SEARCH: {}", jsonResponse.get("linkSearchUtility"));

			}
			if (CommonConstants.TRUE.equals(activePropertyMatch)) {
				PropertySearchUtils.findPropertyInContent(request, extBuildService, resolver, jsonResponse,searchToolService);
				if (!jsonResponse.has(PROPERTY_SEARCH_STATUS) || jsonResponse.getInt(PROPERTY_SEARCH_STATUS) != 500) {
					PropertySearchUtils.writeToExcel(jsonResponse, writeResolver,searchToolService);
				}
				LOGGER.debug("STATUS CODE FOR PROPERTY SEARCH: {}", jsonResponse.get("propertySearchUtility"));

			}

			response.getWriter().print(jsonResponse);
			LOGGER.debug("JSON response object = {}", jsonResponse);
		} catch (IOException | JSONException | LoginException e) {
			LOGGER.error("RepositoryException caught in AssetCompSearch : ", e);
		}
	}


}