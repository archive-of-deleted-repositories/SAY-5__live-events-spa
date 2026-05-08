.PHONY: dev migrate seed test lint typecheck e2e up down clean

dev:
	cd apps/api && mvn spring-boot:run &
	cd apps/web && pnpm dev

migrate:
	cd apps/api && mvn flyway:migrate

seed-events:
	./scripts/seed-events.sh

test:
	cd apps/api && mvn -B test
	cd apps/web && pnpm test

lint:
	cd apps/web && pnpm lint
	cd apps/api && mvn -B -q checkstyle:check || true

typecheck:
	cd apps/web && pnpm typecheck
	cd apps/api && mvn -B -q -DskipTests compile

e2e:
	cd apps/web && pnpm exec playwright test

up:
	docker compose up -d --build

down:
	docker compose down -v

clean:
	rm -rf apps/web/node_modules apps/web/dist apps/api/target
