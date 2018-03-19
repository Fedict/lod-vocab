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
package be.belgif.vocab.views;

import be.belgif.vocab.dao.VoidDAO;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ext.Provider;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

/**
 * HTML view for homepage / VoID descriptions
 * 
 * @author Bart Hanssens
 */
@Provider
public class HomepageView extends RdfView {
	private final List<VoidDAO> vocabs = new ArrayList();

	/**
	 * Get the list of vocabularies
	 * 
	 * @return list
	 */
	public List<VoidDAO> getVocabs() {
		return this.vocabs;
	}
	
	
	/** 
	 * Constructor
	 * 
	 * @param vocs vocabularies as triples
	 * @param lang language
	 */
	public HomepageView(Model vocs, String lang) {
		super("homepage.ftl", lang);
		vocs.subjects().stream().forEachOrdered(subj -> {
			Model m = new LinkedHashModel();
			vocs.getNamespaces().forEach(m::setNamespace);
			m.addAll(vocs.filter(subj, null, null));
			vocabs.add(new VoidDAO(m, (IRI) subj));
		});		
	}
}