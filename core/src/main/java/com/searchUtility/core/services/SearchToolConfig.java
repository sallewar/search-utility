package com.searchUtility.core.services;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Search utility Service Configuration", description = "Service Configuration")
public @interface SearchToolConfig {

    @AttributeDefinition(name = "Component Path", description = "Enter path to your project components, eg /apps/mywebsite/components/content")
    String componentPath();
    
    @AttributeDefinition(name = "Fallback Content Path", description = "Enter Default Search Path to be considered when search path not provided as input, eg /content/mywebsite")
	String fallbackContentPath();
    
    @AttributeDefinition(name = "Fallback DAM path", description = "Enter Default Search Path to be considered when DAM search path not provided as input, eg /content/dam/mywebsite")
    String fallbackDamPath();
    
    @AttributeDefinition(name = "DAM Folder Location", description = "Enter the DAM path under which search reports should get created, eg /content/dam/mywebsite/reports")
    String damFolderLocation();
   
}
