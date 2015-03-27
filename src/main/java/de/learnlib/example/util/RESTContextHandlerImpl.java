package de.learnlib.example.util;

import javax.ws.rs.client.WebTarget;

import de.learnlib.mapper.ContextExecutableInputSUL.ContextHandler;

public class RESTContextHandlerImpl implements ContextHandler<WebTarget> {
	private WebTargetBuilder wtb = null;
	private String webResourceURI;
	
	public RESTContextHandlerImpl(String webResourceURI) {
		this.webResourceURI = webResourceURI;
	}

	/** {@inheritDoc} */
	@Override
	public WebTarget createContext() {
		if (wtb == null) {
			wtb = new WebTargetBuilder(webResourceURI);
		}
		return wtb.build();
	}

	/** {@inheritDoc} */
	@Override
	public void disposeContext(WebTarget context) {
		
	}
}
