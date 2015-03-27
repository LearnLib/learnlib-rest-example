package de.learnlib.example.symbols;

import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.learnlib.api.SULException;
import de.learnlib.mapper.api.ContextExecutableInput;

public class Update implements ContextExecutableInput<Integer, WebTarget> {
	private Integer id;

	public Update(Integer id) {
		this.id = id;
	}

	@Override
	public Integer execute(WebTarget resource) throws SULException, Exception {
		Response response = resource
				.path("user/read/mail" + String.valueOf(id))
				.request(MediaType.APPLICATION_JSON_TYPE)
				.get();

		if (response.getStatus() != 200) {
			// 404 not found?
			return response.getStatus();
		}

		JsonObject updatedUser = null;
		try (JsonReader jsonReader = Json.createReader((InputStream) response
				.getEntity())) {
			// json looks like
			// {"id":13,"mail":"mail21","name":"firstname","password":"123","roles":[]}

			// copy to new json while changing the password
			JsonObject object = jsonReader.readObject();
			JsonObjectBuilder builder = Json.createObjectBuilder();
			builder.add("id", object.getInt("id"));
			builder.add("mail", object.getString("mail"));
			builder.add("name", object.getString("name"));
			builder.add("password", "456");
			// ignore JSonArray roles

			updatedUser = builder.build();
		}

		response = resource.path("user/update")
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.json(updatedUser));

		return response.getStatus();
	}
}
