## What and why

<!-- One or two sentences. Link the issue. -->

## Checklist

- [ ] Tests added or updated in every touched service (`.\mvnw.cmd test`)
- [ ] Contract changes are backward compatible (REST DTOs, Kafka map fields: add, never rename)
- [ ] New config keys added to `.env.example` and `k8s/<service>/configmap.yaml` or `secret.yaml` + `scripts/create-secrets.sh`
- [ ] New gateway paths added to `JwtAuthFilter` lists and `JwtAuthFilterTest`
- [ ] Documentation updated (service `README.md` / `CLAUDE.md`, `docs/`, `docs/changelog.md`)
- [ ] Kubernetes changes render: `kubectl kustomize k8s/<folder>`
