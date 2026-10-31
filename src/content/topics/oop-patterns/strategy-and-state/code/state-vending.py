class State:
    """Default behaviour; concrete states override only the buttons that mean something for them."""
    def insert_coin(self, m, paise): m.refund(paise)
    def select(self, m, code): m.display("Insert money first")
    def cancel(self, m): pass

class Idle(State):
    def insert_coin(self, m, paise):
        m.credit += paise
        m.state = HAS_CREDIT

class HasCredit(State):
    def insert_coin(self, m, paise):
        m.credit += paise

    def select(self, m, code):
        p = m.products.get(code)
        if p is None or p["stock"] == 0:
            return m.display("Unavailable")
        if m.credit < p["price"]:
            return m.display(f"Insert {p['price'] - m.credit} more")
        m.state = DISPENSING
        m.dispense(p)                         # dispensing → idle when done, returns change

    def cancel(self, m):
        m.refund(m.credit)
        m.credit = 0
        m.state = IDLE

class Dispensing(State):
    def select(self, m, code): m.display("Please wait")

IDLE, HAS_CREDIT, DISPENSING = Idle(), HasCredit(), Dispensing()

class VendingMachine:
    def __init__(self, products: dict, display, refund, release):
        self.products, self.display, self.refund, self.release = products, display, refund, release
        self.state, self.credit = IDLE, 0

    # Every button delegates to the current state — no if/elif on a state enum anywhere.
    def insert_coin(self, paise): self.state.insert_coin(self, paise)
    def select(self, code): self.state.select(self, code)
    def cancel(self): self.state.cancel(self)

    def dispense(self, p: dict) -> None:
        p["stock"] -= 1
        self.release(p)
        self.refund(self.credit - p["price"])
        self.credit = 0
        self.state = IDLE
