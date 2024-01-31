
# One-Stop

---
## Rail-Module
### Rail-Service
This service provides a facade over the suppored financial Rail services.
It provides the REST functionality required to connect to and retrieve data
from the Rail Provider APIs.

### Rail-API
Presents an Adaptor interface to the Rail Provider APIs. Allowing a common
interface to the support Rail Providers.

### Nordigen-Lib
Provides an implementation of the `com.hillayes.rail.api.RailProviderApi` for
the Nordigen API.

### Yapily-Lib
Provides an implementation of the `com.hillayes.rail.api.RailProviderApi` for
the Yapily API.

### SIM-Lib
Provides a simulator for integration testing. This mocks the external email and Rail
Provider services.


