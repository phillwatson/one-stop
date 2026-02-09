# Copilot Guidelines for One-Stop Client

This is a React 19 + TypeScript financial management application featuring account management, transactions, portfolios, and reporting.

## Build and Test

**Install dependencies:**
```bash
pnpm install
```

**Start development server:**
```bash
pnpm start
```

**Build for production:**
```bash
pnpm build
```

**Run tests:**
```bash
pnpm test
```

## Code Style

- **Language**: TypeScript with strict mode enabled
- **UI Framework**: Material-UI (MUI) components, augmented with react-bootstrap
- **Path Alias**: Use `@/` prefix when importing from `src/` (maps to `./src/*` via `tsconfig.paths.json`)

Example imports:
```typescript
import UserProfile from '@/model/user-profile.model';
import { useCurrentUser } from '@/contexts/user-context';
```

## Architecture

### Layered Structure
- **`pages/`**: Route-level components (main, accounts, transactions, profiles, audits, portfolios)
- **`components/`**: Reusable UI components organized by feature (account, categories, reconciliation, oauth, etc.)
- **`services/`**: HTTP communication layer using Axios with centralized `http-common.ts`
- **`contexts/`**: React Context providers for global state (UserProfileProvider, MonetaryFormatProvider, NotificationProvider, MessageProvider)
- **`model/`**: TypeScript interfaces and types for API contracts
- **`util/`**: Utility helpers (e.g., date-util.ts)

### Data Flow
1. Components call service methods
2. Services use `http-common.ts` Axios instance to communicate with backend
3. HTTP responses mapped to `model/` types
4. State updates via Context API or local useState
5. UI rendered from state

### Key Providers
Located in `App.tsx` root:
- `UserProfileProvider` → provides `useCurrentUser()` hook
- `MonetaryFormatProvider` → global monetary formatting
- `NotificationProvider` → toast/notification system
- `MessageProvider` → message/alert system
- `ReconcileTransactionsProvider` → reconciliation workflow state

## Service Layer Conventions

Services follow a consistent pattern:
```typescript
// services/account.service.ts
class AccountService {
  getAll(page: number, pageSize: number): Promise<PaginatedList<AccountDetail>>
  fetchAll(): Promise<Array<AccountDetail>>  // pagination helper
  get(accountId: string): Promise<AccountDetail>
  getTransactions(...): Promise<PaginatedTransactions>
}
const accountService = new AccountService();
export default accountService;
```

**HTTP Client** (`http-common.ts`):
- Axios singleton with XSRF-TOKEN handling
- Response interceptor for error handling and token refresh
- Auto-injects location headers (X-Location-IP, X-Location-City, etc.)
- Handles authentication token refresh in-flight

## Integration Points

- **Backend**: REST API on `http://localhost:80` (configured as proxy in package.json)
- **OIDC Authentication**: OAuth provider integration in `components/oauth/`
- **API Endpoints**: `/rails/*` prefix for Rails-based backend (see models: rails-account, rails-transaction, rails-balances)
- **Pagination**: All list endpoints follow `PaginatedList<T>` pattern with `page`, `page-size`, `items`, and navigation links

## Project Conventions

### Type Naming
- Model files use `.model.ts` suffix (e.g., `user-profile.model.ts`)
- Services use `.service.ts` suffix
- Components use component-specific names (e.g., `account-header.tsx`)

### Component Organization
- One feature = one subdirectory in `components/`
- Each component directory may contain multiple related component files

### Pagination Pattern
Used throughout services for both fetching and transactions:
- Helper methods like `fetchAll()` handle pagination transparently
- Results wrapped in `PaginatedList<T>` with `items`, `page`, `size`, and `links.next`

### Contexts as Hooks
Always export hooks from context providers:
```typescript
export function useCurrentUser(): [UserProfile | undefined, (user: UserProfile | undefined) => void]
```

## TypeScript Configuration

- **Target**: ES5 (for broad browser compatibility)
- **JSX**: react-jsx (automatic JSX transform)
- **Strict Mode**: Enabled
- **Module Resolution**: Node
- **No unused local variables**: Enforced by `noUnusedLocals` (via eslintConfig)

## Common Patterns to Follow

1. **Page-to-Component**: Pages in `pages/` wire up routes and compose components
2. **Service Injection**: Components import service singletons, avoid creating new instances
3. **Error Handling**: Services return Promises; components handle `catch` with user notifications via NotificationProvider
4. **State Management**: Use Context for global state; useState for local component state
5. **API Types**: Import interfaces from `model/` to ensure type safety across layers
