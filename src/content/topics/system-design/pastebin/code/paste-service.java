@Service
class PasteService {
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private final PasteRepository pastes;       // metadata
    private final ObjectStore objects;          // S3 / GCS / Azure Blob
    private final IdRange ids;                  // see URL shortener

    @Transactional
    public Paste create(String ownerId, String content, Duration ttl, Visibility visibility) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new PayloadTooLargeException(MAX_BYTES);
        String id = ShortCodes.scrambled(ids.nextId());
        Instant expiresAt = ttl == null ? null : Instant.now().plus(ttl);

        objects.put("pastes/" + id, bytes, "text/plain; charset=utf-8");   // write content first…
        return pastes.save(new Paste(id, ownerId, bytes.length, visibility, expiresAt));   // …then metadata
        // A crash between the two leaves an orphan object, never a paste without content;
        // a lifecycle rule removes orphans.
    }
}

@RestController
class RawPasteController {
    private final PasteRepository pastes;
    private final ObjectStore objects;

    @GetMapping(value = "/raw/{id}", produces = "text/plain; charset=utf-8")
    ResponseEntity<byte[]> raw(@PathVariable String id, Principal user) {
        Paste p = pastes.findById(id).orElseThrow(NotFoundException::new);
        Instant now = Instant.now();
        if (p.isExpired(now)) return ResponseEntity.status(HttpStatus.GONE).build();
        if (p.visibility() == Visibility.PRIVATE && !p.isOwnedBy(user)) throw new NotFoundException();

        CacheControl cache = p.visibility() == Visibility.PRIVATE
            ? CacheControl.noStore().cachePrivate()
            : CacheControl.maxAge(p.cacheableFor(now, Duration.ofHours(1))).cachePublic();   // ≤ time left

        return ResponseEntity.ok()
            .cacheControl(cache)
            .eTag(p.id())                                  // immutable content: id is a perfect ETag
            .body(objects.get("pastes/" + id));
    }
}
