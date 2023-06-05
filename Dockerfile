FROM bellsoft/liberica-openjre-alpine
EXPOSE 10093
RUN mkdir --parents ~/.postgresql && wget "https://storage.yandexcloud.net/cloud-certs/CA.pem" --output-document ~/.postgresql/root.crt && chmod 0600 ~/.postgresql/root.crt
COPY tegro-bot/build/install/tegro-bot /opt/tegro-bot
RUN apt install postgresql-client-common

WORKDIR /opt/tegro-bot
ENTRYPOINT /opt/tegro-bot/bin/tegro-bot