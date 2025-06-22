# WebSocket Chat Application

A real-time chat application built with Spring Boot and WebSockets that allows users to communicate with each other directly. The application supports user registration, authentication, and real-time messaging with offline message delivery.

## Features

### Core Functionality
- **Real-time messaging** using WebSocket connections
- **User registration and authentication** with simple username/password
- **Online user tracking** with live status updates
- **Direct messaging** between users
- **Offline message delivery** - messages sent to offline users are stored and delivered when they come back online
- **Session management** - tracks active WebSocket connections per user
- **H2 in-memory database** for data persistence during runtime

### Technical Architecture
- **Spring Boot 3.2.0** with WebSocket support
- **Raw WebSocketHandler** implementation (no STOMP abstractions)
- **SockJS** for enhanced browser compatibility and fallback support
- **H2 Database** for storing users and undelivered messages
- **Spring Data JPA** for database operations
- **Spring Security** configured for API access
- **Pure HTML/JavaScript** frontend with no client-side frameworks

## How It Works

### User Flow
1. **Registration/Login**: Users register with a username and password or login with existing credentials
2. **WebSocket Connection**: Upon successful login, a WebSocket connection is established
3. **Authentication**: The client authenticates over WebSocket by sending username
4. **Online Status**: The server tracks active sessions and broadcasts online user lists
5. **Messaging**: Users select recipients from the online users list and send messages
6. **Message Delivery**:
    - If recipient is online: Message delivered immediately via WebSocket
    - If recipient is offline: Message stored in database as undelivered
7. **Offline Message Delivery**: When users come back online, all undelivered messages are sent
8. **Historical Messages**: When users come back online, the whole conversation history is fetched

### Technical Implementation

#### WebSocket Message Types
- `authenticate`: Client authenticates with username
- `chat`: Send a message to another user
- `get_users`: Request current online users list
- `message`: Receive a message from another user
- `online_users`: Receive updated list of online users
- `message_sent`: Confirmation that message was sent/queued

#### Database Schema
- **Users Table**: id, username, password, created_at, last_login
- **Messages Table**: id, sender_username, recipient_username, content, sent_at, delivered, delivered_at

#### Session Management
- WebSocket sessions are stored in a `ConcurrentHashMap` with username as key
- Sessions are cleaned up when connections close or errors occur
- Online user lists are broadcast to all connected clients when users join/leave

#### Message Persistence
- All messages are stored in the database regardless of delivery status
- Undelivered messages are marked with `delivered = false`
- When delivered, messages are marked with `delivered = true` and `delivered_at` timestamp
- Messages are delivered in chronological order (oldest first)

## API Endpoints

### Authentication REST API
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `GET /api/auth/users` - Get all registered users

### WebSocket Endpoint
- `/websocket/chat` - Main WebSocket endpoint with SockJS support

## Usage

1. **Start the application**: Run the Spring Boot application
2. **Access the chat**: Open browser to `http://localhost:8080`
3. **Register users**: Create multiple user accounts for testing
4. **Login and chat**: Login with different users in multiple browser tabs/windows
5. **Test offline messaging**: Close one browser tab, send messages to that user, then reopen to see message delivery
6. **Message History**: Fetches conversation history for each user
7. **Monitor database**: Access H2 console at `http://localhost:8080/h2-console` (URL: `jdbc:h2:mem:chatdb`, user: `sa`, password: `password`)


## Interesting Points:
We are not using ws anywhere for WebSocket URL - one that starts with ws://.

The frontend is using SockJS, not raw WebSocket URLs. In the JavaScript code, it creates the connection with:
```javascript
socket = new SockJS(baseUrl);
```
Where baseUrl is constructed as the HTTP URL (http:// or https://) + host + /websocket/chat.

SockJS handles the protocol negotiation internally and will use WebSocket (ws:// or wss://) when available, but the client code uses the HTTP-based SockJS endpoint. The raw WebSocket URL would be something like ws://localhost:8080/websocket/chat but SockJS abstracts this away.
This application demonstrates a complete real-time chat system with robust offline message handling, making it suitable for both development learning and as a foundation for more complex messaging applications.

## Limitations: 
1. Login and Register are sketchy, you do a Register first and then Login on the same page.
2. Refreshing the windows logs you out.
3. If the user logs out, the other user can send messages until the chat window of the logged out use is selected, 
after that you cannot select it again.
(can be more :))
