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
package be.belgif.vocab.tasks;

import be.belgif.vocab.App;
import be.belgif.vocab.helpers.QueryHelper;
import be.belgif.vocab.ldf.QueryHelperLDF;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import a SKOS file and create full (static) download files in various formats.
 *
 * @author Bart.Hanssens
 */
public class VocabImportTask extends AbstractImportDumpTask {
	private final Logger LOG = (Logger) LoggerFactory.getLogger(VocabImportTask.class);
	
	/**
	 * Add VoID metadata about the thesaurus
	 *
	 * @param conn triple store repository connection
	 * @param name name of the thesaurus
	 * @param ctx context / named graph
	 * @return VoID triples
	 */
	private Model addVOID(RepositoryConnection conn, String name, Resource ctx) {
		LOG.info("Adding VOID metadata for {}", name);

		String prefix = App.getPrefix();

		Model m = new LinkedHashModel();
		ValueFactory f = conn.getValueFactory();
		IRI voidID = QueryHelper.getDatasetName(name);

		m.add(voidID, RDF.TYPE, VOID.DATASET);

		// multi-lingual titles, descriptions etc
		List<IRI> props = Arrays.asList(DCTERMS.TITLE, DCTERMS.DESCRIPTION,
				DCTERMS.LICENSE, DCTERMS.SOURCE);
		props.forEach(p
				-> Iterations.asList(conn.getStatements(null, p, null, ctx)).forEach(
						s -> m.add(voidID, p, s.getObject()))
		);

		m.add(voidID, DCTERMS.MODIFIED, f.createLiteral(new Date()));
		m.add(voidID, FOAF.HOMEPAGE, f.createIRI(prefix));

		// information about downloadable file
		m.add(voidID, VOID.DATA_DUMP, f.createIRI(prefix + "dataset/" + name));
		m.add(voidID, VOID.FEATURE, f.createIRI("http://www.w3.org/ns/formats/N-Triples"));
		m.add(voidID, VOID.FEATURE, f.createIRI("http://www.w3.org/ns/formats/Turtle"));
		m.add(voidID, VOID.FEATURE, f.createIRI("http://www.w3.org/ns/formats/JSON-LD"));

		// linked data query service(s)
		m.add(voidID, VOID.URI_LOOKUP_ENDPOINT, f.createIRI(prefix + QueryHelperLDF.LDF + "/" + name));
		m.add(voidID, VOID.URI_LOOKUP_ENDPOINT, f.createIRI(prefix + QueryHelperLDF.LDF));

		// top level and examples
		Iterations.asList(conn.getStatements(null, RDF.TYPE, SKOS.CONCEPT_SCHEME, ctx)).forEach(
				s -> m.add(voidID, VOID.ROOT_RESOURCE, s.getSubject()));
		Iterations.asList(conn.getStatements(null, SKOS.TOP_CONCEPT_OF, null, ctx)).forEach(
				s -> m.add(voidID, VOID.EXAMPLE_RESOURCE, s.getSubject()));

		m.add(voidID, VOID.TRIPLES, f.createLiteral(conn.size(ctx)));
		m.add(voidID, VOID.URI_SPACE, f.createLiteral(prefix + "auth/" + name));
		m.add(voidID, VOID.VOCABULARY, f.createIRI(SKOS.NAMESPACE));

		return m;
	}
	

	@Override
	protected void process(RepositoryConnection conn, String name, Resource ctx) throws IOException {
		Model voidM = addVOID(conn, name, ctx);
		conn.add(voidM, ctx);
		
		writeDumps(conn, name, ctx);
	}

	/**
	 * Constructor
	 *
	 * @param repo triple store
	 * @param inDir import directory
	 * @param outDir download directory
	 */
	public VocabImportTask(Repository repo, String inDir, String outDir) {
		super("vocab-import", repo, inDir, outDir, QueryHelper.VOCAB);
	}
}
