# URL Shortener - Frontend

Modern React + TypeScript frontend for the URL Shortener Platform.

## Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **React Router** - Client-side routing
- **Axios** - HTTP client
- **Zustand** - State management
- **Recharts** - Data visualization
- **QRCode.react** - QR code generation

## Features

- **Authentication**: Login and registration with JWT tokens
- **URL Shortening**: Create short URLs with optional custom aliases
- **Dashboard**: Manage all your shortened URLs
- **Analytics**:
  - Real-time click tracking (5-10 second polling)
  - Time-series charts (30-day history)
  - Geographic distribution
  - Device/browser/OS analytics
  - Recent clicks table
- **QR Codes**: Automatic QR code generation for each short URL
- **Copy to Clipboard**: One-click copy for short URLs
- **Responsive Design**: Mobile-first, works on all screen sizes

## Project Structure

```
frontend/
├── src/
│   ├── api/          # API client and service modules
│   │   ├── client.ts      # Axios instance with interceptors
│   │   ├── auth.ts        # Authentication API
│   │   ├── urls.ts        # URL shortening API
│   │   └── analytics.ts   # Analytics API
│   ├── components/   # Reusable components
│   │   ├── Navbar.tsx
│   │   └── ProtectedRoute.tsx
│   ├── pages/        # Page components
│   │   ├── Home.tsx       # URL shortening form
│   │   ├── Login.tsx
│   │   ├── Register.tsx
│   │   ├── Dashboard.tsx  # User's URLs list
│   │   └── Analytics.tsx  # Detailed analytics
│   ├── store/        # Zustand state management
│   │   └── authStore.ts
│   ├── styles/       # CSS modules
│   │   ├── Navbar.css
│   │   ├── Auth.css
│   │   ├── Home.css
│   │   ├── Dashboard.css
│   │   └── Analytics.css
│   ├── types/        # TypeScript type definitions
│   │   └── index.ts
│   ├── App.tsx       # Main app component
│   ├── App.css       # Global styles
│   └── main.tsx      # Entry point
├── Dockerfile        # Multi-stage production build
├── nginx.conf        # Nginx configuration for SPA
└── package.json
```

## Getting Started

### Prerequisites

- Node.js 20+
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Copy environment file
cp .env.example .env
```

### Development

```bash
# Start development server
npm run dev
```

The app will be available at `http://localhost:3000`

### Build

```bash
# Create production build
npm run build

# Preview production build
npm run preview
```

## Environment Variables

Create a `.env` file:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## API Integration

The frontend communicates with the API Gateway (port 8080) which routes requests to:
- URL Service (8081) - URL CRUD operations
- Analytics Service (8082) - Analytics and metrics

### API Endpoints Used

**Authentication (API Gateway)**
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login

**URLs (URL Service via Gateway)**
- `POST /api/urls/shorten` - Create short URL
- `GET /api/urls/{shortCode}` - Get URL details
- `GET /api/urls/user/{userId}` - List user's URLs
- `DELETE /api/urls/{shortCode}` - Delete URL

**Analytics (Analytics Service via Gateway)**
- `GET /api/analytics/url/{shortCode}` - URL summary
- `GET /api/analytics/url/{shortCode}/daily` - Daily stats
- `GET /api/analytics/url/{shortCode}/geo` - Geographic data
- `GET /api/analytics/url/{shortCode}/clicks` - Recent clicks
- `GET /api/analytics/top` - Top URLs

## Real-time Features

- **Dashboard**: Polls for URL updates every 5 seconds
- **Analytics**: Polls for analytics updates every 10 seconds
- Automatically updates click counts without page refresh

## Docker Deployment

### Build and Run

```bash
# Build Docker image
docker build -t url-shortener-frontend .

# Run container
docker run -p 3000:80 url-shortener-frontend
```

### Docker Compose

The frontend is included in the main `docker-compose.yml`:

```bash
# Start all services including frontend
docker-compose up
```

The frontend will be available at `http://localhost:3000`

## Nginx Configuration

The production build uses Nginx with:
- SPA routing (all routes serve index.html)
- Gzip compression
- Static asset caching
- Security headers
- Health check endpoint at `/health`

## Authentication Flow

1. User registers or logs in
2. JWT token is returned from API Gateway
3. Token is stored in localStorage
4. Token is automatically included in all API requests via Axios interceptor
5. On 401 errors, user is redirected to login and token is cleared

## State Management

Uses Zustand for authentication state:
- `user` - Current user object
- `token` - JWT token
- `isAuthenticated` - Boolean flag
- `login()` - Store token and user
- `logout()` - Clear token and user
- `initAuth()` - Restore session from localStorage

## Styling

- CSS Variables for theming
- Responsive design with media queries
- Mobile-first approach
- Consistent spacing and typography
- Modern card-based UI

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Performance

- Code splitting with React Router
- Lazy loading of charts
- Optimized bundle size
- Efficient re-renders with React hooks
- Nginx caching for static assets

## Security

- XSS protection via React's escaping
- CSRF protection via JWT
- Secure headers in Nginx
- Input validation
- Protected routes requiring authentication

## License

Part of the URL Shortener Platform project.
