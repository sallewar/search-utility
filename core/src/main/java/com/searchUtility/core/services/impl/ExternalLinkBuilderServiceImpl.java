package com.searchUtility.core.services.impl;

import com.searchUtility.core.services.ExternalLinkBuilderService;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.Externalizer;

@Component(service = ExternalLinkBuilderService.class)
public class ExternalLinkBuilderServiceImpl implements ExternalLinkBuilderService {
	
	private static final Logger logger = LoggerFactory.getLogger(ExternalLinkBuilderServiceImpl.class);
	
	@Reference
	private ResourceResolverFactory resolverFactory;
	
	@Reference
	private SlingSettingsService slingSettingService;
	
	@Reference
	private Externalizer externalizer;

	@Override
	public String buildExternalLink(String internalPath) {
		
		String externalLink = null;
		
		if(slingSettingService.getRunModes().contains("author")) {
			logger.debug("inside ext if condition");
			externalLink = externalizer.authorLink(resolverFactory.getThreadResourceResolver(), internalPath);
		}
		else {
			logger.debug("inside ext else condition");
			externalLink = externalizer.publishLink(resolverFactory.getThreadResourceResolver(), internalPath);
		}
		
		return externalLink;
	}
	
	

}
