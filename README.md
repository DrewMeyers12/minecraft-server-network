# Minecraft Server Network

A self-developed multiplayer Minecraft network focused on scalable backend architecture, server management, and custom Java development.

This project has been developed over several years and has grown from a single Minecraft server into a network of services capable of supporting **up to 200 concurrent players**.

## Overview

The network is built around a proxy-based architecture that allows players to move between multiple specialized Minecraft servers while backend services communicate through Redis and persist player data through MySQL.

The project was designed and implemented independently, including custom server software, databases, networking, deployment, and infrastructure.

## Architecture

```text
                         Minecraft Client
                                |
                                v
                       +----------------+
                       |    Velocity     |
                       |  Proxy Server   |
                       +--------+-------+
                                |
                +---------------+---------------+
                |               |               |
                v               v               v
           +---------+     +---------+     +---------+
           |  Lobby  |     | Game    |     | Game    |
           | Server  |     | Server  |     | Server  |
           +----+----+     +----+----+     +----+----+
                |               |               |
                +---------------+---------------+
                                |
                         +------+------+
                         |    Redis    |
                         | Discovery & |
                         | Communication|
                         +------+------+
                                |
                         +------+------+
                         |    MySQL    |
                         | Player Data |
                         +-------------+
```

## Key Components

### Java Backend Services

The network contains multiple custom Java plugins responsible for implementing gameplay, server management, player data, communication, and other functionality.

### Velocity Proxy

A Velocity proxy acts as the entry point to the network and manages player connections and distribution between backend servers.

Backend servers are dynamically managed rather than relying entirely on a static server configuration.

### Redis

Redis is used for communication and coordination between services, including:

* Server discovery
* Server status and availability
* Player information
* Inter-server communication
* Temporary/cached data

### MySQL

MySQL provides persistent storage for player-related data and other information that needs to survive server restarts.

### Networking & Infrastructure

The project also includes the deployment and networking required to make the network accessible to players.

Infrastructure work includes:

* Reverse proxy configuration
* Domain and DNS management
* Cloudflare integration
* Server networking
* Service deployment and management

## Technologies

| Technology       | Purpose                                       |
| ---------------- | --------------------------------------------- |
| **Java**         | Backend services and custom Minecraft plugins |
| **Velocity**     | Proxy and player connection management        |
| **Redis**        | Server discovery, caching, and communication  |
| **MySQL**        | Persistent data storage                       |
| **Cloudflare**   | DNS, networking, and infrastructure           |
| **Git / GitHub** | Version control and source management         |


## Engineering Challenges

### Dynamic Backend Management

The network needs to coordinate multiple backend servers that may start, stop, restart, or become unavailable.

Redis is used to maintain information about available backend servers and allow services to discover each other without requiring every server to be manually configured.

### Communication

Multiple Minecraft servers need to exchange information and coordinate player activity.

The architecture separates persistent data from temporary state and inter-service communication, using MySQL for persistent storage and Redis for fast distributed communication.

### Scalability

The network was designed around multiple backend servers rather than placing all functionality into a single Minecraft instance.

This allows additional servers and game instances to be introduced as the network grows.

## My Role

I designed and developed the project independently, working across the entire stack.

My responsibilities included:

* Designing the overall backend architecture
* Developing custom Java plugins
* Designing Redis-based communication and discovery
* Designing and managing MySQL data storage
* Configuring the Velocity proxy
* Deploying and maintaining services
* Managing networking and Cloudflare infrastructure
* Debugging issues across multiple interconnected services
* Designing systems to handle server failures and restarts

## Project Status

This project is actively developed and serves as a long-term engineering project for experimenting with backend architecture, distributed systems, networking, and large-scale multiplayer infrastructure.

## Disclaimer

This repository contains a portfolio-safe version of the project. Production credentials, private configuration, and other sensitive infrastructure information have been excluded.
