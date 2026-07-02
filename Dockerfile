FROM sbtscala/scala-sbt:eclipse-temurin-25.0.3_9_2.x

RUN apt-get update && apt-get install -y \
    libgtk-3-0 \
    libglib2.0-0 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libgbm1 \
    libasound2t64 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libx11-6 \
    libxext6 \
    libxxf86vm1 \
    libgl1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /president
COPY . .
CMD ["sbt", "run"]