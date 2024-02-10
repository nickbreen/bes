A Bazel Build Event Service.

Run tests.
```shell
bazel test --test_output all ...
```

Build and run the BES.
```shell
bazel run bes \
      journal+binary:file:$PWD/src/test-support/resources/jnl.bin \
      journal+json:file:$PWD/src/test-support/resources/jnl.json \
      journal+text:file:$PWD/src/test-support/resources/jnl.text
```

Re-build forwarding to the BES.
```shell
bazel build --bes_backend=grpc://localhost:8888 --bes_results_url=http://localhost:8080 ...
```

All-in-one:
```shell
bazel run bes journal+text:file:///dev/stderr
```
```shell
bazel run bec grpc://localhost:8888 < src/test-support/resources/jnl.bin
```

Generate test fixtures.
```shell
bazel build --config bes --bes_backend=grpc://localhost:8888 --bes_results_url=http://localhost:8080 ...
```



----

Sinks:
- Message to File as Binary
- Message to File as Text
- Message tp File as JSON
- Message to Redis as JSON
- Message to JDBC:SQLite as JSON
- Message to JDBC:H2 can store JSON but cannot query into it :-(
