# Kotlin In-Memory Order Book with Vert.x

Table of Contents
=================

* [Kotlin In-Memory Order Book](#kotlin-in-memory-order-book)
    * [Features](#features)
    * [Minimum Requirements](#minimum-requirements)
* [Getting Started](#getting-started)
    * [Major Libraries / Tools](#major-libraries--tools)
    * [Checkout the Code](#checkout-the-code)
* [Setting Up](#setting-up)
    * [Gradle Setup](#gradle-setup)
    * [Running the Application](#running-the-application)
* [API Endpoints](#api-endpoints)
* [Running Tests & Quality Checks](#running-tests--quality-checks)
    * [Unit Tests](#unit-tests)
    * [Integration Tests](#integration-tests)
    * [Code Coverage](#code-coverage)
    * [Static Analysis](#static-analysis)
* [Performance Considerations](#performance-considerations)
* [Trade-offs](#trade-offs)

# Kotlin In-Memory Order Book with Vert.x

A order book implementation using Vert.x: 
- This is an order book implementation for a trading system that handles buy and sell orders, matches them according to price-time priority, and maintains trade history. 
  - Featuring:
    - In-memory order matching engine
    - REST API for order management

## Features
- Limit order support
- Fetch Recent Trades
- Fetch an order book

## Minimum Requirements
- Java 17 or higher
- Kotlin 1.7+
- Gradle 7.5+

# Getting Started

## Major Libraries / Tools

| Component              | Technology       | Description                          |
|------------------------|------------------|--------------------------------------|
| Framework              | Vert.x           | Toolkit for reactive apps            |
| Language               | Kotlin           | Concise, modern language             |
| Testing                | Kotest           | Powerful testing framework           |

## Checkout the Code
```bash
git clone git@github.com:bonganim911/vertx-order-book.git
cd vertx-order-book
```

# Setting Up
Gradle Setup
The project includes Gradle wrapper:

```bash
./gradlew tasks  # View available tasks
```
## Running the Application
```bash
./gradlew run
```
The service will start on port 8080 by default.

# API Endpoints
## Order Management
##### GET BTCZAR/orderbook - Get Order Book
##### GET /BTCZAR/tradehistory - Get Recent Trades
##### GET /orders/:orderId - GET an order
##### POST /orders/limit - Submit limit order


# Running Tests & Quality Checks
## Unit Tests
```bash
./gradlew test
```
## Integration Tests
```bash
./gradlew integrationTest
```
## Code Coverage
```bash
./gradlew jacocoTestReport
```
View report at: build/reports/jacoco/test/html/index.html

## Static Analysis
```bash
./gradlew detekt
```


# Trade-offs
## Tests
- Refactor the magic number, some could be global variable and be reused.

## Architecture Choices
Design:
- Service layer
  - Could have made controller delegate business logic to service layer instead of calling processor directly.
  - Service could return the response body back to controller layer instead of controller setting that up.
  
Vert.x over Spring:
- Pros: Better performance for high-throughput systems
- Cons: Steeper learning curve, less "batteries included"

In-Memory Storage:
- Pros: Extremely fast, simple implementation
- Cons: No persistence, limited by JVM heap size

Concurrency Model:
- Vert.x event loop simplifies thread safety
- Trade engine runs single-threaded for consistency

## Could Have Added
- API Documentation
- Published code coverage
