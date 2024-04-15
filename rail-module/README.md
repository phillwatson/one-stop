
# One-Stop

---
## Rail-Module
### Rail-Service
This is the main service for this module, interacting with the Rail Provider APIs.
It provides a REST interface for the supported Rail Providers; calling upon the
Rail Providers below for functionality required to connect to and retrieve data from
the Rail.

### Rail-API
Presents a Bridge interface to the Rail Provider APIs. Allowing a common
interface to the support Rail Providers.

### Nordigen-Lib
The Nordigen implementation of the Rail Provider API (`com.hillayes.rail.api.RailProviderApi`).

### Yapily-Lib
The Yapily implementation of the Rail Provider API (`com.hillayes.rail.api.RailProviderApi`).
