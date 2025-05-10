# Fabler Backend

A firebase based backend service for the Fabler application, a platform for creating and sharing short stories (fables).

## Project Structure

```
fabler-firecloud/
├── build.sbt                    # SBT build configuration
├── src/
│   └── main/
│       ├── resources/
│       │   ├── application.conf # Application configuration
│       │   └── logback.xml      # Logging configuration
│       └── scala/
│           └── com/
│               └── fabler/
│                   └── firecloud/
│                       ├── FablerApp.scala           # Main application entry point
│                       ├── config/
│                       │   ├── AppConfig.scala       # Application configuration model
│                       │   └── FirebaseConfig.scala  # Firebase specific configuration
│                       ├── database/
│                       │   └── FirebaseClient.scala  # Firebase database client
│                       ├── middleware/
│                       │   └── Auth.scala            # Authentication middleware
│                       ├── models/
│                       │   ├── Comment.scala         # Comment data model
│                       │   ├── Fable.scala           # Fable data model
│                       │   └── User.scala            # User data model and auth token
│                       ├── repositories/
│                       │   ├── CommentRepository.scala  # Comment repository
│                       │   ├── FableRepository.scala    # Fable repository
│                       │   └── UserRepository.scala     # User repository
│                       ├── services/
│                       │   ├── AuthService.scala     # Authentication service
│                       │   ├── CommentService.scala  # Comment service
│                       │   └── FableService.scala    # Fable service
│                       └── routes/
│                           ├── AuthRoutes.scala      # Authentication routes
│                           ├── CommentRoutes.scala   # Comment routes
│                           └── FableRoutes.scala     # Fable routes
```

## Tech Stack

- Scala 3.5
- Cats Effect for functional programming
- Http4s for HTTP server
- Circe for JSON handling
- Firebase Authentication

## Project Structure

The project follows a clean architecture approach with the following components:

- **Routes**: Handle HTTP requests and responses
- **Services**: Implement business logic
- **Repositories**: Data access layer
- **Models**: Domain objects
- **Config**: Application configuration

## Getting Started

### Prerequisites
- JDK 17 or higher
- SBT
- Firebase project with service account credentials


## Setup

1. Create a Firebase project and download the service account credentials file.
2. Place the credentials file at the project root as `service-account.json`
3. Configure the `application.conf` with your Firebase database URL.
```
sbt compile
```

### Running locally
1. Make sure PostgreSQL is running and create a database named `fabler`
2. Place Firebase credentials JSON file in the appropriate location
3. Update `application.conf` with correct database connection details
4. Run the application:
```
sbt run
```

## API Documentation

### Auth
- `POST /auth/register`: Register a new user
- `POST /auth/login`: Login with email and password
- `GET /auth/verify`: Verify authentication token

### Fables
- `GET /fables`: Get all fables
- `GET /fables/:id`: Get a fable by ID
- `POST /fables`: Create a new fable (authenticated)
- `PUT /fables/:id`: Update a fable (authenticated)
- `DELETE /fables/:id`: Delete a fable (authenticated)

### Comments
- `GET /comments/fable/:fableId`: Get comments for a fable
- `GET /comments/:id`: Get a comment by ID
- `POST /comments`: Create a new comment (authenticated)
- `PUT /comments/:id`: Update a comment (authenticated)
- `DELETE /comments/:id`: Delete a comment (authenticated)

## Deployment
The application (will be) deployed to any environment that supports Java applications. A Dockerfile is included for container-based deployments.

## License
This project (may be) licensed under the MIT License - see the LICENSE file for details.
