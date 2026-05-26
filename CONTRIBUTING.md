# Contributing to MOEX Select

Project owner: **Delta team**.

> Информация не является индивидуальной инвестиционной рекомендацией. Подбор основан на выбранных пользователем параметрах и открытых рыночных данных MOEX ISS.

## Local Setup

Backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

The interface is available at `http://localhost:5173`, with the API at `http://localhost:8000/api`.

## Workflow

1. Update the local `main` branch.
2. Create a focused branch such as `feature/instrument-filters` or `fix/mobile-card-layout`.
3. Keep commit messages brief and written in English.
4. Open a pull request into `main` and request teammate review.

## Pull Request Checklist

- The affected user flow has been checked locally.
- `.\mvnw.cmd test` succeeds for backend changes.
- `npm.cmd run build` succeeds for frontend changes.
- API changes are reflected in frontend types and documentation.
- The investment recommendation disclaimer remains visible.
- Secrets, generated builds and dependency folders are not committed.
