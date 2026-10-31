# pytest + Testcontainers: a real PostgreSQL in Docker, not an in-memory imitation.
import pytest
from sqlalchemy import create_engine
from testcontainers.postgres import PostgresContainer

from shop.db import migrate
from shop.orders import OrderRepository

@pytest.fixture(scope="module")
def engine():
    # started once per module
    with PostgresContainer("postgres:17-alpine", driver="psycopg") as pg:
        eng = create_engine(pg.get_connection_url())
        # the same migrations as production
        migrate(eng)
        yield eng

@pytest.fixture
def repo(engine):
    with engine.connect() as conn:
        tx = conn.begin()
        yield OrderRepository(conn)
        # each test leaves no data behind
        tx.rollback()

def test_find_with_lines_loads_lines(repo):
    customer_id = repo.create_customer("Asha")
    repo.create_order(customer_id, [("BOOK", 2), ("PEN", 1)])
    [order] = repo.find_with_lines(customer_id)
    assert sorted(line.sku for line in order.lines) == ["BOOK", "PEN"]

def test_reserve_refuses_when_stock_is_insufficient(repo):
    product_id = repo.create_product("BOOK", 1)
    # the atomic UPDATE refuses
    assert repo.reserve(product_id, 2) == 0
