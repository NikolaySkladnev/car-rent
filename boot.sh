#!/usr/bin/env bash
docker compose up -d --build
curl -s http://localhost:8080/healthz || true
