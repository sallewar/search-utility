
package com.searchUtility.core.servlets;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;

import com.searchUtility.core.constants.CommonConstants;
import com.searchUtility.core.services.ReadWriteService;
import com.searchUtility.core.services.SearchToolService;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;

/**
 * @author This servlet acts a data-source for the dropdown field in Carousel
 *         and Product Grid components. It reads the configured categories names
 *         and IDs from the respective locale's data page and forms a
 *         data-source to display them in the drop-down
 */

@Component(service = Servlet.class, property = { Constants.SERVICE_DESCRIPTION
		+ "= Data source to populate the product categories configured in data page, in the carousel and grid component drop-downs",
		"sling.servlet.resourceTypes=" + "searchUtility/componentsDropdownOptions" })

public class ComponentsDataSource extends SlingSafeMethodsServlet {
	
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ComponentsDataSource.class);

	@Reference
	private transient ReadWriteService readWriteService;

	@Reference
	private transient SearchToolService searchToolService;

	@Override
	public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
LOGGER.debug("fetching dropdown components options");
		String COMPONENTS_CONTENT_PATH = searchToolService.getComponentPath();

		Map<String, String> componentsMap = new LinkedHashMap<>();
		Map<String, String> sortedComponentsMap = new LinkedHashMap<>();
		try {
			ResourceResolver resolver = readWriteService.getReadService();
			Resource appsRootResource = resolver.getResource(COMPONENTS_CONTENT_PATH);
			if (appsRootResource != null) {
				setComponentsMap(appsRootResource, componentsMap);
				if (!componentsMap.isEmpty()) {
					componentsMap.entrySet().stream().sorted(Map.Entry.comparingByValue())
							.forEachOrdered(x -> sortedComponentsMap.put(x.getKey(), x.getValue()));
					@SuppressWarnings("unchecked")
					DataSource dataSource = new SimpleDataSource(
							new TransformIterator(sortedComponentsMap.keySet().iterator(), o -> {
								String category = (String) o;
								ValueMap valueMap = new ValueMapDecorator(new HashMap<String, Object>());
								valueMap.put("value", category);
								valueMap.put("text", sortedComponentsMap.get(category));
								LOGGER.debug("Putting key in valuemap : {}", category);
								return new ValueMapResource(resolver, new ResourceMetadata(),
										JcrConstants.NT_UNSTRUCTURED, valueMap);
							}));
					request.setAttribute(DataSource.class.getName(), dataSource);
				}
			}

		} catch (RepositoryException | LoginException e) {
			LOGGER.error("Exception caught in ProductCategoriesDataSource : ", e);
		}
	}

	private void setComponentsMap(Resource parentResource, Map<String, String> componentsMap)
			throws RepositoryException {
		Node parentNode = parentResource.adaptTo(Node.class);
		if (parentNode != null && parentNode.hasProperty(JcrConstants.JCR_PRIMARYTYPE)) {
			String primaryType = parentNode.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString();
			if (NameConstants.NT_COMPONENT.equals(primaryType) && parentNode.hasProperty(JcrConstants.JCR_TITLE)) {
				String componentName = parentNode.getProperty(JcrConstants.JCR_TITLE).getString();
				String componentPath = parentNode.getPath();
				componentsMap.put(componentPath, componentName);

			} else if (parentResource.hasChildren()) {
				Iterable<Resource> childResources = parentResource.getChildren();
				Iterator<Resource> resourceIterator = childResources.iterator();
				while (resourceIterator.hasNext()) {
					Resource childResource = resourceIterator.next();
					setComponentsMap(childResource, componentsMap);
				}
			}
		}
			
	}
}


