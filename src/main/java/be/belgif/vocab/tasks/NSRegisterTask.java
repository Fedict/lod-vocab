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
package be.belgif.vocab.tasks;

import be.belgif.vocab.App;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;

import io.dropwizard.servlets.tasks.Task;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import a SKOS file and create full (static) download files in various formats.
 *
 * @author Bart.Hanssens
 */
public class NSRegisterTask extends Task {
	private final String importDir;
	private final Repository repo;

	private final Logger LOG = (Logger) LoggerFactory.getLogger(NSRegisterTask.class);

	/**
	 * Import triples from file into RDF store, using name as namespace name.
	 *
	 * @param file input file path
	 * @param name short name
	 * @param format RDF format
	 */
	private void importFile(Path file, String name, RDFFormat format) {
		try (RepositoryConnection conn = repo.getConnection()) {
			String ns = name.split("\\.")[0];
			Resource ctx = repo.getValueFactory().createIRI(App.getPrefixGraph() + ns);

			conn.begin();

			conn.remove((Resource) null, null, null, ctx);
			conn.add(file.toFile(), null, format, ctx);

			conn.commit();
		} catch (RepositoryException | IOException rex) {
			// will be rolled back automatically
			throw new WebApplicationException("Error importing namespace", rex);
		}
	}

	/**
	 * Execute task
	 *
	 * @param param parameters
	 * @param w output writer
	 * @throws Exception
	 */
	@Override
	@Timed
	public void execute(ImmutableMultimap<String, String> param, PrintWriter w) throws Exception {
		ImmutableCollection<String> files = param.get("file");
		if (files == null || files.isEmpty()) {
			throw new WebApplicationException("Param file empty");
		}

		String file = files.asList().get(0);
		Path infile = Paths.get(importDir, file);

		LOG.info("Trying to parse {}", infile);

		if (!Files.isReadable(infile)) {
			throw new WebApplicationException("File not readable");
		}
		Optional<RDFFormat> format = Rio.getParserFormatForFileName(infile.toString());
		if (!format.isPresent()) {
			throw new WebApplicationException("File type not supported");
		}

		importFile(infile, file, format.get());

		LOG.info("Done");
	}

	/**
	 * Constructor
	 *
	 * @param repo triple store
	 * @param inDir import directory
	 */
	public NSRegisterTask(Repository repo, String inDir) {
		super("register-ns");
		this.repo = repo;
		this.importDir = inDir;
	}
}