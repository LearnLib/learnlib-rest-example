package de.learnlib.example;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import javax.ws.rs.client.WebTarget;

import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.commons.dotutil.DOT;
import net.automatalib.util.graphs.dot.GraphDOT;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import de.learnlib.algorithms.lstargeneric.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.api.EquivalenceOracle.MealyEquivalenceOracle;
import de.learnlib.api.LearningAlgorithm.MealyLearner;
import de.learnlib.api.SUL;
import de.learnlib.cache.mealy.MealyCacheOracle;
import de.learnlib.eqtests.basic.RandomWordsEQOracle.MealyRandomWordsEQOracle;
import de.learnlib.example.util.RESTContextHandlerImpl;
import de.learnlib.example.util.RESTDataMapper;
import de.learnlib.mapper.ContextExecutableInputSUL;
import de.learnlib.mapper.Mappers;
import de.learnlib.mapper.api.ContextExecutableInput;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.oracles.SULOracle;

public class Example1 {
	public static void main(String[] args) {
		Instant instant = Instant.now();
		
		/** Main configuration to learn a REST application. */
		// Main URI that contains REST resources:
		// The application is available here: https://github.com/oliverbauer/tapestry-resteasy-hibernate
		final String webResourceURI = "http://localhost:8080/tapestry-resteasy-example/rest/";
		// An alphabet containing four symbols for Create/Read/Update/Delete
		// (Please note that the RESTDataMapper must provide a mapping to concrete symbols):
		Alphabet<String >alphabet = Alphabets.fromArray("C","R","U","D");
		// A ContextHandler provides a JAX-RS WebTarget which will be used by the concrete symbols
		final RESTContextHandlerImpl contextHandler = new RESTContextHandlerImpl(webResourceURI);
		// A ContextExecutableInputSUL will work with
		// -concrete input symbols of type ContextExecutableInput (see symbol implementations)
		// -concrete output symbols of type Integer (HTTP status code)
		// -and with context WebTarget
		final ContextExecutableInputSUL<ContextExecutableInput<Integer, WebTarget>, Integer, WebTarget> 
		ceiSUL = new ContextExecutableInputSUL<>(contextHandler);
		/** From this LOC we let LearnLib take control.	 */

		// We create a SUL working with abstract inputs outputs
		SUL<String, String> sul = Mappers.apply(new RESTDataMapper(), ceiSUL);
		SULOracle<String, String> oracle = new SULOracle<>(sul);
		
		MealyCacheOracle<String, String> mqOracle = MealyCacheOracle.createDAGCacheOracle(alphabet, null, oracle);
		
		MealyLearner<String, String> learner = new ExtensibleLStarMealyBuilder<String, String>()
				.withAlphabet(alphabet)
				.withOracle(mqOracle)
				.create();
		
		// We do one round of learning (this means up to first hypothesis model)
		learner.startLearning();
		MealyMachine<?, String, ?, String> hypothesis = learner.getHypothesisModel();

		MealyEquivalenceOracle<String, String> eqOracle = new MealyRandomWordsEQOracle<>(
				mqOracle, 
				1, // minLength
				4, //maxLength
				50, // maxTests
				new Random(1));
		
		DefaultQuery<String, Word<String>> ce;
		while ((ce = eqOracle.findCounterExample(hypothesis, alphabet)) != null) {
			System.err.println("Found counterexample "+ce);
			System.err.println("Current hypothesis has "+hypothesis.getStates().size()+" states");

			learner.refineHypothesis(ce);
			hypothesis = learner.getHypothesisModel();
		}
		System.err.println("Final hypothesis has "+hypothesis.getStates().size()+" states");
		
		
		Instant end = Instant.now();
		Duration duration = Duration.between(instant, end);
		System.err.println("duration "+duration);
		
		// if graphviz is not installed we just capture the exception
		try {
			Appendable sb = new StringBuffer();
			GraphDOT.write(hypothesis, alphabet, sb);
			StringReader sr = new StringReader(sb.toString());
			DOT.renderDOT(sr, true);
		} catch (IOException e) {
			System.err.println("Unable to render hypothesis "+e);
		}
	}
}
