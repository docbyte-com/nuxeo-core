nuxeo-platform-audit-opensearch1
================================

## About

This project provides a backend based on OpenSearch for Nuxeo for Audit Service.

The idea is to use Lucene / OpenSearch as storage backend for the Audit trail entries.

Usage of OpenSearch audit backend allows to easily make the Audit service scale :

 - when there are a lot of entries
     - lot of access
     - custom logging
 - when there are a lot of queries
     - reporting
     - usage of sync systems like Nuxeo Drive
 - when custom attributes on Audit entries are used

## How it works

An OpenSearch  based `AuditBackend` is contributed at startup using a dedicated `audit` index to handle storage and queries.

The queries and PageProviders are based on OpenSearch native DSL.

The original Audit service uses a JPA sequence to assign each audit entry a unique id.

In the Elasticsearch implementation, an alternate sequence genaration system is used : `nuxeo-elasticsearch-seqgen`.
