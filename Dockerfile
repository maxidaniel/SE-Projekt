FROM sbtscala/scala-sbt:eclipse-temurin-25.0.3_9_2.x

RUN apt-get update && apt-get install -y --no-install-recommends \
    libgtk-3-0 \
    libgl1 \
    libxxf86vm1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /president
COPY . .

# Convert all container args to one sbt command string.
ENTRYPOINT ["sh", "-lc", "exec sbt --batch \"$*\"", "--"]
CMD ["run --tui --json"]
