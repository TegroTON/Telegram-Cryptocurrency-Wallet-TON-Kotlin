FROM bellsoft/liberica-openjre-alpine
COPY tegro-bot/build/install/tegro-bot /opt/tegro-bot
WORKDIR /opt/tegro-bot
ENTRYPOINT /opt/tegro-bot/bin/tegro-bot