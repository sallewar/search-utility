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
import javax.jcr.PropertyIterator;
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



public class StringSearchUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(StringSearchUtils.class);
	private static List<LinksBean> searchStringWorkBookInput;
	private static final String STRING_PARAMETER = "searchString";
	private static final String SEARCH_PATH_PARAMETER = "rootPath";
	private static final String MESSAGE_KEY = "stringSearchUtility";
	private static final String MESSAGE_STATUS = "stringSearchStatus";

	@Reference
	private ReadWriteService readWriteService;

	public static void findStringInContent(SlingHttpServletRequest request, ExternalLinkBuilderService extBuildService,
										   ResourceResolver resolver, JSONObject jsonResponse, SearchToolService searchToolService) throws JSONException {
		searchStringWorkBookInput = new ArrayList<>();
		if (request.getParameter(STRING_PARAMETER) != null) {
			String root = searchToolService.getFallbackContentPath();
			String searchString = request.getParameter(STRING_PARAMETER);
			if (StringUtils.isNotBlank(request.getParameter(SEARCH_PATH_PARAMETER))) {
				root = request.getParameter(SEARCH_PATH_PARAMETER).toString();
			}
			Resource rootResource = resolver.getResource(root);
			LOGGER.debug("Root resource = {}", rootResource);
			LOGGER.debug("Root path = {}", root);
			try {
				parseNodes(resolver, rootResource, searchString, extBuildService);
			} catch (RepositoryException e) {
				LOGGER.error("Error caught :::: {}", e);
				jsonResponse.put(MESSAGE_KEY,
						"Error encountered while running search string. Please refer to logs for more details");
				jsonResponse.put(MESSAGE_STATUS, 500);
			}
		} else {
			jsonResponse.put(MESSAGE_KEY, "Please enter a valid search string to generate report for.");
			jsonResponse.put(MESSAGE_STATUS, 500);
		}
	}

	private static void parseNodes(ResourceResolver resolver, Resource rootResource, String searchString,
								   ExternalLinkBuilderService extBuildService)
			throws RepositoryException {
		if (rootResource != null && rootResource.hasChildren()) {
			Iterable<Resource> children = rootResource.getChildren();
			Iterator<Resource> childrenIterator = children.iterator();

			while (childrenIterator.hasNext()) {
				Resource childResource = childrenIterator.next();
				if (childResource.hasChildren()) {
					parseNodes(resolver, childResource, searchString, extBuildService);
				}
				parseProperties(childResource, searchString, extBuildService);
			}
		}
	}

	private static void parseProperties(Resource childResource, String searchString,
										ExternalLinkBuilderService extBuildService) throws RepositoryException {
		Node childNode = childResource.adaptTo(Node.class);
		if (childNode != null) {
			PropertyIterator propIterator = childNode.getProperties();
			while (propIterator.hasNext()) {
				Property property = propIterator.nextProperty();
				if (property.getType() == PropertyType.STRING) {
					findStringInProperties(property, childNode, searchString, extBuildService);
				}
			}
		}
	}

	private static void findStringInProperties(Property property, Node childNode, String searchString,
											   ExternalLinkBuilderService extBuildService) throws RepositoryException {
		if (property.isMultiple()) {
			Value[] values = property.getValues();
			for (Value value : values) {
				if (StringUtils.containsIgnoreCase(value.getString(), searchString)) {
					storeFormattedLink(value.getString(), childNode.getPath(), extBuildService);
					// LOGGER.debug("Found match in multi-value property = {}", value.getString());
				}
			}
		} else {
			String value = property.getString();
			if (StringUtils.containsIgnoreCase(value, searchString)) {
				storeFormattedLink(value, childNode.getPath(), extBuildService);
				// LOGGER.debug("Found match in single-valued property = {}", value);
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
		searchStringWorkBookInput.add(bean);

	}

	public static void writeToExcel(JSONObject jsonResponse, ResourceResolver resolver,SearchToolService searchToolService)
			throws IOException, JSONException {
		LOGGER.debug("workBookInput count = {}", searchStringWorkBookInput.size());
		Workbook workbook = new XSSFWorkbook();
		try {
			Sheet sheet = workbook.createSheet("Authored Links");
			int rowNum = 1;
			Row headerRow = sheet.createRow(0);
			Cell headerCell1 = headerRow.createCell(0);
			headerCell1.setCellValue("Page Path");
			Cell headerCell2 = headerRow.createCell(1);
			headerCell2.setCellValue("Node Path");
			for (LinksBean bean : searchStringWorkBookInput) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(bean.getLink());
				row.createCell(1).setCellValue(bean.getNodeName());
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			workbook.write(baos);

			InputStream is = new ByteArrayInputStream(baos.toByteArray());
			searchStringWorkBookInput.clear();

			LOGGER.debug("writing to DAM");
			SavetoDam saveToDam = new SavetoDam();

			int status = saveToDam.writeToDam(is, searchToolService.getDamFolderLocation()+"/string-search", "string-match", resolver);

			if (status == 1) {
				jsonResponse.put(MESSAGE_KEY, "Successfully created String search report in DAM");
				jsonResponse.put(MESSAGE_STATUS, 200);
			} else {
				jsonResponse.put(MESSAGE_KEY,
						"An error occurred while creating string search report in DAM. Please refer to logs for more details");
				jsonResponse.put(MESSAGE_STATUS, 500);
			}
		} finally {
			workbook.close();
		}
	}
}
