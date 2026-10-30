// The client sends back an edited product it received earlier (now detached, version 7).
@Transactional
public ProductDto update(ProductDto edited) {
    // Option A — merge: copies EVERY field of the detached object onto a managed copy.
    //   SELECT product WHERE id = ?        (if not already in the context)
    //   copy all fields, including version 7
    //   at flush: UPDATE … SET name=?, price=?, …, version = 8 WHERE id = ? AND version = 7
    //   If someone else saved meanwhile (DB version is 8) → OptimisticLockException.
    Product managed = em.merge(edited.toEntity());
    return ProductDto.from(managed);           // use the returned instance, not the argument
}

// Option B — usually better for APIs: load, check the version, copy only what may change.
@Transactional
public ProductDto patch(long id, int expectedVersion, ProductPatch patch) {
    Product p = repo.findById(id).orElseThrow(() -> new NotFoundException("product", id));
    if (p.getVersion() != expectedVersion) throw new StaleVersionException(id); // → 409 / 412
    patch.name().ifPresent(p::setName);        // fields the client didn't send stay untouched
    patch.price().ifPresent(p::setPrice);      // dirty checking writes only these columns' row
    return ProductDto.from(p);
}
