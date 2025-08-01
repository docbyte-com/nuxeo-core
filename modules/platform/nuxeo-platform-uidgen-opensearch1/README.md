nuxeo-platform-uidgen-opensearch1
=================================

## About

This project provides a sequence number generator based on OpenSearch 1.x.

You need to contribute the sequencer to the Nuxeo Server, for example:
```
  <extension target="org.nuxeo.ecm.core.uidgen.UIDGeneratorService" point="sequencers">
    <sequencer name="my-sequencer" class="org.nuxeo.uidgen.opensearch1.OpenSearchUIDSequencer" />
  </extension>
```

If you want to make it the default for the whole Nuxeo Server you need to name it `default`.

## Why this module?

The Elasticsearch backend for the Nuxeo Audit service `ESAuditBackend` needs to generate sequence ids.

It seems bad to use a SQL database just for handling these sequences: that's why this OpenSearch based implementation does exist.

## How it works

The implementation is based on the Blog post "[ElasticSearch::Sequence - a blazing fast ticket server](http://blogs.perl.org/users/clinton_gormley/2011/10/elasticsearchsequence---a-blazing-fast-ticket-server.html)".

Basically, it uses an index with a single entry where the revision number is used as current value of the sequence.

## Using it

    UIDGeneratorService service = Framework.getService(UIDGeneratorService.class);
    UIDSequencer seq = service.getSequencer("my-sequencer");
    int number = seq.getNext(key);

## Building

To build and run the tests, simply run the Maven build:

    mvn clean install
