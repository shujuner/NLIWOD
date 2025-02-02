package org.aksw.qa.dataset_generator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aksw.jena_sparql_api.cache.extra.CacheFrontend;
import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.SparqlServiceReference;
import org.aksw.jena_sparql_api.http.QueryExecutionHttpWrapper;
import org.aksw.qa.commons.datastructure.Entity;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.nlp.nerd.AGDISTIS;
import org.aksw.qa.commons.nlp.nerd.Spotlight;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.riot.WebContent;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.sparql.vocabulary.FOAF;

import org.apache.jena.vocabulary.RDFS;
import org.dllearner.algorithms.qtl.QueryTreeUtils;
import org.dllearner.algorithms.qtl.datastructures.impl.RDFResourceTree;
import org.dllearner.algorithms.qtl.impl.QueryTreeFactory;
import org.dllearner.algorithms.qtl.impl.QueryTreeFactoryBaseInv;
import org.dllearner.algorithms.qtl.operations.lgg.LGGGenerator;
import org.dllearner.algorithms.qtl.operations.lgg.LGGGeneratorSimple;
import org.dllearner.algorithms.qtl.util.StopURIsDBpedia;
import org.dllearner.algorithms.qtl.util.StopURIsOWL;
import org.dllearner.algorithms.qtl.util.StopURIsRDFS;
import org.dllearner.algorithms.qtl.util.StopURIsSKOS;
import org.dllearner.algorithms.qtl.util.filters.NamespaceDropStatementFilter;
import org.dllearner.algorithms.qtl.util.filters.ObjectDropStatementFilter;
import org.dllearner.algorithms.qtl.util.filters.PredicateDropStatementFilter;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.SymmetricConciseBoundedDescriptionGeneratorImpl;
import org.json.simple.parser.ParseException;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lorenz Buehmann
 */
public class DatasetGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatasetGenerator.class);

	private static final String NAMESPACE ="http://dbpedia.org/resource/";
	private static final String NAMESPACE2 ="http://dbpedia.org/property/";
	private static final String CSV_FILE_NAME="evaluation_datasetevaluator.csv";
	private AGDISTIS disambiguator;
	private Spotlight recognizer;

	private ConciseBoundedDescriptionGenerator cbdGen;
	private QueryTreeFactory qtf;
	private LGGGenerator lggGen;

	private QueryExecutionFactory qef;

	List<String[]>evaluation;
	public DatasetGenerator(QueryExecutionFactory qef) {

		this.disambiguator = new AGDISTIS();
		this.recognizer = new Spotlight();

		this.qef=qef;
		// CBD generator
		cbdGen = new ConciseBoundedDescriptionGeneratorImpl(qef);
		cbdGen = new SymmetricConciseBoundedDescriptionGeneratorImpl(qef);

		evaluation=new ArrayList<>();
		// query tree factory
		qtf = new QueryTreeFactoryBaseInv();
		// filters
		ArrayList<Predicate<Statement>> treeFilters = Lists.newArrayList(new PredicateDropStatementFilter(StopURIsDBpedia.get()), new ObjectDropStatementFilter(StopURIsDBpedia.get()),
		        new PredicateDropStatementFilter(Sets.union(StopURIsRDFS.get(), Sets.newHashSet(RDFS.seeAlso.getURI()))), new PredicateDropStatementFilter(StopURIsOWL.get()),
		        new ObjectDropStatementFilter(StopURIsOWL.get()), new PredicateDropStatementFilter(StopURIsSKOS.get()), new ObjectDropStatementFilter(StopURIsSKOS.get()),
		        new NamespaceDropStatementFilter(Sets.newHashSet(
		                // "http://dbpedia.org/property/",
		                "http://purl.org/dc/terms/", "http://dbpedia.org/class/yago/", FOAF.getURI())),
		        new PredicateDropStatementFilter(Sets.newHashSet("http://www.w3.org/2002/07/owl#equivalentClass", "http://www.w3.org/2002/07/owl#disjointWith")));
		qtf.addDropFilters((Predicate<Statement>[]) treeFilters.toArray(new Predicate[treeFilters.size()]));

		// LGG generator
		lggGen = new LGGGeneratorSimple();
	}

	public void generate(Map<String, Set<String>> question2Answers) {

		question2Answers.forEach((question, answers) -> {
			/*if (!question.contains("writer"))
				return;*/
			LOGGER.info("Question:"+question);
			LOGGER.info("Answers:"+answers);
			LOGGER.info("###################################################################################");
			LOGGER.info("processing \"{}\" ...", question);

			// 1. annotate the question
			Map<String, List<Entity>> questionEntities = recognize(question);

			// 2. disambiguate the answers
			Map<String, Optional<String>> answerEntities = disambiguateAnswers(answers);

			// we stop if we could not find entities
			if (answerEntities.isEmpty()) {
				LOGGER.warn("Could not find the answer entities for");
				return;
			}

			// 3. generate SPARQL query

			generateSPARQLQuery(answerEntities, questionEntities,question);
		});

	}

	public void generate(List<IQuestion> questions) {
		generate(questions.stream().collect(Collectors.toMap(q -> q.getLanguageToQuestion().get("en"), q -> q.getGoldenAnswers(), (q1, q2) -> q1)));
	}

	private Map<String, List<Entity>> recognize(String question) {
		LOGGER.info("entity detection in question...");
		Map<String, List<Entity>> entities = recognizer.getEntities(question);
		if (question.contains("Super Bowl 50")) {
			Entity e = new Entity();
			e.setLabel("Super Bowl 50");
			e.getUris().add(new ResourceImpl("http://dbpedia.org/resource/Super_Bowl_50"));
			entities.put("Super Bowl", Lists.newArrayList(e));
		}
		LOGGER.info("entities:{}", entities.entrySet().stream().map(e -> e.toString()).collect(Collectors.joining("\n")));
		return entities;
	}

	private Map<String, Optional<String>> disambiguateAnswers(Set<String> answers) {
		return answers.stream().collect(Collectors.toMap(a -> a, a -> disambiguate(a)));
	}

	private Optional<String> disambiguate(String label) {
		LOGGER.info("NED for {} ...", label);
		String preAnnotatedText = "<entity>" + label + "</entity>";

		try {
			HashMap<String, String> results = disambiguator.runDisambiguation(preAnnotatedText);
			String namedEntity = results.get(label);
			if (namedEntity == null) {
				LOGGER.warn("no entity found for {}", label);
			} else {
				LOGGER.info("{} -> {}", label, namedEntity);
			}
			return Optional.ofNullable(namedEntity);
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void generateSPARQLQuery(Map<String, Optional<String>> entities, Map<String, List<Entity>> questionEntities,String question) {
		Set<Resource> filterResources = questionEntities.values().stream().flatMap(l -> l.stream()).map(e -> e.getUris()).flatMap(u -> u.stream()).collect(Collectors.toSet());
		System.out.println(filterResources);

		Set<Entity> filterEntities = questionEntities.values().stream().flatMap(l -> l.stream()).collect(Collectors.toSet());
		System.out.println(filterEntities);
		List<Set<String>> knownProperties=new ArrayList<>();
		Set<String> commonProperties=new HashSet<>();
		List<Object[]>scippedCBDs=new ArrayList<>();
		// generate query trees
		List<RDFResourceTree> trees = new ArrayList<>(entities.size());

		entities.values().stream().filter(e -> e.isPresent()).map(e -> e.get()).forEach(uri -> {
			LOGGER.info(uri);
			// generate CBD
			Model cbd = cbdGen.getConciseBoundedDescription(uri);
			LOGGER.info("|cbd(" + uri + ")|=" + cbd.size() + " triples");
			//First 100 cbd statements should be enough to represent one single entity
			if(entities.size()==1&&cbd.size()>100){
				StmtIterator it = cbd.listStatements();
				Statement[] statements=new Statement[100];
				for(int i=0;i<100;i++)
					statements[i]=it.nextStatement();
				cbd.removeAll();
				cbd.add(statements);
			}

			Predicate<Statement> isRelevant = st -> {
				// return filterResources.contains(st.getSubject())||
				// filterResources.contains(st.getObject());

				return filterEntities.isEmpty() || filterEntities.stream()
				        .anyMatch(e -> (st.getSubject().toString().toLowerCase().contains(e.getLabel().toLowerCase())) || st.getObject().toString().toLowerCase().contains(e.getLabel().toLowerCase()));
			};
			Predicate<Statement> isDBO = st -> {
				// return filterResources.contains(st.getSubject())||
				// filterResources.contains(st.getObject());
				//http://www.w3.org/1999/02/22-rdf-syntax-ns#type
				//http://www.w3.org/1999/02/22-rdf-syntax-ns#type


				return !"http://www.w3.org/1999/02/22-rdf-syntax-ns#type".equals(st.getPredicate().toString()) || st.getObject().toString().startsWith("http://dbpedia.org/ontology/");
			};

			// filter CBD
			LOGGER.info("|cbd_filtered(" + uri + ")|=" + cbd.size() + " triples");
			cbd.remove(cbd.listStatements().filterDrop(isRelevant).toList());
			LOGGER.info("|cbd_filtered(" + uri + ")|=" + cbd.size() + " triples");
			cbd.remove(cbd.listStatements().filterDrop(isDBO).toList());
			LOGGER.info("|cbd_filtered(" + uri + ")|=" + cbd.size() + " triples");

			// generate query tree

			boolean contains=false;
			NsIterator it=cbd.listNameSpaces();
			while(it.hasNext()&&!contains) {
				String namespace=it.nextNs();
				contains = "http://dbpedia.org/property/".equals(namespace) || "http://dbpedia.org/ontology/".equals(namespace);
			}


			if(contains) {
				Set<String>properties=new HashSet<>();
				cbd.listStatements().forEachRemaining(s -> properties.add(s.getPredicate().toString()));
				knownProperties.add(properties);
				if(cbd.size()>1000){
					scippedCBDs.add(new Object[]{uri,cbd});
				}else {
					RDFResourceTree tree = qtf.getQueryTree(uri, cbd);
					trees.add(tree);
					LOGGER.info(tree.getStringRepresentation(true));
				}
			}
		});

		if(knownProperties.size()>0) {
			commonProperties.addAll(knownProperties.get(0));
			knownProperties.remove(0);

			for (Set properties : knownProperties) {
				commonProperties=(Sets.intersection(commonProperties, properties));
			}
		}
		Set p=new HashSet<>();
		p.addAll(commonProperties);
		scippedCBDs.forEach(skipped->{
			Predicate<Statement> isknownProperty = st -> {
				// return filterResources.contains(st.getSubject())||
				// filterResources.contains(st.getObject());
				//http://www.w3.org/1999/02/22-rdf-syntax-ns#type
				//http://www.w3.org/1999/02/22-rdf-syntax-ns#type

				return p.contains(st.getPredicate().toString());
			};
			String uri=(String)skipped[0];
			Model cbd= (Model) skipped[1];
			LOGGER.info("|cbd_filtered|=" + cbd.size() + " triples");
			cbd.remove(cbd.listStatements().filterDrop(isknownProperty).toList());
			LOGGER.info("|cbd_filtered|=" + cbd.size() + " triples");
			if(cbd.size()<1000) {
				RDFResourceTree tree = qtf.getQueryTree(uri, cbd);
				trees.add(tree);
				LOGGER.info(tree.getStringRepresentation(true));
			}else{
				LOGGER.error("CBD for "+uri+" to large the Tree g eneration was skipped");
			}
		});

		// compute LGG
		if(trees.size()>0) {
			RDFResourceTree lgg = lggGen.getLGG(trees);

			// SPARQL query
			Query query = QueryTreeUtils.toSPARQLQuery(lgg);

			// 5) run best QTL against DBpedia and measure
			// f-measure/accuracy to answer
			System.out.println(query);
			evaluate(entities.keySet(), query, question);
		}

		else{
			LOGGER.debug("No tree found for question: "+question);
			evaluation.add(new String[]{question,"0.0","0.0","0.0","0.0","0.0","0.0","0.0"});
		}
	}
	private void evaluate(Set<String>answers,Query query,String question){

		QueryExecution qe = qef.createQueryExecution(query);

		LOGGER.debug(query.toString());
		LOGGER.debug("Question:"+ question);

		double truepositive=0;
		double falsepositive=0;
		try {
			ResultSet rs = qe.execSelect();
			while (rs.hasNext()) {
				QuerySolution qs = rs.next();

				RDFNode node = qs.get("s");

				if (node != null && node.isResource()) {
					String resourceString = (node.asResource().toString().replace(NAMESPACE, ""));
					resourceString = resourceString.replace(NAMESPACE2, "");
					if (answers.contains(resourceString)) {
						truepositive++;
						//LOGGER.debug("truepositive: " + resourceString);
					} else {
						falsepositive++;
						//LOGGER.debug("falsepositive: " + resourceString);
					}
				}
			}
		}catch(Exception e){
			LOGGER.error("Executing SPARQL Query"+query.toString()+"failed");
		}
		if(truepositive>0||falsepositive>0) {
			double falsenegative = answers.size() - truepositive;
			double recall = truepositive / (truepositive + falsepositive);
			double precision = truepositive / (truepositive + falsenegative);
			double accuracy = truepositive / (truepositive + falsenegative + falsepositive);
			double f1 = 2 * (precision * recall) / (precision + recall);
			LOGGER.debug("Evaluation: tp:" + truepositive + " fp:" + falsepositive + " fn:" + falsenegative + " recall:" + recall + " precision:" + precision + " f1:" + f1 + " accuracy:" + accuracy);
			evaluation.add(new String[]{question, truepositive + "", falsepositive + ""
					, falsenegative + "", recall + "", precision + "", f1 + "", accuracy + ""});
		}
		else{
			LOGGER.debug("No results for question: "+question);
			evaluation.add(new String[]{question,"0.0","0.0","0.0","0.0","0.0","0.0","0.0"});
		}
	}
	// used to get up-to-date answers for a SPARQL query instead of the
	// hard-code and probably out-dated list
	// of resources in the benchmark data
	private static void updateGoldenAnswers(QueryExecutionFactory qef, IQuestion q) {
		Set<String> uris = new HashSet<>();
		if (null != q && null != q.getSparqlQuery() && !q.getSparqlQuery().contains("ASK")) {
			try (QueryExecution qe = qef.createQueryExecution(q.getSparqlQuery())) {
				ResultSet rs = qe.execSelect();
				while (rs.hasNext()) {
					QuerySolution qs = rs.next();

					RDFNode node = qs.get("uri");

					if (node != null && node.isResource()) {
						uris.add(node.asResource().getURI());
					}
				}
			}
			q.setGoldenAnswers(uris);
		} else {// TODO what happens if q is null?
		}

	}
	private String convertToCSV(String[] data) {
		return Stream.of(data)
				.collect(Collectors.joining(","));
	}
	private void writeCSVEvaluation() throws IOException {
		File csvOutputFile = new File(CSV_FILE_NAME);
		try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
			pw.println("Question,truepositive,falsepositive,falsenegative,recall,precision,f1,accuracy");
			evaluation.stream()
					.map(this::convertToCSV)
					.forEach(pw::println);
		}
	}
	public static void main(String[] args) throws IOException {
		// DBpedia as SPARQL endpoint
		long timeToLive = TimeUnit.DAYS.toMillis(30);
		CacheFrontend cacheFrontend = CacheUtilsH2.createCacheFrontend("/tmp/qald/sparql", true, timeToLive);
		  final QueryExecutionFactory qef  = FluentQueryExecutionFactory
				                                 .http("http://dbpedia.org/sparql", Lists.newArrayList("http://dbpedia.org"))
				                                 .config().withPostProcessor(qe -> ((QueryEngineHTTP) ((QueryExecutionHttpWrapper) qe).getDecoratee())
				                                                 .setModelContentType(WebContent.contentTypeRDFXML))
				                                 .withCache(cacheFrontend)
				                                 .end()
				                                 .create();
		List<IQuestion> questions = LoaderController.load(Dataset.QALD9_Train_Multilingual);
		// List<IQuestion> questions =
		// LoaderController.load(Dataset.QALD6_Train_Multilingual);
		questions.stream().filter(q -> q.getAnswerType().equals("resource")).collect(Collectors.toList());
		questions.forEach(q -> updateGoldenAnswers(qef, q));

		IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
		questions.forEach(q -> q.setGoldenAnswers(q.getGoldenAnswers().stream().map(a -> sfp.getShortForm(IRI.create(a))).collect(Collectors.toSet())));
		// questions = questions.stream()
		// .filter(q -> q.getLanguageToQuestion().get("en").equals("Where did
		// Super Bowl 50 take place?"))
		// .collect(Collectors.toList());

		DatasetGenerator stan = new DatasetGenerator(qef);
		stan.generate(questions);
		stan.writeCSVEvaluation();
	}
}
