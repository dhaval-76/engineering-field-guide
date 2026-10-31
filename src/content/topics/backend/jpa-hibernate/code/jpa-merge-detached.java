// The client sends back an edited product it received earlier (now detached, version 7).
@Transactional
public ProductDto update(ProductDto edited) {
    // Option A — merge: copies EVERY field of the detached object onto a managed copy.
    //   SELECT product WHERE id = ?        (if not already in the context)
    //   copy all fields, including version 7
    // at flush: UPDATE … SET name=?, price=?, …, version = 8 WHERE id = ? AND version = 7
    //   If someone else saved meanwhile (DB version is 8) → OptimisticLockException.
    Product managed = em.merge(edited.toEntity());
    // use the returned instance, not the argument
    return ProductDto.from(managed);
}

// Option B — usually better for APIs: load, check the version, copy only what may change.
@Transactional
public ProductDto patch(long id, int expectedVersion, ProductPatch patch) {
    Product p = repo.findById(id).orElseThrow(() -> new NotFoundException("product", id));
    // → 409 / 412
    if (p.getVersion() != expectedVersion) throw new StaleVersionException(id);
    // fields the client didn't send stay untouched
    patch.name().ifPresent(p::setName);
    // dirty checking writes only these columns' row
    patch.price().ifPresent(p::setPrice);
    return ProductDto.from(p);
}
