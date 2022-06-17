package com.searchUtility.core.services.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import com.searchUtility.core.services.ReadWriteService;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(service = ReadWriteService.class)

public class ReadWriteServiceImpl implements ReadWriteService {

	@Reference
	ResourceResolverFactory resourceResolverFactory;

	/**
	 * This method returns a ResourceResolver object with access rights equivalent
	 * to that of system user readService. This system user has only read access.
	 */
	public ResourceResolver getReadService() throws LoginException {
		Map<String, Object> authenticationInfo = new HashMap<>();
		authenticationInfo.put(ResourceResolverFactory.SUBSERVICE, "readService");
		return resourceResolverFactory.getServiceResourceResolver(authenticationInfo);
	}

	/**
	 * This method returns a ResourceResolver object with access rights equivalent
	 * to that of system user writeService. This system user has both read and write
	 * access.
	 */
	public ResourceResolver getWriteService() throws LoginException {
		Map<String, Object> authenticationInfo = new HashMap<>();
		authenticationInfo.put(ResourceResolverFactory.SUBSERVICE, "writeService");
		return resourceResolverFactory.getServiceResourceResolver(authenticationInfo);
	}

	@Override
	public void closeSession(Session session) {
		if (session != null && session.isLive()) {
			session.logout();
		}
	}

}
