package com.searchUtility.core.services.impl;

import com.searchUtility.core.services.SearchToolConfig;
import com.searchUtility.core.services.SearchToolService;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

@Component(service = SearchToolService.class,configurationPolicy=ConfigurationPolicy.REQUIRE)
@Designate(ocd = SearchToolConfig.class)
public class SearchToolConfigImpl implements SearchToolService {

    private SearchToolConfig config;

    private boolean author;

    @Reference
    private SlingSettingsService settings;

    @Activate
    public void activate(SearchToolConfig config) {
        this.config = config;
        author = settings.getRunModes().contains("author");
    }

   
    public boolean isAuthor() {
        return author;
    }

	@Override
	public String getComponentPath() {
		return config.componentPath();
	}

	@Override
	public String getFallbackContentPath() {
		return config.fallbackContentPath();
	}

	@Override
	public String getFallbackDamPath() {
		return config.fallbackDamPath();
	}

	@Override
	public String getDamFolderLocation() {
		return config.damFolderLocation();
	}
}
