# TSLM WebUI

![Screenshot](imgs/screenshot.png)

TSLM WebUI is a Kotlin Multiplatform web interface and HTTP service for the
[TSLM](https://github.com/TorrenKt/tslm-java) torrent-title recognition model.
It serves a Compose for Web frontend and a Ktor API from one JVM process.

## Project Layout

```text
.
├── tslm-java/      # Git submodule: TSLM inference library
└── tslm-server/    # Ktor server and Compose for Web client
```

`tslm-java` is a Gradle subproject and is required to build the server.

## Requirements

- JDK 21
- Docker, for container builds
- NVIDIA driver and NVIDIA Container Toolkit, for CUDA inference
- A TSLM ONNX model

The GPU model is `tslm-fp16.onnx`; use the CPU model with
`TSLM_WEBUI_USE_CUDA=false` when CUDA is unavailable.

## Clone And Build

Clone with submodules:

```bash
git clone --recurse-submodules <repository-url>
```

For an existing checkout:

```bash
git submodule update --init --recursive
```

Build a local JVM distribution, including the production WebAssembly client:

```bash
./gradlew :tslm-server:installJvmDist
```

The runnable distribution is written to:

```text
tslm-server/build/install/tslm-server-jvm/
```

## Run Locally

```bash
./gradlew :tslm-server:run --args='--public-instance=true --model-file=/path/to/tslm-fp16.onnx'
```

Use `--help` to list all options. The server listens on port `2156` by default.

For a private instance, provide a super token and PostgreSQL connection options:

```bash
./gradlew :tslm-server:run --args='--public-instance=false --super-token=<token> --database-host=<host> --database-username=<user> --database-password=<password> --model-file=/path/to/tslm-fp16.onnx'
```

Environment variable names use the `TSLM_WEBUI_` prefix, for example
`TSLM_WEBUI_MODEL_FILE` and `TSLM_WEBUI_USE_CUDA`.

## Docker

The project provides two Gradle Docker workflows.

```bash
# Builds the application inside a multi-stage Docker image.
./gradlew :tslm-server:buildDockerImage

# Builds the JVM distribution locally, then copies it into a runtime image.
./gradlew :tslm-server:buildDevDockerImage
```

Both runtime images use CUDA 12.9 with cuDNN and install Zulu 21. The
development image is tagged `mhmzx/tslm-webui:dev`.

For GPU containers, Docker Compose must request the GPU and persist the CUDA
driver JIT cache. This reduces startup time after the first model preheat.

```yaml
services:
  tslm-webui:
    image: mhmzx/tslm-webui:dev
    gpus: all
    volumes:
      - /path/to/tslm-fp16.onnx:/models/tslm-fp16.onnx:ro
      - tslm-cuda-cache:/root/.nv/ComputeCache
    environment:
      TSLM_WEBUI_MODEL_FILE: /models/tslm-fp16.onnx
      TSLM_WEBUI_USE_CUDA: "true"

volumes:
  tslm-cuda-cache:
```

Verify the Docker GPU runtime before starting the service:

```bash
docker run --rm --gpus all --entrypoint nvidia-smi mhmzx/tslm-webui:dev
```

## License

Copyright 2026 TorrenKt.

Licensed under the [Apache License, Version 2.0](LICENSE).
