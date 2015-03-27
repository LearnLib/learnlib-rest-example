package de.learnlib.example.symbols;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.learnlib.api.SULException;
import de.learnlib.mapper.api.ContextExecutableInput;

public class Read implements ContextExecutableInput<Integer, WebTarget> {
	private Integer id;

	public Read(Integer id) {
		this.id = id;
	}

	@Override
	public Integer execute(WebTarget resource) throws SULException, Exception {
		Response response = null;

		// the mails are set to "mail${counter}" in create
		response = resource
				.path("user/read/mail" + String.valueOf(id))
				.request(MediaType.APPLICATION_JSON_TYPE)
				.get();

		// e.g. 204 or 200
		return response.getStatus();
	}
}
