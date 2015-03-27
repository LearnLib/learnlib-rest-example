package de.learnlib.example.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.json.stream.JsonGenerator;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

public class WebTargetBuilder {
	private Map<String, Object> properties;
	private Set<Class<?>> registeredProviderClasses;
	private String webResourceURI;
	
	public WebTargetBuilder(String webResourceURI) {
		this.webResourceURI = webResourceURI;
		
		properties = new HashMap<>();
		properties.put(JsonGenerator.PRETTY_PRINTING, false);
		properties.put(ClientProperties.CONNECT_TIMEOUT, 10000);
		properties.put(ClientProperties.READ_TIMEOUT, 8000);
		
		registeredProviderClasses = new HashSet<>();
		registeredProviderClasses.add(JsonProcessingFeature.class);
		registeredProviderClasses.add(MultiPartFeature.class);
		registeredProviderClasses.add(JacksonFeature.class);
	}
	
	public WebTargetBuilder withProvider(Class<?> providerClass) {
		registeredProviderClasses.add(providerClass);
		return this;
	}
	
	public WebTargetBuilder withReadTimeout(int ms) {
		properties.put(ClientProperties.READ_TIMEOUT, ms);
		return this;
	}
	
	public WebTargetBuilder withConnectTimeout(int ms) {
		properties.put(ClientProperties.CONNECT_TIMEOUT, ms);
		return this;
	}
	
	public WebTargetBuilder withProperty(String name, Object value) {
		properties.put(name, value);
		return this;
	}
	
	public WebTarget build() {
		ClientConfig clientConfig = new ClientConfig();
		for (Class<?> providerClass : registeredProviderClasses) {
			clientConfig.register(providerClass);
		}
		for (Entry<String, Object> entry : properties.entrySet()) {
			clientConfig.property(entry.getKey(), entry.getValue());
		}
		
		Client context = ClientBuilder.newClient(clientConfig);
	
		return context.target(webResourceURI);
	}
}
