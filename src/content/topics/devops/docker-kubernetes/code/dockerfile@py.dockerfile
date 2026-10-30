# syntax=docker/dockerfile:1
FROM python:3.13-slim AS build
WORKDIR /app
RUN python -m venv /venv
ENV PATH=/venv/bin:$PATH
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt

FROM python:3.13-slim
ENV PATH=/venv/bin:$PATH \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
COPY --from=build /venv /venv
WORKDIR /app
COPY app ./app
RUN useradd --system --uid 10001 app
USER 10001
EXPOSE 8000
CMD ["gunicorn", "app.main:app", "-k", "uvicorn.workers.UvicornWorker", "-b", "0.0.0.0:8000", "--graceful-timeout", "20"]
