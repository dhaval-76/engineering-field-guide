@Component                     // singleton (default): one instance per container, shared by all threads
class PriceCalculator { }      // → must be stateless or thread-safe

@Component
@Scope("prototype")            // a new instance each time one is requested
class ReportBuilder { private final List<Row> rows = new ArrayList<>(); }

@Service
class ReportService {
    private final ObjectProvider<ReportBuilder> builders;   // ✓ ask for a fresh prototype per use

    ReportService(ObjectProvider<ReportBuilder> builders) { this.builders = builders; }

    Report monthly() {
        ReportBuilder b = builders.getObject();             // new ReportBuilder every call
        return b.addMonth().build();
    }
}
// ✗ Injecting ReportBuilder directly into a singleton gives it ONE builder forever —
//   the prototype scope only applies when the container is asked again.

@Component
@RequestScope                  // one per HTTP request; injected into singletons via a scoped proxy
class RequestAudit { }
