package de.learnlib.example.util;

import javax.ws.rs.client.WebTarget;

import de.learnlib.api.SULException;
import de.learnlib.example.symbols.Create;
import de.learnlib.example.symbols.Delete;
import de.learnlib.example.symbols.Read;
import de.learnlib.example.symbols.Update;
import de.learnlib.mapper.api.ContextExecutableInput;
import de.learnlib.mapper.api.Mapper;

public class RESTDataMapper
		implements
		Mapper<String, String, ContextExecutableInput<Integer, WebTarget>, Integer> {

	private int counter = 0;

	/** {@inheritDoc} */
	@Override
	public void pre() {
		// we don't reset explicitly, here we use a counter
		++counter;
	}

	/** {@inheritDoc} */
	@Override
	public void post() {
		// Nothing to do. See ContextHandler.createContext() in RunningExample.
	}

	/** {@inheritDoc} */
	@Override
	public String mapOutput(Integer concreteOutput) {
		return String.valueOf(concreteOutput);
	}

	/** {@inheritDoc} */
	@Override
	public de.learnlib.mapper.api.Mapper.MappedException<? extends String> mapWrappedException(
			SULException exception) throws SULException {
		return MappedException.ignoreAndContinue("error");
	}

	/** {@inheritDoc} */
	@Override
	public de.learnlib.mapper.api.Mapper.MappedException<? extends String> mapUnwrappedException(
			RuntimeException exception) throws SULException, RuntimeException {
		return MappedException.ignoreAndContinue("error");
	}

	/** {@inheritDoc} */
	@Override
	public ContextExecutableInput<Integer, WebTarget> mapInput(
			String abstractInput) {
		switch (abstractInput) {
		case "C":
			return new Create(counter);
		case "R":
			return new Read(counter);
		case "U":
			return new Update(counter);
		case "D":
			return new Delete(counter);
		default:
			throw new RuntimeException("Unknown abstract input symbol: "+abstractInput);
		}
	}
}
