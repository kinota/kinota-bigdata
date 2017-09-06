
# Kinota&trade;

Kinota&trade; is a modular open-source implmentation of
[OGC SensorThings API Part 1: Sensing](http://docs.opengeospatial.org/is/15-078r6/15-078r6.html);
referred to as "STA" hereafter.  Kinota was developed to support multiple persistence backends including traditional
RDBMS as as well as NoSQL databases; currently the only persistence backend is for Apache Cassandra.

## License

Kinota is licensed under GNU Lesser General Public License v3.0 (LGPLv3); you may not use
this file except in compliance with the License.  For more information, please refer to
the included [license](LICENSE.txt).

## Modules

Kinota is made up of the following sub-projects/modules.

### kinota-commons

kinota-commons includes base classes defining STA objects (e.g. Thing, Location, Datastream, etc.; these base classes
also know how to serialize themselves to JSON) as well as high-level service layer definitions for performing CRUD
operations for each STA object (including JSON deserialization).  The commons module also includes REST service
definitions (which are built using Jersey), as well as other utility and helper classes and methods that are shared
or likely to be shared across modules.

### kinota-persistence-cassandra

kinota-persistence-cassandra is the persistence backend for Apache Cassandra.

### kinota-rest-cassandra

kinota-rest-cassandra defines a Spring Boot application used to deploy an STA REST API with data stored in an Apache
Cassandra cluster.  kinota-rest-cassandra also includes an implementation of JSON Web Token (JWT) authentication for
limiting access to CUD (create, update, delete) operations.  Note that in the near future we plan to split JWT
authentication components into a separate module (e.g. cassandra-security-jwt).

## Compliance with OGC SensorThings API Part 1: Sensing

Kinota currently implements of subset of OGC SensorThings API Part 1: Sensing.  See the table below for a summary of
currently supported features (numbers refer to relevant sections of the STA specification).

Feature | Supported?
------- | --------------
SensorThings Service Interface | &nbsp;
9.2 Resource Path | &nbsp;
9.2.1 Usage 1: no resource path | Yes
9.2.2 Usage 2: address to a collection of entities | Yes
9.2.3 Usage 3: address to an entity in a collection | Yes
9.2.4 Usage 4: address to a property of an entity | No
9.2.5 Usage 5: address to the value of an entity’s property | No
9.2.6 Usage 6: address to a navigation property (navigationLink) | Yes
9.2.7 Usage 7: address to an associationLink | Yes
9.2.8 Usage 8: nested resource path | No
9.3 Requesting Data | &nbsp;
9.3.2 $expand and $selected | No
9.3.3 Query Entity Sets | &nbsp;
9.3.3.1 $orderby | No
9.3.3.2 $top | Yes
9.3.3.3 $skip | Yes
9.3.3.4 $count | No
9.3.3.5 $filter | No
9.3.3.6 Server-Driven Paging | Yes
10. Create-Update-Delete | &nbsp;
10.2 Create an entity | &nbsp;
10.2.1.1 Link to existing entities when creating an entity | Yes
10.2.1.2 Create related entities when creating an entity | No
10.3 Update an entity | Yes
10.4 Delete an entity | Yes
11. Batch Requests | No
12. SensorThings MultiDatastream extension | No
13. SensorThings Data Array Extension | Yes
14. SensorThings Sensing MQTT Extension | No

## Requirements

Kinota requires the following:
* Java 8
* Maven 3
* Apache Cassandra 3.x (to use the default Cassandra persistence backend)

### Maven configuration

Coming soon!

### Development

Unit and integration tests use an embedded version of Apache Cassandra to allow easy development from a single
developer machine.

### Help

brian.miles@cgifederal.com

david.fladung@cgifederal.com
