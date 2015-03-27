package de.learnlib.example.symbols;

import java.io.InputStream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import de.learnlib.api.SULException;
import de.learnlib.mapper.api.ContextExecutableInput;

public class Delete implements ContextExecutableInput<Integer, WebTarget> {
	private int id;

	public Delete(int id) {
		this.id = id;
	}

	@Override
	public Integer execute(WebTarget resource) throws SULException, Exception {
		Response response = null;

		response = resource
				.path("user/read/mail" + String.valueOf(id))
				.request(MediaType.APPLICATION_JSON_TYPE)
				.get();

		String json = IOUtils.toString((InputStream) response.getEntity(),
				"UTF-8");

		/*
		 * Otherwise:
		 * 
		 * [WARN] core.ExceptionHandler Failed executing POST /user/delete
		 * org.jboss.resteasy.spi.ReaderException: java.io.EOFException: No
		 * content to map to Object due to end of input .. Caused by:
		 * java.io.EOFException: No content to map to Object due to end of input
		 * at
		 * org.codehaus.jackson.map.ObjectMapper._initForReading(ObjectMapper.
		 * java:2775) ...
		 * 
		 * which cannot be handeled in the REST service definition.
		 */
		if (!json.contains("mail" + String.valueOf(id))) {
			return 404;
		}

		response = resource.path("user/delete")
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.json(json));

		return response.getStatus();
	}
}
