## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

## Progress

## Wednesday 17th August

1. Requirements analysis.
2. Project build.
3. Docker image build.
4. Application execution and project structure examination.
5. Endpoints testing via Postman.
6. Simple stress test.

## Thursday 18th August

1. Creating implementation of a simple billing service.(Just a simple change of status Pending->Paid)
2. Writing down some possible errors that might occur.
3. Exposing a rest endpoint in order to execute the service.
4. Spending time studying about kotlin in general and some features that could be implemented to the service.

**Next Steps:**
1. Developing the billing service completely.
2. Error handling (double charge, network error etc)
3. Code optimization (split into methods, refactoring)

## Saturday 20th August
1. Better implementation of Billing service.
* Fetching and charging only pending invoices.
* Failure stimulation.

2. Changed API paths for the billing process in order to fetch only the pending invoices.
* each call is idempotent, meaning no invoice is going to be charged twice.

**Next Steps:**
* Check if retry function works correctly on network failures.
* Implement proper testing.
* Create dockerfile configuration and bash scripts to run a docker cron the first day of the month(may use docker-compose)

## Tuesday 23rd August
1. Changed Payment provider mock implementation in order to throw network and currency mismatch exceptions.
2. Unit testing implementation with minor refactoring.

**Next Steps:**
* Change billing service logic. Instead of Transaction rollback, I could update the invoice as FAILED in case something went wrong
  (network or currency mismatch etc.)

## Thursday 25th August
1. Implementation of different invoice processing for FAILED invoices.
2. Creation of cron job for two cases. The app is executed every first day of each month.
   Another execution is made on the second day each month, charging FAILED invoices.
3. Experimenting with flows.

**Next Steps:**
1. Will try to implement authorization as a POC, since the app uses a third party provider or if we are going to use it with an external service.
2. Create a currency converter.

## Friday 26th August
1. Experimenting with authorization using basic auth with 3 roles (ANYONE, READ, WRITE) and 2 users (pleo-user, pleo-admin)
2. Creating a currency converter based on moneta API and ECB (Europe Central Bank) exchange rates.

#### Notes:
* I am using docker-compose to create a docker image for cron in order to run in parallel with the app and config crontab to run on the first day of
  the month.


## Summary of app functionality

* On the first day of each month the app is executed using cron.
* It fetches only the pending invoices, attempts to charge and change the invoice status to PAID from PENDING.
* In case of some failure, double charge, customer not found etc. we update the invoice status to FAILED instead of PAID.
* In case of network failure we use the retry function, which tries to repeat the charging for the specific invoice. If it succeeds
  , the invoice status is updated to PAID. If it fails, the invoice status is updated to FAILED.
* On the second day of each month the app is executed again.
* This time it fetches the failed invoices and follows the same flow as above.

## How to run the challenge.
1. Running `./docker-start.sh` will execute a `docker-compose up` that will start all the needed services.
2. Once the services are up and running, the `cronjob` will execute the jobs as explained above. If we want to test the app without having to wait, we can send a request to the api using the following commands:
   `curl -s http://localhost:7000/rest/v1/charging/pending -u "pleo-admin:pleo-admin"` (for fetching and charging the pending invoices) and `curl -s http://localhost:7000/rest/v1/charging/failed -u "pleo-admin:pleo-admin"` (for fetching and charging the failed invoices).
  Note that `-u "pleo-admin:pleo-admin"` is the username and password that has the authorization to write, since we used basic auth. In any other case we can use the user pleo-user:pleo-user.


##  Business logic ideas

App could be extended to handle possible cases that are not handled as it is.
Additional features could involve the following:

1. Even though we used a basic auth as a POC, since the app uses a third party provider or if we are going to use it with an external service, a robust
   authentication should be implemented. (probably oauth2)
2. No handling exists for possible app crashes.
3. If the charging fails the second day we could alert the customer with an email or an SMS.
4. The customer could be set to inactive after several failures.
5. In case the number of the invoices is way too big, we could use pagination for fetching invoices in batches, kafka queues or any other method for scaling.
6. We are assuming that the payment provider (3rd party) is idempotent, meaning it cannot double charge an invoice.