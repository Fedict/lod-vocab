/*
 * Copyright (c) 2017, Bart Hanssens <bart.hanssens@fedict.be>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package be.belgif.vocab.ldf;

import be.belgif.vocab.App;
import be.belgif.vocab.helpers.QueryHelper;
import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.query.BindingSet;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for querying the triple store using Linked Data Fragments.
 * 
 * @author Bart.Hanssens
 */
public class QueryHelperLDF {
	private final static Logger LOG = (Logger) LoggerFactory.getLogger(QueryHelperLDF.class);
	
	private final static ValueFactory F = SimpleValueFactory.getInstance();
	
	private final static String PREFIX = App.getPrefix();
	
	private final static IRI LDF_GRAPH = F.createIRI(App.PREFIX_GRAPH + "ldf");
	private final static IRI LDF_SEARCH = F.createIRI(PREFIX + "ldf#search");		
	private final static IRI LDF_MAPPING = F.createIRI(PREFIX + "ldf#mapping");
	
	// Hydra mapping, does not change
	private final static Model HYDRA_MAPPING = new LinkedHashModel();
	static {
		IRI s = F.createIRI(PREFIX + "ldf#s");
		HYDRA_MAPPING.add(s, Hydra.VARIABLE, F.createLiteral("s"), LDF_GRAPH);
		HYDRA_MAPPING.add(s, Hydra.PROPERTY, RDF.SUBJECT, LDF_GRAPH);
		
		IRI p = F.createIRI(PREFIX + "ldf#p");
		HYDRA_MAPPING.add(p, Hydra.VARIABLE, F.createLiteral("p"), LDF_GRAPH);
		HYDRA_MAPPING.add(p, Hydra.PROPERTY, RDF.PREDICATE, LDF_GRAPH);
		
		IRI o = F.createIRI(PREFIX + "ldf#o");
		HYDRA_MAPPING.add(o, Hydra.VARIABLE, F.createLiteral("o"), LDF_GRAPH);
		HYDRA_MAPPING.add(o, Hydra.PROPERTY, RDF.OBJECT, LDF_GRAPH);
		
		IRI mapping = F.createIRI(PREFIX + "ldf#mapping");
		HYDRA_MAPPING.add(mapping, Hydra.MAPPING, s, LDF_GRAPH);
		HYDRA_MAPPING.add(mapping, Hydra.MAPPING, p, LDF_GRAPH);
		HYDRA_MAPPING.add(mapping, Hydra.MAPPING, o, LDF_GRAPH);
	}
	
	private final static long PAGE = 50;
	private final static Value PAGE_VAL = F.createLiteral(PAGE);	

	
	private final static String Q_COUNT =
		"SELECT COUNT(*) AS ?cnt " + 
		"WHERE { GRAPH ?graph { ?s ?p ?o } } ";
	
	private final static String Q_LDF = 
		"CONSTRUCT { ?s ?p?o } " +
		"WHERE { GRAPH ?graph { ?s ?p ?o } } " +
		"ORDER BY ?s " +
		"OFFSET ?offset " +
		"LIMIT " + PAGE;
	
	
	/**
	 * Convert string into IRI or null
	 * @param s string
	 * @return IRI or null
	 */
	private static IRI createIRI(String s) {
		// Variable
		if (s.equals("") || (s.startsWith("?") && s.length() > 1)) {
			return null;
		}
		
		// IRI
		return F.createIRI(s);
	}
	
	/**
	 * Convert string into literal or URI
	 * 
	 * @param s object
	 * @return value (literal or URI)
	 */
	private static Value createLiteralOrUri(String s) {
		if (s.startsWith("\"")) {
			// test for simple literal
			if (s.endsWith("\"")) {
				return F.createLiteral(s);
			}
			// test for language tag
			int l = s.lastIndexOf("\"@");
			if (l > 0) {
				return F.createLiteral(s.substring(0, l), s.substring(l+1));
			}
			// test for data type
			// test for language tag
			int t = s.lastIndexOf("\"^^");
			if (l > 0) {
				return F.createLiteral(s.substring(0, l), s.substring(l+2));
			}
			// malformed
			LOG.warn("Malformed object value");
			return null;
		}
		return createIRI(s);
	}
	
	/**
	 * Metadata of the fragment
	 * 
	 * @param fragment fragment IRI
	 * @param page page of the fragment
	 * @param count total number of results
	 * @return RDF triples
	 */
	private static Model meta(IRI page, IRI fragment, long count) {
		Model m = new LinkedHashModel();
		
		Value total = F.createLiteral(count);
		// as per spec two properties with same value
		m.add(page, Hydra.ITEMS, PAGE_VAL, LDF_GRAPH);
		m.add(page, VOID.TRIPLES, total, LDF_GRAPH);
		m.add(page, Hydra.TOTAL, total, LDF_GRAPH);
		m.add(page, Hydra.VIEW, fragment, LDF_GRAPH);
		//m.add(LDF_GRAPH, FOAF.PRIMARY_TOPIC, fragment);
		return m;
	}
	
	/**
	 * Hydra hypermedia controls
	 * 
	 * @param vocab vocabulary name
	 * @param dataset dataset IRI
	 * @param builder URI Builder
	 * @param offset offset
	 * @param count total number of triples
	 * @return RDF triples
	 */
	private static Model hyperControls(String vocab, IRI dataset, UriBuilder builder, 
										long offset, long count) {
		Model m = new LinkedHashModel();
	
		m.add(dataset, RDF.TYPE, VOID.DATASET, LDF_GRAPH);
		m.add(dataset, RDF.TYPE, Hydra.COLLECTION, LDF_GRAPH);
		
		IRI fragment = F.createIRI(builder.build().toString());
		m.add(dataset, VOID.SUBSET, fragment, LDF_GRAPH);

		// page count starts at 1
		long current = (offset % PAGE) + 1;
		builder.queryParam("page", "{page}");
		IRI page = F.createIRI(builder.build("page", current).toString());
		
		// previous and next pages, if any
		if (offset >= PAGE) {
			URI prevPage = builder.build("page", current - 1);
			m.add(dataset, Hydra.PREVIOUS, F.createIRI(prevPage.toString()), LDF_GRAPH);
		}
		if (offset + PAGE < count) {
			URI nextPage = builder.build("page", current + 1);
			m.add(dataset, Hydra.NEXT, F.createIRI(nextPage.toString()), LDF_GRAPH);
		}
	
		// search template
		m.add(dataset, Hydra.SEARCH, LDF_SEARCH, LDF_GRAPH);
		m.add(LDF_SEARCH, Hydra.TEMPLATE, 
				F.createIRI(PREFIX + "ldf/" + vocab + "{?s, ?p, ?o}"), LDF_GRAPH);
		m.add(LDF_SEARCH, Hydra.MAPPING, LDF_MAPPING, LDF_GRAPH);
		
		// generic mapping
		m.addAll(HYDRA_MAPPING);
		
		m.addAll(meta(page, fragment, count));
		m.add(fragment, Hydra.ITEMS, PAGE_VAL);
		
		return m;
	}

	
	/**
	 * Get fragment / one page of results
	 * 
	 * @param conn repository
	 * @param subj subject IRI
	 * @param pred predicate IRI
	 * @param obj object value
	 * @param graph named graph
	 * @param offset
	 * @return RDF triples
	 */
	private static Model getFragment(RepositoryConnection conn, IRI subj, IRI pred, 
								Value obj, IRI graph, long offset, long count) {	
		// nothing (more) to show
		if ((count <= 0) || (offset >= count)) { 
			return new LinkedHashModel();
		}
				
		GraphQuery gq = conn.prepareGraphQuery(Q_LDF);
		gq.setBinding("s", subj);
		gq.setBinding("p", pred);
		gq.setBinding("o", obj);
		gq.setBinding("graph", graph);
		gq.setBinding("offset", F.createLiteral(offset));
		
		return QueryResults.asModel(gq.evaluate());
	}
	
	/**
	 * Count number of results
	 * 
	 * @param conn repository
	 * @param subj subject IRI
	 * @param pred predicate IRI
	 * @param obj object value
	 * @param graph named graph
	 * @return number of results
	 */
	private static long getCount(RepositoryConnection conn, 
									IRI subj, IRI pred, Value obj, IRI graph) {
		TupleQuery tq = conn.prepareTupleQuery(Q_COUNT);
		tq.setBinding("s", subj);
		tq.setBinding("p", pred);
		tq.setBinding("o", obj);
		tq.setBinding("graph", graph);
		
		BindingSet res = QueryResults.singleResult(tq.evaluate());
		String val = res.getValue("cnt").stringValue();
		return Long.valueOf(val);
	}
	
	/**
	 * Set namespaces
	 * 
	 * @param m
	 * @return 
	 */
	private static Model setNamespaces(Model m) {
		Model ns = QueryHelper.setNamespaces(m);
		ns.setNamespace(Hydra.PREFIX, Hydra.NAMESPACE);
		ns.setNamespace(VOID.PREFIX, VOID.NAMESPACE);
		return ns;
	}
	
	/**
	 * Get linked data fragment
	 *
	 * @param repo RDF store 
	 * @param s subject to search for or null
	 * @param p predicate to search for or null
	 * @param o object to search for or null
	 * @param vocab named graph
	 * @param page page number
	 * @return RDF model 
	 */
	public static Model getLDF(Repository repo, String s, String p, String o, 
													String vocab, long page) {
		if (page < 1) {
			throw new WebApplicationException("Invalid (zero or negative) page number");
		}
		long offset = (page - 1) * PAGE;
		
		// speedup: vocabularies are stored in separate graphs
		IRI graph = QueryHelper.asGraph(vocab);
		
		IRI subj = (s != null) ? createIRI(s) : null;
		IRI pred = (p != null) ? createIRI(p) : null;
		Value obj = (o != null) ? createLiteralOrUri(o) : null;
		
		IRI dataset = F.createIRI(PREFIX + "ldf/" + vocab + "#dataset");
		
		UriBuilder builder  = UriBuilder.fromUri(PREFIX).path("ldf").path(vocab).
					queryParam("s", s).queryParam("p", p).queryParam("o", o);

		try (RepositoryConnection conn = repo.getConnection()) {
			long count = getCount(conn, subj, pred, obj, graph);
			
			Model m = new LinkedHashModel();
			m = setNamespaces(m);
			
			m.addAll(hyperControls(vocab, dataset, builder, offset, count));
			m.addAll(getFragment(conn, subj, pred, obj, graph, offset, count));
			
			return m;
		} catch (RepositoryException|MalformedQueryException|QueryEvaluationException e) {
			throw new WebApplicationException(e);
		}
	}
}
