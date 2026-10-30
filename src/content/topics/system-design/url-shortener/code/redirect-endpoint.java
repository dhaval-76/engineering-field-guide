@RestController
class RedirectController {
    private final StringRedisTemplate redis;
    private final LinkRepository links;
    private final KafkaTemplate<String, ClickEvent> kafka;

    @GetMapping("/{code:[0-9a-zA-Z]{7}|[a-z0-9-]{3,32}}")
    ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest req) {
        String target = redis.opsForValue().get("link:" + code);
        if (target == null) {
            Optional<Link> link = links.findByCode(code);
            if (link.isEmpty()) {
                redis.opsForValue().set("link:" + code, "", Duration.ofMinutes(1));   // negative cache
                return ResponseEntity.notFound().build();
            }
            if (link.get().isExpired(Instant.now())) return ResponseEntity.status(HttpStatus.GONE).build();
            target = link.get().longUrl();
            redis.opsForValue().set("link:" + code, target, link.get().cacheTtl(Duration.ofHours(24)));
        } else if (target.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Fire and forget: KafkaTemplate.send is asynchronous; failures are logged, not surfaced.
        kafka.send("clicks", code, new ClickEvent(code, Instant.now(),
                req.getHeader("Referer"), req.getHeader("User-Agent"), req.getRemoteAddr()));

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }
}

@Service
class LinkService {
    private final LinkRepository links;
    private final IdRange ids;
    private final SafeBrowsing safeBrowsing;

    @Transactional
    public Link create(String url, String alias, Instant expiresAt, String ownerId) {
        if (!safeBrowsing.isSafe(url)) throw new UnsafeUrlException(url);
        String code = alias != null ? alias : ShortCodes.scrambled(ids.nextId());
        try {
            return links.saveAndFlush(new Link(code, url, ownerId, expiresAt, alias != null));
        } catch (DataIntegrityViolationException taken) {     // UNIQUE(code) settles alias races
            throw new AliasTakenException(alias);              // → 409
        }
    }
}
