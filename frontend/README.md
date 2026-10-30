# LearnMate AI frontend

The LearnMate AI frontend is a React, TypeScript, and Vite application. It
includes the landing page, sign-in/registration, an authenticated app shell,
and screens for every planned module. The backend has no endpoints yet, so
screens show "Not connected yet" notices. See `../README.md` and `../docs/`.

## Requirements

- Node.js 20.19+ or 22.12+
- npm

## Install

From this directory, install the dependencies:

```sh
npm install
```

## Configure

Copy `.env.example` to `.env.local` and set `VITE_API_BASE_URL` to the API base
URL you want to use. The default is `http://localhost:8080/api/v1`. Vite exposes
`VITE_` variables to the frontend, so do not put secrets in them.

## Develop

```sh
npm run dev
```

Vite prints the local development URL in the terminal (typically
`http://localhost:5173`).

## Lint and build

```sh
npm run lint
npm run build
```

The production output is written to `dist/`.
