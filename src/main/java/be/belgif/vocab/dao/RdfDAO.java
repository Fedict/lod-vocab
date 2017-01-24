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
package be.belgif.vocab.dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO helper class for RDF triples
 * 
 * @author Bart.Hanssens
 */
public class RdfDAO {
	private final Logger LOG = (Logger) LoggerFactory.getLogger(RdfDAO.class);
	
	private final ValueFactory f = SimpleValueFactory.getInstance();
	private final Model m;
	private final IRI id;
	
	/**
	 * Get set of triple objects
	 * 
	 * @param prop property URI
	 * @return set of objects (IRI or literal)
	 */
	public Set<Value> objs(IRI prop) {
		Set objs = m.filter(id, prop, null).objects();
		return (objs == null ? Collections.EMPTY_SET : objs);
	}
	
	/**
	 * Get set of triple objects
	 * 
	 * @param prefix property namespace prefix
	 * @param term property term
	 * @return set of objects (IRI or literal)
	 */
	public Set<Value> objs(String prefix, String term) {
		Optional<Namespace> ns = m.getNamespace(prefix);
		if (ns.isPresent()) {
			return objs(f.createIRI(ns.get().getName(), term));
		} else { 
			LOG.error("Namespace for prefix {} not found", prefix);
			return Collections.EMPTY_SET;
		}
	}

	/**
	 * Get one triple
	 * 
	 * @param prop property URI
	 * @return set of objects (IRI or literal)
	 */
	public Value obj(IRI prop) {
		Iterator<Value> i = objs(prop).iterator();
		return (i.hasNext() ? i.next() : null);
	}
	
	/**
	 * Get one triple
	 * 
	 * @param prefix property namespace prefix
	 * @param term property term
	 * @return set of objects (IRI or literal)
	 */
	public Value obj(String prefix, String term) {
		Iterator<Value> i = objs(prefix, term).iterator();
		return (i.hasNext() ? i.next() : null);
	}
	
	/**
	 * Get one literal
	 * 
	 * @param prop property uri
	 * @return literals (IRI or literal)
	 */
	public Set<Literal> literals(IRI prop) {
		Set objs = m.filter(id, prop, null).objects();
		return (objs == null ? Collections.EMPTY_SET : objs);
	}
	
	/**
	 * Get one literal
	 * 
	 * @param prop property URI
	 * @return literal
	 */
	public String literal(IRI prop) {
		Iterator<Literal> i = literals(prop).iterator();
		return (i.hasNext() ? i.next().stringValue() : null);
	}
	
	/**
	 * Get one literal
	 * 
	 * @param prefix property namespace prefix
	 * @param term property term
	 * @param lang language code
	 * @return literals (IRI or literal)
	 */
	public String literal(String prefix, String term, String lang) {
		Iterator<String> i = literals(prefix, term, lang).iterator();
		return (i.hasNext() ? i.next() : null);
	}
	
	/**
	 * Get a set of literals
	 * 
	 * @param prefix property namespace prefix
	 * @param term property term
	 * @param lang language code
	 * @return literals (IRI or literal)
	 */
	public Set<String> literals(String prefix, String term, String lang) {
		Set<String> vals = new HashSet<>();
		for (Value obj: objs(prefix, term)) {
			Literal l = (Literal) obj;
			if (l.getLanguage().orElse("").equals(lang)) {
				vals.add(l.stringValue());
			}
		}
		return vals;
	}
	
	/**
	 * Get sameAs URI
	 * 
	 * @return set of uri
	 */
	public Set<Value> getSameAs() {
		return objs(OWL.SAMEAS);
	}
	
	/**
	 * Get the triple subject ID
	 * 
	 * @return subject IRI 
	 */
	public IRI getId() {
		return id;
	}
	
	/**
	 * Constructor
	 * 
	 * @param m triple model
	 * @param id  subject IRI
	 */
	public RdfDAO(Model m, IRI id) {
		this.id = id;
		this.m = m.filter(id, null, null);
	}
}
