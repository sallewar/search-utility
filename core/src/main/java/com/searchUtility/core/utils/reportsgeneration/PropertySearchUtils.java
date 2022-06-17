package com.searchUtility.core.utils.reportsgeneration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import com.searchUtility.core.services.ExternalLinkBuilderService;
import com.searchUtility.core.services.ReadWriteService;
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
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.NameConstants;


public class PropertySearchUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(PropertySearchUtils.class);
	private static List<LinksBean> searchPropertyWorkBookInput;
	private static final String PROPERTY_VALUE_PARAMETER = "propertyValue";
	private static final String SEARCH_PATH_PARAMETER = "propertySearchPath";
	private static final String MESSAGE_KEY = "propertySearchUtility";
	private static final String MESSAGE_STATUS = "propertySearchStatus";

	@Reference
	private ReadWriteService readWriteService;

	public static void findPropertyInContent(SlingHttpServletRequest request, ExternalLinkBuilderService extBuildService,
											 ResourceResolver resolver, JSONObject jsonResponse, SearchToolService searchToolService) throws JSONException {
		searchPropertyWorkBookInput = new ArrayList<>();
		if (request.getParameter(NameConstants.PN_DT_NAME) != null && request.getParameter(PROPERTY_VALUE_PARAMETER) != null) {
			String root = searchToolService.getFallbackContentPath();
			if (StringUtils.isNotBlank(request.getParameter(SEARCH_PATH_PARAMETER))) {
				root = request.getParameter(SEARCH_PATH_PARAMETER).toString();
			}
			Resource rootResource = resolver.getResource(root);
			LOGGER.debug("Root resource = {}", rootResource);
			LOGGER.debug("Root path = {}", root);
			String propertyName = request.getParameter(NameConstants.PN_DT_NAME);
			String propertyValue = request.getParameter(PROPERTY_VALUE_PARAMETER);
			try {
				parseNodes(resolver, rootResource, propertyName, propertyValue, extBuildService);
			} catch (RepositoryException e) {
				LOGGER.error("Error caught :::: ", e);
				jsonResponse.put(MESSAGE_KEY,
						"Error encountered while running Property search. Please refer to logs for more details");
				jsonResponse.put(MESSAGE_STATUS, 500);
			}
		} else {
			jsonResponse.put(MESSAGE_KEY, "Please enter a valid property name and value to search and generate report for.");
			jsonResponse.put(MESSAGE_STATUS, 500);
		}
	}

	private static void parseNodes(ResourceResolver resolver, Resource rootResource, String propertyName,
								   String propertyValue, ExternalLinkBuilderService extBuildService)
			throws RepositoryException {
		if (rootResource != null && rootResource.hasChildren()) {
			Iterable<Resource> children = rootResource.getChildren();
			Iterator<Resource> childrenIterator = children.iterator();

			while (childrenIterator.hasNext()) {
				Resource childResource = childrenIterator.next();
				if (childResource.hasChildren()) {
					parseNodes(resolver, childResource, propertyName, propertyValue, extBuildService);
				}
				parseProperties(childResource, propertyName, propertyValue, extBuildService);
			}
		}
	}

	private static void parseProperties(Resource childResource, String propertyName, String propertyValue, 
			ExternalLinkBuilderService extBuildService) throws RepositoryException {
		Node childNode = childResource.adaptTo(Node.class);
		if (childNode != null) {
			if(childNode.hasProperty(propertyName)) {
				Property property = childNode.getProperty(propertyName);
				if (property.getType() == PropertyType.STRING) {
					findPropValueInProperties(property, childNode, propertyName, propertyValue, extBuildService);
				}
			}
		}
	}

	private static void findPropValueInProperties(Property property, Node childNode, String propertyName, String propertyValue,
											   ExternalLinkBuilderService extBuildService) throws RepositoryException {
		if (property.isMultiple()) {
			Value[] values = property.getValues();
			for (Value value : values) {
				if (StringUtils.containsIgnoreCase(value.getString(), propertyValue)) {
					storeFormattedLink(value.getString(), childNode.getPath(), extBuildService);
				}
			}
		} else {
			String value = property.getString();
			if (StringUtils.containsIgnoreCase(value, propertyValue)) {
				storeFormattedLink(value, childNode.getPath(), extBuildService);
			}
		}
	}

	private static void storeFormattedLink(String fieldValue, String nodeName,
										   ExternalLinkBuilderService extBuildService) {
		LinksBean bean = new LinksBean();
		String formattedNodeName = "/editor.html" + nodeName;
		String formattedLink = extBuildService.buildExternalLink(formattedNodeName);
		formattedLink = formattedLink.split("/jcr:content")[0].split("/_jcr_content")[0] + ".html";
		bean.setNodeName(nodeName);
		bean.setLink(formattedLink);
		bean.setRawLink(fieldValue);
		searchPropertyWorkBookInput.add(bean);

	}

	public static void writeToExcel(JSONObject jsonResponse, ResourceResolver resolver,SearchToolService searchToolService)
			throws IOException, JSONException {
		LOGGER.debug("workBookInput count = {}", searchPropertyWorkBookInput.size());
		Workbook workbook = new XSSFWorkbook();
		try {
			Sheet sheet = workbook.createSheet("Authored Links");
			int rowNum = 1;
			Row headerRow = sheet.createRow(0);
			Cell headerCell1 = headerRow.createCell(0);
			headerCell1.setCellValue("Page Path");
			Cell headerCell2 = headerRow.createCell(1);
			headerCell2.setCellValue("Node Path");
			for (LinksBean bean : searchPropertyWorkBookInput) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(bean.getLink());
				row.createCell(1).setCellValue(bean.getNodeName());
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			workbook.write(baos);

			InputStream is = new ByteArrayInputStream(baos.toByteArray());
			searchPropertyWorkBookInput.clear();

			LOGGER.debug("writing to DAM");
			SavetoDam saveToDam = new SavetoDam();

			int status = saveToDam.writeToDam(is, searchToolService.getDamFolderLocation()+"/property-search", "property-match", resolver);

			if (status == 1) {
				jsonResponse.put(MESSAGE_KEY, "Successfully created Property search report in DAM");
				jsonResponse.put(MESSAGE_STATUS, 200);
			} else {
				jsonResponse.put(MESSAGE_KEY,
						"An error occurred while creating Property search report in DAM. Please refer to logs for more details");
				jsonResponse.put(MESSAGE_STATUS, 500);
			}
		} finally {
			workbook.close();
		}
	}
}
