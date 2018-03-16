<!DOCTYPE html>
<html>
<head>
<meta charset='UTF-8'>
<link rel="stylesheet" type="text/css" href="/static/style.css" />
<link rel="alternate" type="text/turtle" href="/void" />
<title>Vocab Belgif - DEMO</title>
</head>
<body>
<#include "header.ftl">
<#assign l = lang>
<#assign m = messages>
<main>
    <div id="container">
    <#include "message.ftl">
    <#if ontos?has_content>
    <section>
	<h3>Ontologies</h3>
	<section>
	    <h4>${m.getString("msg.overview")}</h4>
	    <table>
	    <tr><th>${m.getString("msg.name")}</th>
		<th>${m.getString("msg.description")}</th>
		<th>${m.getString("msg.downloads")}</th>
	    </tr>
	    <#list ontos as o>
	    <tr><td><a href="${o.id}">${o.literal("rdfs", "label", l)!""}</a></td>
		<td>${o.literal("rdfs", "comment", l)!""}</td>
                <#assign download = o.download?remove_ending("#")>
                <td>OWL: <a href="${download}.ttl">TTL</a>
                        <a href="${download}.jsonld">JSON-LD</a>
                        <a href="${download}.nt">N-Triples</a><br/>
                    SHACL: <a href="${download}-shacl.ttl">TTL</a>
                        <a href="${download}-shacl.jsonld">JSON-LD</a>
                        <a href="${download}-shacl.nt">N-Triples</a>
                    </td>
	    </tr>
	    </#list>
	    </table>
	</section>
    </section>
    </#if>
    <#if xmlns?has_content>
    <section>
	<h3>Namespaces</h3>
	<section>
	    <h4>${m.getString("msg.overview")}</h4>
	    <table>
	    <tr><th>${m.getString("msg.name")}</th>
		<th>${m.getString("msg.description")}</th>
		<th>${m.getString("msg.downloads")}</th>
	    </tr>
	    <#list xmlns as n>
	    <tr><td>${n.literal("dcterms", "title", l)!""}</td>
		<td>${n.literal("dcterms", "description", l)!""}</td>
		<td><a href="${n.download!""}">XSD</a></td>
	    </tr>
	    </#list>
	    </table>
	</section>
    </section>
    </#if>
    <#if vocabs?has_content>
    <section>
	<h3>Thesauri</h3>
	<section>
	    <h4>${m.getString("msg.overview")}</h4>
	    <table>
	    <tr><th>${m.getString("msg.name")}</th>
		<th>${m.getString("msg.description")}</th>
		<th>${m.getString("msg.downloads")}</th>
	    </tr>
	    <#list vocabs?sort_by("root") as v>
	    <tr><td><a href="${v.root}">${v.literal("dcterms", "title", l)!""}</a></td>
		<td>${v.literal("dcterms", "description", l)!""}</td>
                <#assign download = v.download!"">
		<td><a href="${download}.ttl">TTL</a>
		    <a href="${download}.jsonld">JSON-LD</a>
		    <a href="${download}.nt">N-Triples</a></td>
	    </tr>
	    </#list>
	    </table>
	</section>
    </section>
    <section>
	<section>
	    <h4>Content Negotiation (De-referencing)</h4>
	    <table>
	    <tr><th>${m.getString("msg.format")}</th><th>HTTP Accept:</th></tr>
	    <tr><td>HTML</td><td>text/html</td></tr>
	    <tr><td>JSON-LD</td><td>application/ld+json</td></tr>
	    <tr><td>N-Triples</td><td>application/n-triples</td></tr>
	    <tr><td>Turtle</td><td>text/turtle</td></tr>
	    </table>
	</section>
	<section>
	    <h4>Linked Data Fragments</h4>
	    <p>http://vocab.belgif.be/_ldf</p>
	</section>
    </section>
    </#if>
    </div>
</main>
<#include "footer.ftl">
</body>
</html>
