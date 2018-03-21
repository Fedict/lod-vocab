/*
 * Copyright (c) 2017, Bart Hanssens <bart.hanssens@bosa.fgov.be>
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

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
	private final Resource id;

	/**
	 * Get model
	 * 
	 * @return 
	 */
	protected Model getModel() {
		return this.m;
	}
		
	/**
	 * Get the triple subject ID
	 *
	 * @return subject IRI
	 */
	public Resource getId() {
		return id;
	}
	
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
	 * Get a set of literals as strings
	 *
	 * @param prop RDF property
	 * @param lang language code
	 * @return set of strings
	 */
	public Set<String> literals(IRI prop, String lang) {
		Set<String> vals = new HashSet<>();

		for (Value obj : objs(prop)) {
			Literal l = (Literal) obj;
			if (l.getLanguage().orElse("").equals(lang)) {
				String val = l.stringValue();
				if (val != null) {
					vals.add(val);
				}
			}
		}
		return vals;
	}
	
	/**
	 * Get one literal
	 *
	 * @param prop
	 * @param lang language code
	 * @return literal (IRI or value)
	 */
	public String literal(IRI prop, String lang) {
		Iterator<String> i = literals(prop, lang).iterator();
		return (i.hasNext() ? i.next() : "");
	}
	
	/**
	 * Get RDFS comment
	 * 
	 * @param lang language code
	 * @return value
	 */
	public String getComment(String lang) {
		return literal(RDFS.COMMENT, lang);
	}
	
	/**
	 * Get sameAs URIs
	 *
	 * @return set of uri
	 */
	public Set<Value> getSameAs() {
		return objs(OWL.SAMEAS);
	}
	
	/**
	 * Get DCTERMS description
	 * 
	 * @param lang language code
	 * @return string
	 */
	public String getDescription(String lang) {
		return literal(DCTERMS.DESCRIPTION, lang);
	}

	/**
	 * Get RDFS label
	 * 
	 * @param lang language code
	 * @return string
	 */
	public String getLabel(String lang) {
		return literal(RDFS.LABEL, lang);
	}
	
	/**
	 * Get DCTERMS title
	 * 
	 * @param lang language code
	 * @return title 
	 */
	public String getTitle(String lang) {
		return literal(DCTERMS.TITLE, lang);
	}

	/**
	 * Constructor
	 *
	 * @param m triple model
	 * @param id subject IRI or black node
	 */
	public RdfDAO(Model m, Resource id) {
		this.id = id;
		this.m = m.filter(id, null, null);
	}
}
