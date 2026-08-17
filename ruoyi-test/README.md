# RuoYi Automated Tests

This module keeps automated tests outside the business modules while still
running against the real application classes.

## Run

```bash
mvn -pl ruoyi-test -am test
```

Run the whole backend reactor, including the test module:

```bash
mvn test
```

The first test set focuses on fast unit and slice-style coverage that does not
require MySQL, Redis, Nacos, or other external services.
