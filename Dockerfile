FROM openjdk:17-oracle
RUN apt-get install wget
RUN mkdir --parents ~/.postgresql && wget "https://storage.yandexcloud.net/cloud-certs/CA.pem" --output-document ~/.postgresql/root.crt && chmod 0600 ~/.postgresql/root.crt
COPY tegro-bot/build/install/tegro-bot /opt/tegro-bot

RUN apk add --no-cache msttcorefonts-installer fontconfig
RUN update-ms-fonts

WORKDIR /opt/tegro-bot
ENTRYPOINT /opt/tegro-bot/bin/tegro-bot