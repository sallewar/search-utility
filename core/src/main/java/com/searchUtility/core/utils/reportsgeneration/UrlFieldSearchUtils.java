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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import com.searchUtility.core.constants.CommonConstants;
import com.searchUtility.core.services.ExternalLinkBuilderService;
import com.searchUtility.core.services.SearchToolService;
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


public class UrlFieldSearchUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(UrlFieldSearchUtils.class);
	private static Set<String> linkFields;
	private static List<LinksBean> searchUrlFieldsWorkBookInput;
	private static final String MESSAGE_KEY = "linkSearchUtility";
	private static final String MESSAGE_STATUS = "linkSearchStatus";
	private static final String URL_PARAMETER = "urlPattern";
	private static final String SEARCH_PATH_PARAMETER = "searchPath";

	public static void findLinkPatternInLinkFieldsRTE(SlingHttpServletRequest request,
													  ExternalLinkBuilderService extBuildService, JSONObject jsonResponse, SearchToolService searchToolService) throws JSONException {

		ResourceResolver resolver = request.getResourceResolver();
		List<String> searchText = new ArrayList<>();


		if (request.getParameter(URL_PARAMETER) != null) {
			linkFields = new HashSet<>();
			searchUrlFieldsWorkBookInput = new ArrayList<>();
			String urlPatternParameter = request.getParameter(URL_PARAMETER);
			String[] urlPatterns = urlPatternParameter.split(CommonConstants.COMMA);
			for (String pattern : urlPatterns) {
				searchText.add(pattern);
			}
			String root = searchToolService.getFallbackContentPath();
			if (request.getParameter(SEARCH_PATH_PARAMETER) != null) {
				root = request.getParameter(SEARCH_PATH_PARAMETER);
			}
			setLinkFields(resolver,searchToolService);
			getAllLinks(resolver, root, searchText, extBuildService, jsonResponse);
		} else {

			jsonResponse.put(MESSAGE_KEY, "Please enter a valid URL pattern to search");
			jsonResponse.put(MESSAGE_STATUS, 500);

		}
	}

	private static void getAllLinks(ResourceResolver resolver, String root, List<String> searchText,
			ExternalLinkBuilderService extBuildService, JSONObject jsonResponse) throws JSONException {

		Resource rootResource = resolver.getResource(root);
		try {
			parseNodes(resolver, rootResource, searchText, extBuildService);
		} catch (RepositoryException e) {
			LOGGER.error("Error caught :::: {}", e);
			jsonResponse.put(MESSAGE_KEY, "Error encountered during URL pattern. Please refer logs for more details.");
			jsonResponse.put(MESSAGE_STATUS, 500);
		}

	}

	private static void setLinkFields(ResourceResolver resolver,SearchToolService searchToolService) {
		List<String> resourceTypes = new ArrayList<>();
		resourceTypes.add("granite/ui/components/coral/foundation/form/pathfield");
		resourceTypes.add("granite/ui/components/coral/foundation/form/pathbrowser");
		resourceTypes.add("cq/gui/components/authoring/dialog/richtext");

		String appsRoot = searchToolService.getComponentPath();
		Resource rootResource = resolver.getResource(appsRoot);
		getAllLinkFieldNames(resolver, rootResource, resourceTypes);
		LOGGER.debug("Size of field names = {}", linkFields.size());

	}

	private static void getAllLinkFieldNames(ResourceResolver resolver, Resource rootResource,
			List<String> resourceTypes) {
		if (rootResource != null && rootResource.hasChildren()) {
			Iterable<Resource> children = rootResource.getChildren();
			Iterator<Resource> childrenIterator = children.iterator();
			while (childrenIterator.hasNext()) {
				Resource childResource = childrenIterator.next();
				if (childResource.hasChildren()) {
					getAllLinkFieldNames(resolver, childResource, resourceTypes);
				}
				addLinkFieldNames(childResource, resourceTypes);
			}
		}
	}

	private static void addLinkFieldNames(Resource childResource, List<String> resourceTypes) {
		ValueMap valueMap = childResource.getValueMap();
		if (valueMap.get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY) != null) {
			String resourceType = (String) valueMap.get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
			if (resourceTypes.contains(resourceType)) {
				String fieldName = (String) valueMap.get("name");
				linkFields.add(fieldName.split("/")[1]);
			}
		}
	}

	private static void parseNodes(ResourceResolver resolver, Resource rootResource, List<String> searchText,
			ExternalLinkBuilderService extBuildService)
			throws RepositoryException {
		if (rootResource != null && rootResource.hasChildren()) {
			Iterable<Resource> children = rootResource.getChildren();
			Iterator<Resource> childrenIterator = children.iterator();
			while (childrenIterator.hasNext()) {
				Resource childResource = childrenIterator.next();
				if (childResource.hasChildren()) {
					parseNodes(resolver, childResource, searchText, extBuildService);
				}
				parseProperties(childResource, searchText, extBuildService);
			}
		}
	}

	private static void parseProperties(Resource childResource, List<String> searchString,
			ExternalLinkBuilderService extBuildService) throws RepositoryException {
		Node childNode = childResource.adaptTo(Node.class);
		for (String property : linkFields) {
			if (childNode != null && childNode.hasProperty(property)) {
				findUrlPatternInProperties(property, childNode, searchString, extBuildService);

			}
		}

	}

	private static void findUrlPatternInProperties(String property, Node childNode, List<String> searchString,
			ExternalLinkBuilderService extBuildService) throws RepositoryException {
		Property nodeProperty = childNode.getProperty(property);
		if (nodeProperty.isMultiple()) {
			findUrlPatternInMultiValueField(nodeProperty, childNode, searchString, extBuildService);
		} else {
			String linkValue = nodeProperty.getString();
			for (String pattern : searchString) {
				if (linkValue.contains(pattern)) {
					storeFormattedLink(linkValue, childNode.getPath(), extBuildService);
					LOGGER.debug("Link Value = {}", linkValue);
				}
			}
		}
	}

	private static void findUrlPatternInMultiValueField(Property nodeProperty, Node childNode,
			List<String> searchString, ExternalLinkBuilderService extBuildService) throws RepositoryException {
		Value[] values = nodeProperty.getValues();
		for (Value value : values) {
			String propertyValue = value.getString();
			for (String pattern : searchString) {
				if (propertyValue.contains(pattern)) {
					storeFormattedLink(propertyValue, childNode.getPath(), extBuildService);
					LOGGER.debug("Link Value = {}", propertyValue);
				}
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
		searchUrlFieldsWorkBookInput.add(bean);

	}

	public static void writeToExcel(JSONObject jsonResponse, ResourceResolver resolver,SearchToolService searchToolService)
			throws IOException, JSONException {
		LOGGER.debug("workBookInput count = {}", searchUrlFieldsWorkBookInput.size());
		Workbook workbook = new XSSFWorkbook();
		try {
			Sheet sheet = workbook.createSheet("Authored Links");
			int rowNum = 1;
			Row headerRow = sheet.createRow(0);
			Cell headerCell1 = headerRow.createCell(0);
			headerCell1.setCellValue("Page Path");
			Cell headerCell2 = headerRow.createCell(1);
			headerCell2.setCellValue("Node Path");
			for (LinksBean bean : searchUrlFieldsWorkBookInput) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(bean.getLink());
				row.createCell(1).setCellValue(bean.getNodeName());
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			workbook.write(baos);
	
			InputStream is = new ByteArrayInputStream(baos.toByteArray());
			searchUrlFieldsWorkBookInput.clear();
	
			SavetoDam saveToDam = new SavetoDam();
	
			int status = saveToDam.writeToDam(is, searchToolService.getDamFolderLocation()+"/link-search", "url-match", resolver);
	
			if (status == 1) {
				jsonResponse.put(MESSAGE_KEY, "Successfully created link search report in DAM");
				jsonResponse.put(MESSAGE_STATUS, 200);
			} else {
				jsonResponse.put(MESSAGE_KEY,
						"An error occurred while creating link search report in DAM. Please refer to logs for more details");
				jsonResponse.put(MESSAGE_STATUS, 500);
			}
		} finally {
			workbook.close();
		}
	}

}
