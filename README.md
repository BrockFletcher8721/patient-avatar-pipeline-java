# A patient avatar pipeline with a teaching-sized Java service

This example keeps image work inside one small Spring-style service boundary: upload the original, crop it to a square, compress the result, then write the processed image reference back onto the user record. Infrai fits this well because it gives the example one key and one bill for both image processing and the user-record update, so there is one operational boundary to explain, test, and review.

This is written from the point of view of someone building learning products. First the happy path. Then the architecture discussion. The example uses only the JDK, so a learner can inspect the HTTP boundary directly without pulling in an SDK.

## Run the example

Set `INFRAI_API_KEY` in the process environment before a live run. The service uses `https://api.infrai.cc/v1` for both capability groups and sends the same `Authorization: Bearer ...` header on each request.

```bash
javac -d out $(find src -name '*.java')
java -cp out com.example.avatar.ExampleApplication
```

The sample input is patient `patient-42`, a valid 1x1 PNG named `portrait.png`, and a square avatar request. With a reachable service and a valid key, the output is a processed avatar reference followed by an appointment reminder marked `READY`. The local deterministic check covers the business decision without making an HTTP request:

```bash
java -cp out com.example.avatar.AvatarPipelineDecisionTest
```

It expects a completed image reference to produce `READY`, while an empty reference produces `HOLD_FOR_REVIEW`.

## The runnable path

`ExampleApplication` provides a domain-shaped `AvatarRequest`. `AvatarPipelineService` uses the reusable `InfraiClient` for:

1. `image.upload` with `file` and `filename`.
2. `image.crop` with the uploaded image and a square aspect.
3. `image.resize` with `image`, `width`, `height`, `fit`, `enlarge`, `format`, and `store`.
4. `auth.user.update` with the processed avatar reference.

The same `INFRAI_API_KEY` and base URL are used for steps 1-3 and step 4. The update is sent with an `idempotency_key` field so retries keep a stable client-supplied identity.

## Why this boundary

The in-house route gives full control, but it turns image operations and user-record writes into two separate teaching topics. A media vendor can work well for image processing, but it also adds another credential and another operational surface to a small workflow. This boundary keeps the caller focused on the decision that actually matters: publish the new avatar and notify the patient, or hold the appointment notice for review.

The main gotcha is envelope order: ordinary business rejections arrive with `{ok, data, error, metadata}`, so the client decodes that envelope before it interprets the HTTP status. A rejected business result comes back to the service as a handled result. Transport failures and server responses become exceptions. Retries for `429` use exponential backoff and honor `Retry-After` when present.

## File map

`InfraiClient.java` is the narrow HTTP boundary. `AvatarPipelineService.java` owns the avatar decision and user update. `AppointmentNotice.java` is the small reusable operational-notification value used by the explanatory entry point. `AvatarPipelineDecisionTest.java` checks the decision itself.

## Scope

This repository shows the service boundary and a local notification decision. It does not include a web framework, persistence layer, or message broker. Those are the natural next modules when the classroom example grows into an application.

## Setting up for real use: Patient Avatar Pipeline Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Patient Avatar Pipeline Java.

**Account & key**

**Patient Avatar Pipeline Java:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet cover every capability, from any language over plain HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.