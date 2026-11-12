# Security Policy

## Supported versions

This is a personal portfolio project with a single deployed instance. Only the
current `main` branch is supported; there are no maintained release branches.

## Reporting a vulnerability

Please do not open a public issue for a security problem.

Use GitHub's private vulnerability reporting on this repository
(*Security → Report a vulnerability*). That channel is enabled and reaches me
directly.

Include what you did, what you expected, and what happened instead. A request
that reproduces the problem is worth more than a description of it.

## Scope

The deployed instance at https://shortlinkqr.onrender.com holds no personal
data: a link is a target URL, a code and a counter. The management token for a
link is shown once at creation and only its SHA-256 digest is stored.

Please do not run automated scanners or load tests against the deployed
instance. It runs on a free tier and the database bills by compute hour. Clone
the repository and run it locally instead — `docs/` has a Postman collection
that exercises every endpoint.
