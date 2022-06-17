package com.searchUtility.core.utils.reportsgeneration;

import java.io.InputStream;

import com.searchUtility.core.constants.CommonConstants;
import com.searchUtility.core.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;


public class SavetoDam {

	private static final Logger LOGGER = LoggerFactory.getLogger(SavetoDam.class);
	private static final String APPLICATION_TYPE = "application/vnd.ms-excel";
	private static final String EXTENTION = ".xlsx";
	
	public int writeToDam(InputStream is, String filePath, String fileName, ResourceResolver resolver) {
		int creationStatus = 0;
		if (StringUtils.isNotBlank(filePath) && StringUtils.isNotBlank(fileName)) {
			String logFileName = fileName.concat(CommonConstants.HYPHEN).concat(Utils.getTimeStamp())
					.concat(EXTENTION);
			AssetManager assetMgr = resolver.adaptTo(com.day.cq.dam.api.AssetManager.class);
			if (assetMgr != null) {
				LOGGER.debug("Creating file name = {} under path = {}", logFileName, filePath);
				Asset asset = assetMgr.createAsset(filePath + CommonConstants.SLASH + logFileName, is,
						APPLICATION_TYPE, true);
				if (asset != null) {
					creationStatus = 1;
					LOGGER.debug("Asset created = {}", asset.getName());
				} else {
					LOGGER.error("Error during asset creation");
				}
			}
		}
		return creationStatus;
	}

	
}
