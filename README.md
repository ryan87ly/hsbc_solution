# HSBC Take Home Exercise

## How to run
`gradlew clean test`

## External libraries used
- LMAX Disruptor
- JUnit 5
- Mockito

## Assumptions
- This submission supports multiple producers and multiple consumers with thread safety
- Slow consumers will cause backpressure on producers, producer threads will be blocked when sending queue is full
- Given the messages are transmitted among producer and consumer threads, no serialization supported on ***Message*** class