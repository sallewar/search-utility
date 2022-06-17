package com.searchUtility.core.utils.reportsgeneration;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.searchUtility.core.services.ExternalLinkBuilderService;
import com.searchUtility.core.services.SearchToolService;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class AssetSearchUtils{

	
	
	
	public AssetSearchUtils() {

	}
	
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AssetSearchUtils.class);
	
	private static  Set<String> linkFields = new HashSet<>();

	private static List<LinksBean> assetWorkBookInput = new ArrayList<>();
	private static final String MESSAGE_KEY = "assetSearchUtility";
	private static final String MESSAGE_STATUS = "assetSearchStatus";
	private static final String ASSET_PARAMETER = "assetName";
	private static final String SEARCH_PATH = "assetPath";
	
	public void setAssetFields(ResourceResolver resolver, SlingHttpServletRequest request,
							   ExternalLinkBuilderService extBuildService, JSONObject jsonResponse, SearchToolService searchToolService) throws JSONException {
		String contentRoot = request.getParameter(SEARCH_PATH);
		String assetName = request.getParameter(ASSET_PARAMETER);
		if (StringUtils.isNotBlank(assetName)) {
			if (StringUtils.isBlank(contentRoot)) {
				contentRoot = searchToolService.getFallbackDamPath();
			}
			LOGGER.debug("Asset path = {}", contentRoot);
			Resource rootResource = resolver.getResource(contentRoot);
			getAllAssetFieldNames(assetName, resolver, rootResource, request, extBuildService);
			LOGGER.debug("Size of field names = {}", linkFields.size());
		} else {
			jsonResponse.put(MESSAGE_KEY, "Please enter a valid Asset pattern to search");
			jsonResponse.put(MESSAGE_STATUS, 500);
		}

	}
	
	private void getAllAssetFieldNames(String assetName, ResourceResolver resolver, Resource rootResource,
			SlingHttpServletRequest request, ExternalLinkBuilderService extBuildService) {
		if (rootResource != null && rootResource.hasChildren()) {
			Iterable<Resource> children = rootResource.getChildren();
			Iterator<Resource> childrenIterator = children.iterator();
			while (childrenIterator.hasNext()) {
				Resource childResource = childrenIterator.next();
				if (childResource.hasChildren()) {
					getAllAssetFieldNames(assetName, resolver, childResource, request, extBuildService);
				}
				String searchedImage = childResource.getPath().substring(childResource.getPath(). lastIndexOf('/') + 1);
				if (searchedImage.contains(assetName)) {
					String nodePath = childResource.getPath();
					storeFormattedAssetLink(extBuildService.buildExternalLink("/damadmin#" + nodePath));
				}
			}
		}
	}
	

	private void storeFormattedAssetLink(String nodeName) {
		LinksBean bean = new LinksBean();
		bean.setNodeName(nodeName);
		assetWorkBookInput.add(bean);

	}
	
	public static void writeToAssetExcel(JSONObject jsonResponse, ResourceResolver resolver, SearchToolService searchToolService)
			throws IOException, JSONException {
		LOGGER.debug("workBookInput count = {}", assetWorkBookInput.size());
		Workbook assetWorkbook = new XSSFWorkbook();
		try {
			Sheet sheet = assetWorkbook.createSheet("Authored Links");
			int rowNum = 1;
			Row headerRow = sheet.createRow(0);
			Cell headerCell1 = headerRow.createCell(0);
			headerCell1.setCellValue("Asset Path");
			for (LinksBean bean : assetWorkBookInput) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(bean.getNodeName());
			}
	
			  ByteArrayOutputStream baos = new ByteArrayOutputStream();
			  assetWorkbook.write(baos);
			    
			InputStream is = new ByteArrayInputStream(baos.toByteArray());
			
			assetWorkBookInput.clear();
			
			SavetoDam saveToDam =  new SavetoDam();
			int status = saveToDam.writeToDam(is, searchToolService.getDamFolderLocation()+"/asset-search", "asset-searched-path", resolver);
			
			if (status == 1) {
				jsonResponse.put(MESSAGE_KEY, "Successfully created asset search report in DAM");
				jsonResponse.put(MESSAGE_STATUS, 200);
			} else {
				jsonResponse.put(MESSAGE_KEY,
						"An error occurred while creating asset search report in DAM. Please refer to logs for more details");
				jsonResponse.put(MESSAGE_STATUS, 500);
			}
		} finally {
			assetWorkbook.close();
		}

	}

}
