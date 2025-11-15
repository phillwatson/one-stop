
# One-Stop

---
## Rail-Service
This micro-service provides a facade over the financial rail API; whilst also
giving admin access to the "bare" API.

### Account Polling
The rail service polls each account for new transactions on a nightly basis. When
doing so, it also checks whether the consent to access those accounts has expired.
The user will be notified (by email and online notifications) if a consent has expired.
The user can then decide whether to renew that consent. Once renewed the transactions
for the account will be retrieved from when it was last polled.

### Transaction Categories
Transactions can be identified and organised into user-defined categories that
can then be used for budgeting and analysis. Categories may identify transactions from
multiple accounts. This allows accounts to be consolidated, and their transactions to
be viewed and analysed according to their purpose. For example; the expenditure on
household services, or car servicing and fuel, or food and entertainment.

The category to which a transaction is allocated is determined using "selectors". 
Each category may have any number of selectors. These selectors identify transactions
by comparing their text properties (such as their "reference" value) against the text
properties of the selector. If the transaction contains the text of the selector
then the transaction is deemed to belong to the selector's category.

The categories are arranged into category groups, allowing the same transaction
to be analysed for different purposes. For example; one category group may analyse
the household budget, and another may analyse the source of income from all accounts.

These categories can then be viewed in charts that illustrate how the income and
expenditure is broken down across all accounts.

### Audit Reports
Users can configure reports that will be run on a daily basis to identify transaction
irregularities that might indicate fraud. These reports are based on templates that
run specific algorithms. The algorithms may be based on variables that the user can
adjust to suit their needs. For example; a report template may identify outgoing
transactions that exceed the average of outgoing transactions over a given period. In
this case the user might configure the period over which outgoing transactions are
averaged, and the multiplication factor (applied to that average) used to determine
the limit by which excessive transactions are identified.

The user can also specify the source of the transactions that will be fed into a report.
Available transaction sources are:
- ALL - the transactions from all accounts
- ACCOUNT - the transactions from one identified account
- CATEGORY_GROUP - the transactions from one identified category group (optionally with
or without the group's un-categorised transactions)
- CATEGORY - the transactions from one identified category

The results of these reports are the transactions they identify. The user will be
notified (by email and online notification) when new transactions are identified by a
report. Those transactions can then be manually inspected, allowing the user to take
whatever action might be required.
