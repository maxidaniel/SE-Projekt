FROM sbtscala/scala-sbt:eclipse-temurin-25.0.3_9_2.x

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    xauth \
    libgtk-3-0 \
    libglib2.0-0 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcairo2 \
    libpango-1.0-0 \
    libpangocairo-1.0-0 \
    libgdk-pixbuf-2.0-0 \
    libdrm2 \
    libgbm1 \
    libasound2t64 \
    libx11-6 \
    libx11-xcb1 \
    libxext6 \
    libxrender1 \
    libxrandr2 \
    libxfixes3 \
    libxdamage1 \
    libxcomposite1 \
    libxi6 \
    libxtst6 \
    libxxf86vm1 \
    libxcursor1 \
    libxinerama1 \
    libfontconfig1 \
    libfreetype6 \
    libgl1 \
    libegl1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /president

# Prime dependency resolution for faster rebuilds.
COPY build.sbt ./
COPY project ./project
RUN sbt --batch update

COPY . .
RUN sbt --batch compile

COPY docker-entrypoint.sh /usr/local/bin/president
RUN chmod +x /usr/local/bin/president

ENV APP_MODE=tui \
    SAVE_FORMAT=json \
    TERM=xterm-256color

ENTRYPOINT ["/usr/local/bin/president"]