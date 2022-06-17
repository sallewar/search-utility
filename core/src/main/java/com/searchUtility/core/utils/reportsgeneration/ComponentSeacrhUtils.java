package com.searchUtility.core.utils.reportsgeneration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.searchUtility.core.constants.CommonConstants;
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
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ComponentSeacrhUtils {

	private ComponentSeacrhUtils() {

	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ComponentSeacrhUtils.class);
	
	private static  Set<String> linkFields = new HashSet<>();

	private static List<LinksBean> componentWorkBookInput = new ArrayList<>();
	
	private static final String COMPONENT_PARAMETER = "compName";
	private static final String SEARCH_PATH_PARAMETER = "compPath";
	private static final String MESSAGE_KEY = "componentSearchUtility";
	private static final String MESSAGE_STATUS = "componentSearchStatus";
	
	
	public static void setLinkFields(ResourceResolver resolver, SlingHttpServletRequest request,
									 ExternalLinkBuilderService extBuildService, JSONObject jsonResponse, SearchToolService searchToolService) throws JSONException {
		List<String> resourceTypes = new ArrayList<>();
		String componentName = request.getParameter(COMPONENT_PARAMETER);
		String contentRoot = request.getParameter(SEARCH_PATH_PARAMETER);
		if (StringUtils.isBlank(contentRoot)) {
			contentRoot = searchToolService.getFallbackContentPath();
		}
		LOGGER.debug("Component node path = {} and search path = {}", componentName, contentRoot);
		if (StringUtils.isNotBlank(componentName) && componentName.contains(CommonConstants.SLASH + CommonConstants.APPS + CommonConstants.SLASH)) {
			String formattedComponentName = componentName.split(CommonConstants.SLASH + CommonConstants.APPS + CommonConstants.SLASH)[1];
			LOGGER.debug("formatted component name = {}", formattedComponentName);
			resourceTypes.add(formattedComponentName);
			Resource rootResource = resolver.getResource(contentRoot);
			if (rootResource != null) {
				getAllLinkFieldNames(resolver, rootResource, resourceTypes, extBuildService);
				LOGGER.debug("Size of field names = {}", linkFields.size());
			} else {
				jsonResponse.put(MESSAGE_KEY,
						"Please enter a valid search path to generate report for component search.");
				jsonResponse.put(MESSAGE_STATUS, 500);
			}
		} else {
			jsonResponse.put(MESSAGE_KEY, "Please select a valid component to generate report for.");
			jsonResponse.put(MESSAGE_STATUS, 500);
		}
	}

	private static void getAllLinkFieldNames(ResourceResolver resolver, Resource rootResource,
			List<String> resourceTypes, ExternalLinkBuilderService extBuildService) {
		if (rootResource.hasChildren()) {
			Iterable<Resource> children = rootResource.getChildren();
			Iterator<Resource> childrenIterator = children.iterator();
			while (childrenIterator.hasNext()) {
				Resource childResource = childrenIterator.next();
				if (childResource.hasChildren()) {
					getAllLinkFieldNames(resolver, childResource, resourceTypes,extBuildService);
				}
				matchProperty(childResource, resourceTypes, extBuildService);
			}
		}
	}

	private static void matchProperty(Resource childResource, List<String> resourceTypes,
			ExternalLinkBuilderService extBuildService) {
		ValueMap valueMap = childResource.getValueMap();
		if (valueMap.get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY) != null) {
			String resourceType = (String) valueMap.get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
			if (resourceTypes.contains(resourceType)) {
				storeFormattedLink(childResource.getPath(), extBuildService);
			}
		}
	}


	private static void storeFormattedLink(String nodeName,ExternalLinkBuilderService extBuildService) {
		LinksBean bean = new LinksBean();
		String formattedNodeName = "/editor.html" + nodeName;
		String formattedLink = extBuildService.buildExternalLink(formattedNodeName);
		formattedLink = formattedLink.split("/jcr:content")[0].split("/_jcr_content")[0] + ".html";
		bean.setNodeName(nodeName);
		bean.setLink(formattedLink);
		componentWorkBookInput.add(bean);

	}

	public static void writeToExcel(JSONObject jsonResponse, ResourceResolver resolver,SearchToolService searchToolService)
			throws IOException, JSONException {
		int searchSize = componentWorkBookInput.size();
		LOGGER.debug("workBookInput count = {}", componentWorkBookInput.size());
		Workbook workbook = new XSSFWorkbook();
		try {
			Sheet sheet = workbook.createSheet("Authored Links");
			int rowNum = 1;
			Row headerRow = sheet.createRow(0);
			Cell headerCell1 = headerRow.createCell(0);
			headerCell1.setCellValue("Page Path");
			Cell headerCell2 = headerRow.createCell(1);
			headerCell2.setCellValue("Node Path");
			for (LinksBean bean : componentWorkBookInput) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(bean.getLink());
				row.createCell(1).setCellValue(bean.getNodeName());
			}
			  ByteArrayOutputStream baos = new ByteArrayOutputStream();
			    workbook.write(baos);
			    
			InputStream is = new ByteArrayInputStream(baos.toByteArray());
			componentWorkBookInput.clear();
	
			SavetoDam saveToDam =  new SavetoDam();
			
			int status = saveToDam.writeToDam(is, searchToolService.getDamFolderLocation()+"/component-search", "component-searched-path",
					resolver);
			
			if (status == 1) {
				jsonResponse.put(MESSAGE_KEY, "Successfully created Component search report in DAM");
				jsonResponse.put(MESSAGE_STATUS, 200);
			} else {
				jsonResponse.put(MESSAGE_KEY,
						"An error occurred while creating Component search report in DAM. Please refer to logs for more details");
				jsonResponse.put(MESSAGE_STATUS, 500);
			}
		} finally {
			workbook.close();
		}
	}
}
