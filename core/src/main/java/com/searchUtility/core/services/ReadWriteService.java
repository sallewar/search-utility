package com.searchUtility.core.services;

import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

public interface ReadWriteService {
	public ResourceResolver getReadService() throws LoginException;

	public ResourceResolver getWriteService() throws LoginException;

	/**
	 * This method is used for closing the JCR session.
	 * 
	 * @param session
	 */
	public void closeSession(Session session);

}
