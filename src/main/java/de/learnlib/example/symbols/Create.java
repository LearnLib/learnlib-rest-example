package de.learnlib.example.symbols;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.learnlib.api.SULException;
import de.learnlib.mapper.api.ContextExecutableInput;

public class Create implements ContextExecutableInput<Integer, WebTarget> {
	private int id;

	public Create(int id) {
		this.id = id;
	}

	@Override
	public Integer execute(WebTarget resource) throws SULException, Exception {
		Response response = null;
		String json = "{\"name\":\"firstname\",\"mail\":\"mail" + id
				+ "\",\"password\":\"123\"}";

		response = resource.path("user/create")
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.json(json));

		return response.getStatus();
	}
}
