package org.alliancegenome.indexer.service;

public interface Service<E, D> {
	public Iterable<E> findAll();
	public E find(Long id);
	public void delete(Long id);
	public Iterable<D> create(Iterable<D> documents);
	public D create(D document);
}
