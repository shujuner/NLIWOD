package org.aksw.qa.commons.load;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.datastructure.Question;
import org.aksw.qa.commons.load.json.EJQuestionFactory;
import org.aksw.qa.commons.load.json.ExtendedQALDJSONLoader;
import org.aksw.qa.commons.load.json.QaldJson;
import org.aksw.qa.commons.load.stanford.StanfordLoader;
import org.aksw.qa.commons.load.tsv.LoadTsv;
import org.aksw.qa.commons.sparql.AnswerSyncer;
import org.aksw.qa.commons.utils.DateFormatter;
import org.aksw.qa.commons.utils.SPARQLExecutor;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Strings;



/**
 * Loads both QALD XML and JSON
 *
 * @author ricardousbeck tortugaattack jonathanhuthmann 
 */

// TODO refactor that class to account for multiple dataset types. make qaldxml,
// qaldjson independent of this class so it becomes easiert to load a new class
public class LoaderController {
	private static Logger log = LoggerFactory.getLogger(LoaderController.class);

	private static InputStream getInputStream(final Dataset set) {
		// Magical get the path from qa-datasets

		try {

			InputStream url = mapDatasetToPath(set);
			return url;
		} catch (NullPointerException e) {
			log.error("No Mapping for this Dataset " + set.toString(), e);
		}
		return null;
	}

	public static Class<?> getLoadingAnchor() {
		Class<?> loadingAnchor = null;
		try {
			loadingAnchor = Class.forName("org.aksw.qa.datasets.ResourceLoadingAnchor");
		} catch (ClassNotFoundException e) {
			log.error("Couldn't find the org.aksw.qa.datasets.ResourceLoadingAnchor class necessary to load the datasets. Returning null.", e);
			return null;
		}
		return loadingAnchor;
	}

	public static InputStream mapDatasetToPath(final Dataset set) {
		Class<?> loadingAnchor = getLoadingAnchor();

		switch (set) {
		case nlq:
			return loadingAnchor.getResourceAsStream("/NLQ-OKBQA/nlq1_vis.json");

		case QALD1_Test_dbpedia:
			return loadingAnchor.getResourceAsStream("/QALD-master/1/data/dbpedia-test.xml");
		case QALD1_Test_musicbrainz:
			return loadingAnchor.getResourceAsStream("/QALD-master/1/data/musicbrainz-test.xml");
		case QALD1_Train_dbpedia:
			return loadingAnchor.getResourceAsStream("/QALD-master/1/data/dbpedia-train.xml");
		case QALD1_Train_musicbrainz:
			return loadingAnchor.getResourceAsStream("/QALD-master/1/data/musicbrainz-train.xml");

		case QALD2_Test_dbpedia:
			return loadingAnchor.getResourceAsStream("/QALD-master/2/data/dbpedia-test.xml");
		case QALD2_Test_musicbrainz:
			return loadingAnchor.getResourceAsStream("/QALD-master/2/data/musicbrainz-test.xml");
		case QALD2_Train_dbpedia:
			return loadingAnchor.getResourceAsStream("/QALD-master/2/data/dbpedia-train-answers.xml");
		case QALD2_Train_musicbrainz:
			return loadingAnchor.getResourceAsStream("/QALD-master/2/data/musicbrainz-train-answers.xml");
		case QALD2_Participants_Challenge:
			return loadingAnchor.getResourceAsStream("/QALD-master/2/data/participants-challenge-answers.xml");

		case QALD3_Test_dbpedia:
			return loadingAnchor.getResourceAsStream("/QALD-master/3/data/dbpedia-test-answers.xml");
		case QALD3_Test_esdbpedia:
			return loadingAnchor.getResourceAsStream("/QALD-master/3/data/esdbpedia-test-answers.xml");
		// case QALD3_Test_esdbpedia_sparql:
		// return
		// loadingAnchor.getResourceAsStream("/QALD-master/3/data/esdbpedia-test.xml");
		case QALD3_Test_musicbrainz:
			return loadingAnchor.getResourceAsStream("/QALD-master/3/data/musicbrainz-test-answers.xml");
		case QALD3_Train_dbpedia:
			return loadingAnchor.getResourceAsStream("/QALD-master/3/data/dbpedia-train-answers.xml");
		case QALD3_Train_esdbpedia:
			return loadingAnchor.getResourceAsStream("/QALD-master/3/data/esdbpedia-train-answers.xml");
		case QALD3_Train_musicbrainz:
			return loadingAnchor.getResourceAsStream("/QALD-master/3/data/musicbrainz-train-answers.xml");

		case QALD4_Test_Hybrid:
			return loadingAnchor.getResourceAsStream("/QALD-master/4/data/qald-4_hybrid_test_withanswers.xml");
		case QALD4_Test_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/4/data/qald-4_multilingual_test_withanswers.xml");
		case QALD4_Test_biomedical:
			return loadingAnchor.getResourceAsStream("/QALD-master/4/data/qald-4_biomedical_test_withanswers.xml");
		case QALD4_Train_Hybrid:
			return loadingAnchor.getResourceAsStream("/QALD-master/4/data/qald-4_hybrid_train.xml");
		case QALD4_Train_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/4/data/qald-4_multilingual_train_withanswers.xml");
		case QALD4_Train_biomedical:
			return loadingAnchor.getResourceAsStream("/QALD-master/4/data/qald-4_biomedical_train_withanswers.xml");

		case QALD5_Test_Hybrid:
		case QALD5_Test_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/5/data/qald-5_test.xml");
		case QALD5_Train_Hybrid:
		case QALD5_Train_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/5/data/qald-5_train.xml");

		case QALD6_Train_Hybrid:
			return loadingAnchor.getResourceAsStream("/QALD-master/6/data/qald-6-train-hybrid.json");
		case QALD6_Train_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/6/data/qald-6-train-multilingual.json");

		case QALD6_Test_Hybrid:
			return loadingAnchor.getResourceAsStream("/QALD-master/6/data/qald-6-test-hybrid.json");
		case QALD6_Test_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/6/data/qald-6-test-multilingual.json");

		case QALD7_Train_Hybrid:
			return loadingAnchor.getResourceAsStream("/QALD-master/7/data/qald-7-train-hybrid.json");
		case QALD7_Train_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/7/data/qald-7-train-multilingual.json");
		case QALD7_Train_Wikidata_en:
			return loadingAnchor.getResourceAsStream("/QALD-master/7/data/qald-7-train-en-wikidata.json");
		case QALD7_Test_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/7/data/qald-7-test-multilingual.json");
		case QALD7_Test_Wikidata_en:
			return loadingAnchor.getResourceAsStream("/QALD-master/7/data/qald-7-test-en-wikidata.json");
		case Stanford_dev:
			return loadingAnchor.getResourceAsStream("/stanfordqa-dev.json");
		case Stanford_train:
			return loadingAnchor.getResourceAsStream("/stanfordqa-train.json");
		case QALD8_Test_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/8/data/qald-8-test-multilingual.json");
		case QALD8_Train_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/8/data/qald-8-train-multilingual.json");
		case QALD9_Train_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/9/data/qald-9-train-multilingual.json");
		case QALD9_Test_Multilingual:
			return loadingAnchor.getResourceAsStream("/QALD-master/9/data/qald-9-test-multilingual.json");
		case LCQUAD:
			return loadingAnchor.getResourceAsStream("/lcquad_qaldformat.json");
			// The cases SemSearch, INEX, QALD2,TREC_Entity belong to DBpedia Entity V2
		case SemSearch:
		case INEX:
		case QALD2:
		case TREC_Entity:
		   return loadingAnchor.getResourceAsStream("/queries-v2.txt");
		case Simple_Question_Wikidata:
			return loadingAnchor.getResourceAsStream("/annotated_wd_data_valid_full.json");
		case Wdaqua_Core0_Logs:
			return loadingAnchor.getResourceAsStream("/webservice_25_06_2017.json");
		// case qbench1:
		// return
		// ClassLoader.getSystemClassLoader().getResourceAsStream("qbench/qbench1.xml");
		// case qbench2:
		// return
		// ClassLoader.getSystemClassLoader().getResourceAsStream("qbench/qbench2.xml");
		// case stonetemple:
		// return
		// ClassLoader.getSystemClassLoader().getResourceAsStream("stonetemple/stonetemple");
		// FIXME datacube und qbench sollte gleich sein?!Konrad Höffner
		// Fragen
		// case QALD6_Train_Datacube:
		// return
		// ClassLoader.getSystemClassLoader().getResourceAsStream("QALD-6/qald-6-train-datacube.json");

		default:
			break;
		}
		return null;
	}

	public static List<IQuestion> load(final Dataset data) {
		return load(data, null, "en");
	}

	/**
	 * Use this to load answers from server:{@link AnswerSyncer}
	 */
	@Deprecated //TODO refactor this so that answersyncing is no longer in this class
	public static List<IQuestion> load(final Dataset data, final String deriveUri, final String questionLang) {
		try {
			InputStream is = null;
			is = getInputStream(data);
			if (is == null) {
				log.error("Couldn't load dataset " + data.name() + ". Returning null.");
				return null;
			}
			
			List<IQuestion> out = null;
			if (is.available() > 0) // check if stream is not empty
			{

				List<IQuestion> hybrid;
				List<IQuestion> loadedQ;
				switch (data) {

				case QALD1_Test_dbpedia:
				case QALD1_Test_musicbrainz:
				case QALD1_Train_dbpedia:
				case QALD1_Train_musicbrainz:
				case QALD2_Test_dbpedia:
				case QALD2_Test_musicbrainz:
				case QALD2_Train_dbpedia:
				case QALD2_Train_musicbrainz:
				case QALD2_Participants_Challenge:
				case QALD3_Test_dbpedia:
				case QALD3_Test_musicbrainz:
				case QALD3_Train_dbpedia:
				case QALD3_Train_esdbpedia:
				case QALD3_Train_musicbrainz:
				case QALD4_Test_Hybrid:
				case QALD4_Test_Multilingual:
				case QALD4_Test_biomedical:
				case QALD4_Train_Hybrid:
				case QALD4_Train_Multilingual:
				case QALD4_Train_biomedical:
					out = loadXML(is, deriveUri, questionLang);
					break;
				// this is necessary because sparql and answers are spread over
				// two files.
				case QALD3_Test_esdbpedia:
					is.close();
					out = qald3TestEsdbpediaLoader(deriveUri, questionLang);
					break;

				case QALD5_Test_Hybrid:
				case QALD5_Train_Hybrid:
					hybrid = new ArrayList<>();
					loadedQ = loadXML(is, deriveUri, questionLang);
					for (IQuestion q : loadedQ) {
						if (q.getHybrid()) {
							hybrid.add(q);
						}
					}
					out = hybrid;
					break;

				case QALD5_Test_Multilingual:
				case QALD5_Train_Multilingual:
					hybrid = new ArrayList<>();
					loadedQ = loadXML(is, deriveUri, questionLang);
					for (IQuestion q : loadedQ) {
						if (!q.getHybrid()) {
							hybrid.add(q);
						}
					}
					out = hybrid;
					break;
				case QALD6_Test_Hybrid:
				case QALD6_Test_Multilingual:
				case QALD6_Train_Hybrid:
				case QALD6_Train_Multilingual:
				case QALD7_Train_Hybrid:
				case QALD7_Train_Multilingual:
				case QALD7_Train_Wikidata_en:
				case QALD7_Test_Multilingual:
				case QALD7_Test_Wikidata_en:
				case QALD8_Test_Multilingual:
				case QALD8_Train_Multilingual:
				case Simple_Question_Wikidata:
				case Wdaqua_Core0_Logs:
				case QALD9_Train_Multilingual:
				case QALD9_Test_Multilingual:
				case LCQUAD :
					QaldJson json = (QaldJson) ExtendedQALDJSONLoader.readJson(getInputStream(data), QaldJson.class);
					out = EJQuestionFactory.getQuestionsFromQaldJson(json);
					for (IQuestion q : out) {
						HashSet<String> set = new HashSet<>();
						if ((deriveUri != null) && (q.getSparqlQuery() != null)) {

							Set<RDFNode> answers = SPARQLExecutor.sparql(deriveUri, q.getSparqlQuery());
							for (RDFNode answ : answers) {
								if (answ.isResource()) {
									set.add(answ.asResource().getURI());
								} else if (answ.isLiteral()) {
									set.add(((Literal) answ).getValue().toString());
								} else {
									set.add(answ.toString());
								}
							}
							q.setGoldenAnswers(set);
						}
					}
					break;
				case nlq:
					out = loadNLQ(is, deriveUri);
					break;
				
				
				case TREC_Entity:
				case INEX:
				case SemSearch:
				case QALD2:
					out=LoaderController.loadTSV(is,data.name());
					break;


				case Stanford_dev:
				case Stanford_train:
					out = StanfordLoader.load(is);
					break;
				}
				is.close();
				return out;
			} else {
				is.close();
				throw new IOException("InputStream is null");
			}
		} catch (IOException e) {
			log.info("Couldnt load dataset ", e);
		}
		return null;
	}

	/*private static List<InputStream> getSequenceInputStream(Dataset data) {
		// TODO Auto-generated method stub
		
	    List<InputStream> inputStreams = new Vector<InputStream>();
		inputStreams.add(getLoadingAnchor().getResourceAsStream("/qrels-v21.txt"));
		inputStreams.add(getLoadingAnchor().getResourceAsStream("/queries-v2.txt"));
	
		return inputStreams;
	}*/

	/**
	 * Use this to load answers from server:{@link AnswerSyncer}
	 */
	@Deprecated //TODO refactor this so that answersyncing is no longer in this class
	private static List<IQuestion> qald3TestEsdbpediaLoader(final String deriveUri, final String questionLang) {
		List<IQuestion> answerList = null;
		try {
			InputStream sparqlIs = null;
			InputStream answerIs = null;
			sparqlIs = getLoadingAnchor().getResourceAsStream("/QALD-master/3/data/esdbpedia-test.xml");
			answerIs = getInputStream(Dataset.QALD3_Test_esdbpedia);
			if ((sparqlIs == null) || (answerIs == null)) {
				log.error("Couldn't load dataset " + "/QALD-master/3/data/esdbpedia-test.xml" + " and  " + Dataset.QALD3_Test_esdbpedia.toString() + ". Returning null.");
				return null;
			}

			if ((sparqlIs.available() > 0) && (answerIs.available() > 0)) {
				answerList = loadXML(answerIs, deriveUri, questionLang);
				List<IQuestion> sparqlList = loadXML(sparqlIs, deriveUri, questionLang);
				for (IQuestion q : answerList) {
					for (IQuestion sparqlQ : sparqlList) {
						if (q.getId().equals(sparqlQ.getId())) {
							q.setSparqlQuery(sparqlQ.getSparqlQuery());
						}
					}
				}

			}

		} catch (IOException e) {
			log.info("Couldnt load datasets ", e);
		}
		return answerList;

	}

	/**
	 * This methods loads QALD XML files (used in QALD 1 to QALD 5)
	 */
	public static List<IQuestion> loadXML(final InputStream file, final String questionLang) {
		return loadXML(file, null, questionLang);
	}

	/**
	 * Use this to load answers from server:{@link AnswerSyncer} This methods loads QALD XML files (used in QALD 1 to QALD 5) and will get the Answers from the given Endpoint deriveUri
	 */
	@Deprecated //TODO refactor this so that answersyncing is no longer in this class
	public static List<IQuestion> loadXML(final InputStream file, final String deriveUri, final String questionLang) {
		List<IQuestion> questions = new ArrayList<>();

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc;
			doc = db.parse(file);
			doc.getDocumentElement().normalize();

			NodeList questionNodes = doc.getElementsByTagName("question");

			for (int i = 0; i < questionNodes.getLength(); i++) {

				IQuestion question = new Question();
				Element questionNode = (Element) questionNodes.item(i);
				question.setId(questionNode.getAttribute("id"));
				question.setAnswerType(questionNode.getAttribute("answertype"));
				question.setAggregation(Boolean.valueOf(questionNode.getAttribute("aggregation")));
				question.setOnlydbo(Boolean.valueOf(questionNode.getAttribute("onlydbo")));
				question.setHybrid(Boolean.valueOf(questionNode.getAttribute("hybrid")));

				// Read question
				NodeList nlrs = questionNode.getElementsByTagName("string");
				for (int j = 0; j < nlrs.getLength(); j++) {
					String lang = ((Element) nlrs.item(j)).getAttribute("lang");
					/**
					 * Workaround for QALD1 Datasets
					 */
					if (Strings.isNullOrEmpty(lang)) {
						//TODO change "en" to questionLanguage
						question.getLanguageToQuestion().put(questionLang, ((Element) nlrs.item(j)).getTextContent().trim());
						break;
					}
					question.getLanguageToQuestion().put(lang, ((Element) nlrs.item(j)).getTextContent().trim());
				}

				// read keywords
				NodeList keywords = questionNode.getElementsByTagName("keywords");
				for (int j = 0; j < keywords.getLength(); j++) {
					String lang = ((Element) keywords.item(j)).getAttribute("lang");
					question.getLanguageToKeywords().put(lang, Arrays.asList(((Element) keywords.item(j)).getTextContent().trim().split(", ")));
				}

				// Read pseudoSPARQL query
				Element element = (Element) questionNode.getElementsByTagName("pseudoquery").item(0);
				if ((element != null) && element.hasChildNodes()) {
					NodeList childNodes = element.getChildNodes();
					Node item = childNodes.item(0);
					question.setPseudoSparqlQuery(item.getNodeValue().trim());
				}

				// Read SPARQL query
				// checks also that the text node containing query is not
				// null
				element = (Element) questionNode.getElementsByTagName("query").item(0);
				if ((element != null) && element.hasChildNodes()) {

					NodeList childNodes = element.getChildNodes();
					Node item = childNodes.item(0);
					question.setSparqlQuery(item.getNodeValue().trim());
					// validate SPARQLQuery
					try {
						QueryFactory.create(question.getSparqlQuery());
					} catch (Exception e) {
						log.error("Couldn't parse a query - Skipping question", e);
						continue;
					}
				}
				// check if OUT OF SCOPE marked
				if (question.getPseudoSparqlQuery() != null) {
					question.setOutOfScope(question.getPseudoSparqlQuery().toUpperCase().contains("OUT OF SCOPE"));
				}
				// check if OUT OF SCOPE marked
				if (question.getSparqlQuery() != null) {
					question.setOutOfScope(question.getSparqlQuery().toUpperCase().contains("OUT OF SCOPE"));
				}
				// Read answers
				HashSet<String> set = new HashSet<>();
				if ((deriveUri != null) && (question.getSparqlQuery() != null)) {

					Set<RDFNode> answers = SPARQLExecutor.sparql(deriveUri, question.getSparqlQuery());
					for (RDFNode answ : answers) {
						if (answ.isResource()) {
							set.add(answ.asResource().getURI());
						} else if (answ.isLiteral()) {
							set.add(((Literal) answ).getValue().toString());
						} else {
							set.add(answ.toString());
						}
					}
				} else {
					NodeList answers = questionNode.getElementsByTagName("answers");

					for (int j = 0; j < answers.getLength(); j++) {
						NodeList answer = ((Element) answers.item(j)).getElementsByTagName("answer");
						for (int k = 0; k < answer.getLength(); k++) {

							switch (question.getAnswerType().toLowerCase()) {
							case "boolean":
								Boolean b = Boolean.valueOf(((Element) answer.item(k)).getTextContent().toLowerCase().trim());
								set.add(b.toString().trim());
								break;
							case "date":
								set.add(DateFormatter.formatDate(((Element) answer.item(k)).getTextContent()).trim());
								break;
							default:

								String answerString = ((Element) answer.item(k)).getTextContent();
								/**
								 * QALD1 questions have in answerSets "uri" and "string" nodes, and returned string contains both. This is a quick workaround
								 */
								String x = Arrays.asList(answerString.trim().split("\n")).get(0);
								set.add(x);
							}

						}
					}
				}
				question.setGoldenAnswers(set);
				questions.add(question);
			}

		} catch (DOMException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return questions;
	}

	public static List<IQuestion> loadNLQ(final InputStream file) {
		return loadNLQ(file, null);
	}

	public static List<IQuestion> loadNLQ(final InputStream file, final String deriveUri) {

		List<IQuestion> output = new ArrayList<>();
		HashMap<Integer, ArrayList<JsonObject>> idToQuestion = new HashMap<>();
		try {
			if (file.available() > 0) // check if stream is not empty
			{
				JsonReader jsonReader = Json.createReader(file);
				JsonArray mainJsonArray = jsonReader.readArray();

				for (JsonValue currentJsonValue : mainJsonArray) {
					JsonObject currentObject = (JsonObject) currentJsonValue;
					try {
						Integer id = Integer.parseInt(currentObject.getString("id"));
						if (idToQuestion.containsKey(id)) {
							idToQuestion.get(id).add(currentObject);
						} else {
							ArrayList<JsonObject> jArray = new ArrayList<>();
							jArray.add(currentObject);
							idToQuestion.put(id, jArray);
						}
					} catch (NumberFormatException e) {
						log.debug("Couldn't load question \"" + ((JsonObject) currentJsonValue).getString("question") + "\" from dataset due to wrong or missing question ID", e);
					}
				}

			}
		} catch (IOException e) {
			log.error("Could not load Dataset", e);
		}

		for (Integer i : idToQuestion.keySet()) {
			Question q = new Question();
			for (JsonObject currentJsonObject : idToQuestion.get(i)) {
				q.setValue("id", currentJsonObject.getString("id"));
				// TODO this answer type needs to be mapped via switch case
				// q.setAnswerType(currentJsonObject.getString("type"));
				String lang = currentJsonObject.getString("lang");
				String questiion = currentJsonObject.getString("question");
				String answer = currentJsonObject.getString("answer");
				// TODO somhow check if answer is boolean or date
				String sparql = currentJsonObject.getString("sparql");

				q.getLanguageToQuestion().put(lang, questiion);
				q.setSparqlQuery(lang, sparql);

				Set<String> answ = new HashSet<>();
				if ((deriveUri != null) && (q.getSparqlQuery() != null)) {
					Set<RDFNode> answers = SPARQLExecutor.sparql(deriveUri, q.getSparqlQuery());

					for (RDFNode a : answers) {
						if (a.isResource()) {
							answ.add(a.asResource().getURI());
						} else if (a.isLiteral()) {
							answ.add(((Literal) answ).getValue().toString());
						} else {
							answ.add(answ.toString());
						}
					}
				} else {
					answ.add(answer);
				}
				q.setGoldenAnswers(lang, answ);
			}
			// validate SPARQL Query
			try {
				QueryFactory.create(q.getSparqlQuery());
			} catch (Exception e) {
				continue;
			}
			output.add(q);
		}

		return output;
	}


	/**
	 * Use this to load tsv files 
	 */
	public static List<IQuestion> loadTSV(InputStream queries, String name) throws IOException {
		List<IQuestion> out = new ArrayList<>();
		out = LoadTsv.readTSV(queries,getLoadingAnchor().getResourceAsStream("/qrels-v2.txt"),name);
		return out;
	}
}
