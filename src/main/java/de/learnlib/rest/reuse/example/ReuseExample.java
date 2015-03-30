package de.learnlib.rest.reuse.example;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Random;

import javax.ws.rs.client.WebTarget;

import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.visualization.Visualization;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.Alphabets;

import com.google.common.collect.Sets;

import de.learnlib.algorithms.lstargeneric.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.api.EquivalenceOracle.MealyEquivalenceOracle;
import de.learnlib.api.LearningAlgorithm.MealyLearner;
import de.learnlib.api.MembershipOracle.MealyMembershipOracle;
import de.learnlib.api.Query;
import de.learnlib.api.SUL;
import de.learnlib.eqtests.basic.RandomWordsEQOracle.MealyRandomWordsEQOracle;
import de.learnlib.example.util.RESTContextHandlerImpl;
import de.learnlib.example.util.RESTDataMapper;
import de.learnlib.filters.reuse.ReuseCapableOracle;
import de.learnlib.filters.reuse.ReuseOracle.ReuseOracleBuilder;
import de.learnlib.mapper.ContextExecutableInputSUL;
import de.learnlib.mapper.api.ContextExecutableInput;
import de.learnlib.oracles.CounterOracle;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.oracles.SymbolCounterSUL;
import de.learnlib.statistics.Counter;

/**
 * This example shows one way to use the "Reuse filter" described in the article
 * "Reusing System States by Active Learning Algorithms" 
 * (http://link.springer.com/chapter/10.1007%2F978-3-642-28033-7_6)
 * to use domain-knowledge while learning a RESTful web service.
 * 
 * @author Oliver Bauer
 */
public class ReuseExample {
	public static void main(String[] args) {
		Instant instant = Instant.now();

		final String webResourceURI = "http://localhost:8080/tapestry-resteasy-example/rest/";

		// abstract input symbols for create, read, update, delete operations
		Alphabet<String> alphabet = Alphabets.fromArray("C", "R", "U", "D");

		SUL<ContextExecutableInput<Integer, WebTarget>, Integer> sul = 
				new ContextExecutableInputSUL<>(
						new RESTContextHandlerImpl(webResourceURI));
		// Count symbols @SUL: Those are really executed at the System Under Learning
		SymbolCounterSUL<ContextExecutableInput<Integer, WebTarget>, Integer> symbolsAtSUL = new SymbolCounterSUL<>("@SUL", sul);

		/** 
		 * Here we use instances of RESTDataMapper as system state.
		 * In more complex scenarios one should create an implementation that
		 * is able to store more values to identify the state of a system 
		 * (e.g. store returned entities, session IDs...). 
		 */
		final ReuseCapableOracle<RESTDataMapper, String, String> reuseCapableOracle = new ReuseCapableOracle<RESTDataMapper, String, String>() {
			@Override
			public QueryResult<RESTDataMapper, String> continueQuery(
					Word<String> trace, RESTDataMapper state) {
				symbolsAtSUL.pre(); // new context
				// we reuse the value of ''counter'', so no call ''state.pre();'' (very important!)

				Word<String> output = executeWord(trace, state);

				return new QueryResult<>(output, state);
			}

			@Override
			public QueryResult<RESTDataMapper, String> processQuery(
					Word<String> trace) {

				RESTDataMapper state = new RESTDataMapper();
				state.pre(); // increment counter, this could be some expensive reset
				symbolsAtSUL.pre(); // new context

				Word<String> output = executeWord(trace, state);

				return new QueryResult<>(output, state);
			}

			private Word<String> executeWord(Word<String> trace,
					RESTDataMapper mapper) {
				WordBuilder<String> wb = new WordBuilder<>();
				for (String symbol : trace) {
					Integer co = symbolsAtSUL.step(mapper.mapInput(symbol));
					String ao = mapper.mapOutput(co);
					wb.add(ao);
				}
				return wb.toWord();
			}
		};
		/** Let LearnLib take control.	 */

		// Reuse with domain-knowledge: 
		// * Some HTTP response codes does not change state of the SUL (observable state).
		// * The update-symbol is a invariant input symbol (it changes the database, but
		//   no other symbol will notice it in this experiment)
		MealyMembershipOracle<String, String> mqOracle = new ReuseOracleBuilder<>(
				alphabet, () -> { return reuseCapableOracle; })
				.withFailureOutputs(Sets.newHashSet("401", "403", "404"))
				.withInvariantInputs(Sets.newHashSet("U"))
				.build();

		// Count the symbols the learning algorithm fires. Without filter this would
		// be the number of executed symbols at the System Under Learning.
		CounterOracle<String, Word<String>> symbolsFromLearner = new CounterOracle<String, Word<String>>(mqOracle, "symbols@Learner") {
			private Counter counter = new Counter("@Learner", "symbols");
			
			@Override
			public void processQueries(
					Collection<? extends Query<String, Word<String>>> queries) {
				queries.forEach(q -> { 
						counter.increment(q.getInput().size()); 
					}
				);
				mqOracle.processQueries(queries);
			}
			
			@Override
			public Counter getStatisticalData() {
				return counter;
			}
		};

		MealyLearner<String, String> learner = new ExtensibleLStarMealyBuilder<String, String>()
				.withAlphabet(alphabet)
				.withOracle(symbolsFromLearner)
				.create();

		// We do one round of learning (this means up to first hypothesis model)
		learner.startLearning();
		MealyMachine<?, String, ?, String> hypothesis = learner
				.getHypothesisModel();

		// Imagine we would not use domain-knowledge... 
		MealyEquivalenceOracle<String, String> eqOracle = new MealyRandomWordsEQOracle<>(
				symbolsFromLearner, 
				5, // minLength
				12, // maxLength
				5000, // maxTests
				new Random(1));

		DefaultQuery<String, Word<String>> ce;
		while ((ce = eqOracle.findCounterExample(hypothesis, alphabet)) != null) {
			learner.refineHypothesis(ce);
			hypothesis = learner.getHypothesisModel();
		}

		Instant end = Instant.now();
		Duration duration = Duration.between(instant, end);
		System.err.println("duration " + duration);

		System.err.println("* "+symbolsAtSUL.getStatisticalData());
		System.err.println("* "+symbolsFromLearner.getStatisticalData());
		
		Visualization.visualizeGraph(hypothesis.transitionGraphView(alphabet),
				true);
	}
}
